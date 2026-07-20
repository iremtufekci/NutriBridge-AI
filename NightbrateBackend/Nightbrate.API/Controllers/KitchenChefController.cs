using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Nightbrate.Application;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Utils;
using Nightbrate.Core.Entities;

namespace Nightbrate.API.Controllers;

[ApiController]
[Route("api/[controller]")] // api/KitchenChef
public class KitchenChefController(
    IKitchenChefService kitchenChefService, // Gemini tarif üretimi
    IKitchenChefRecipeLogRepository recipeLogRepository, // Mongo paylaşım kaydı
    IClientRepository clientRepository, // Danışan adı için
    IActivityLogService activityLogService) : ControllerBase // Sistem aktivite logu
{
    [HttpPost("generate")] // Malzemelerden tarif üret (henüz kaydetmez)
    [Authorize(Roles = "Client")]
    public async Task<IActionResult> Generate([FromBody] KitchenChefRequestDto body, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(body.Ingredients)) // Malzeme zorunlu
            throw new AppException("Malzemeler zorunludur.");
        if (body.Ingredients.Length > 4000)
            throw new AppException("Malzeme listesi çok uzun (en fazla 4000 karakter).");
        if (!KitchenChefPreferences.IsValid(body.Preference)) // vegan, pratik vb.
            throw new AppException("Geçerli bir tercih seçin: pratik, düşük kalori, vejetaryen, yüksek protein, vegan veya glutensiz.");
        if (body.TargetCalories is < 200 or > 5000)
            throw new AppException("Hedef kalori 200–5000 arasında olmalıdır.");

        var result = await kitchenChefService.GenerateRecipesAsync(body, cancellationToken).ConfigureAwait(false); // Gemini/mock
        return Ok(result); // Tarif listesi
    }

    [HttpPost("save")] // Seçilen tek tarifi diyetisyene paylaş
    [Authorize(Roles = "Client")]
    public async Task<IActionResult> Save([FromBody] KitchenChefSaveRequestDto body, CancellationToken cancellationToken)
    {
        if (body.SelectedRecipes is not { Count: 1 }) // Günde 1 tarif kuralı ile uyumlu
            throw new AppException("Diyetisyenle paylaşmak için tam bir tarif seçin (listeden yalnızca birini işaretleyin).");
        if (string.IsNullOrWhiteSpace(body.Ingredients))
            throw new AppException("Malzemeler zorunludur.");
        if (!KitchenChefPreferences.IsValid(body.Preference))
            throw new AppException("Geçerli bir tercih seçin: pratik, düşük kalori, vejetaryen, yüksek protein, vegan veya glutensiz.");
        if (body.TargetCalories is < 200 or > 5000)
            throw new AppException("Hedef kalori 200–5000 arasında olmalıdır.");

        foreach (var r in body.SelectedRecipes) // Her tarif başlık kontrolü
        {
            if (string.IsNullOrWhiteSpace(r.Title))
                throw new AppException("Her tarif için başlık zorunludur.");
        }

        var clientId = User.FindFirstValue("UserId") ?? User.FindFirstValue(ClaimTypes.NameIdentifier) ?? string.Empty;
        if (string.IsNullOrWhiteSpace(clientId))
            throw new AppException("Oturum bilgisi bulunamadı.");

        await EnsureCanShareTodayAsync(clientId, cancellationToken).ConfigureAwait(false); // Günde 1 limit

        var source = string.IsNullOrWhiteSpace(body.Source) ? "mock" : body.Source.Trim().ToLowerInvariant();
        if (source is not ("groq" or "mock" or "mock_network"))
            source = "mock";

        var log = new KitchenChefRecipeLog // MongoDB'ye yazılacak entity
        {
            ClientId = clientId,
            CreatedAtUtc = DateTime.UtcNow,
            Ingredients = body.Ingredients.Trim(),
            Preference = body.Preference.Trim(),
            TargetCalories = body.TargetCalories,
            Source = source,
            SelectedRecipes = body.SelectedRecipes.Select(ToSnapshot).ToList() // DTO → entity snapshot
        };

        await recipeLogRepository.AddAsync(log, cancellationToken).ConfigureAwait(false);

        var c = await clientRepository.GetByIdAsync(clientId).ConfigureAwait(false); // Aktivite logu için isim
        if (c is not null)
        {
            var name = $"{c.FirstName} {c.LastName}".Trim();
            if (name.Length == 0) name = c.Email;
            await activityLogService
                .LogAsync(clientId, name, "Yapay zeka mutfak: seçilen tarifler diyetisyenle paylaşıldı")
                .ConfigureAwait(false);
        }

        return Ok(new { id = log.Id, message = "Tarifler kaydedildi. Diyetisyeniniz görebilir." });
    }

    [HttpGet("share-status")] // Bugün paylaşım hakkı var mı?
    [Authorize(Roles = "Client")]
    public async Task<IActionResult> GetShareStatus(CancellationToken cancellationToken = default)
    {
        var clientId = User.FindFirstValue("UserId") ?? User.FindFirstValue(ClaimTypes.NameIdentifier) ?? string.Empty;
        if (string.IsNullOrWhiteSpace(clientId))
            throw new AppException("Oturum bilgisi bulunamadı.");

        var sharedToday = await HasSharedTodayAsync(clientId, cancellationToken).ConfigureAwait(false); // TR saati günü
        return Ok(new KitchenChefDailyShareStatusDto
        {
            SharedToday = sharedToday,
            CanShareToday = !sharedToday
        });
    }

    [HttpGet("my-shares")] // Danışanın paylaşım geçmişi (tarih filtresi)
    [Authorize(Roles = "Client")]
    public async Task<IActionResult> GetMyShares(
        [FromQuery] string? from,
        [FromQuery] string? to,
        [FromQuery] string? source,
        [FromQuery] int skip = 0,
        [FromQuery] int take = 200,
        CancellationToken cancellationToken = default)
    {
        var clientId = User.FindFirstValue("UserId") ?? User.FindFirstValue(ClaimTypes.NameIdentifier) ?? string.Empty;
        if (string.IsNullOrWhiteSpace(clientId))
            throw new AppException("Oturum bilgisi bulunamadı.");

        DateTime? fromUtc = null;
        DateTime? toUtcEx = null;
        if (!string.IsNullOrWhiteSpace(from)) // Başlangıç tarihi yyyy-MM-dd
        {
            var n = ProgramDateHelper.TryNormalize(from);
            if (n is null) throw new AppException("from: geçerli bir tarih (yyyy-MM-dd) girin.");
            fromUtc = ProgramDateHelper.UtcStartOfYmdString(n);
        }

        if (!string.IsNullOrWhiteSpace(to)) // Bitiş tarihi (dahil değil, ertesi gün 00:00)
        {
            var n = ProgramDateHelper.TryNormalize(to);
            if (n is null) throw new AppException("to: geçerli bir tarih (yyyy-MM-dd) girin.");
            toUtcEx = ProgramDateHelper.UtcStartOfYmdString(n).AddDays(1);
        }

        if (fromUtc.HasValue && toUtcEx.HasValue && fromUtc >= toUtcEx) // Mantık kontrolü
            throw new AppException("Başlangıç tarihi bitiş tarihinden önce olmalıdır.");

        if (skip < 0) skip = 0;
        if (take is < 1 or > 500) take = 200; // Sayfa boyutu sınırı

        var rows = await recipeLogRepository
            .GetByClientIdFilteredAsync(clientId, fromUtc, toUtcEx, source, skip, take, cancellationToken)
            .ConfigureAwait(false);
        return Ok(rows.Select(KitchenChefRecipeLogMapping.ToItemDto).ToList()); // Entity → DTO
    }

    private async Task EnsureCanShareTodayAsync(string clientId, CancellationToken cancellationToken) // Kayıt öncesi limit
    {
        if (await HasSharedTodayAsync(clientId, cancellationToken).ConfigureAwait(false))
            throw new AppException("Bugün zaten bir tarif paylaştınız. Yarın tekrar deneyebilirsiniz.");
    }

    private async Task<bool> HasSharedTodayAsync(string clientId, CancellationToken cancellationToken) // TR günü içinde kayıt var mı
    {
        var from = ProgramDateHelper.TurkeyTodayStartUtc(); // Bugün 00:00 TR → UTC
        var to = ProgramDateHelper.TurkeyTomorrowStartUtc(); // Yarın 00:00 TR → UTC
        var count = await recipeLogRepository
            .CountByClientIdInUtcRangeAsync(clientId, from, to, cancellationToken)
            .ConfigureAwait(false);
        return count > 0;
    }

    private static KitchenChefRecipeSnapshot ToSnapshot(KitchenChefRecipeDto d) => new() // API DTO → DB snapshot
    {
        Title = d.Title.Trim(),
        Description = d.Description?.Trim(),
        EstimatedCalories = d.EstimatedCalories,
        PrepTimeMinutes = d.PrepTimeMinutes,
        Ingredients = d.Ingredients?.Where(x => !string.IsNullOrWhiteSpace(x)).Select(x => x.Trim()).ToList() ?? new List<string>(),
        Steps = d.Steps?.Where(x => !string.IsNullOrWhiteSpace(x)).Select(x => x.Trim()).ToList() ?? new List<string>()
    };
}
