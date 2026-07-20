using System.Security.Claims; // JWT'den clientId okuma
using Microsoft.AspNetCore.Authorization; // Rol bazlı erişim
using Microsoft.AspNetCore.Mvc;
using Nightbrate.Application.Exceptions; // AppException → 400 JSON
using Nightbrate.Application.Interfaces;

namespace Nightbrate.API.Controllers;

[ApiController]
[Route("api/[controller]")] // api/Meal
public class MealController(IMealPhotoAnalysisService mealPhotoAnalysisService) : ControllerBase // Primary constructor DI
{
    private const long MaxBytes = 5 * 1024 * 1024; // Max 5 MB fotoğraf
    private static readonly HashSet<string> AllowedExt = new(StringComparer.OrdinalIgnoreCase) { ".jpg", ".jpeg", ".png" }; // İzinli uzantılar

    [HttpPost("upload-meal-photo")] // POST api/Meal/upload-meal-photo
    [Authorize(Roles = "Client")] // Sadece danışan
    [Consumes("multipart/form-data")]
    [RequestSizeLimit(MaxBytes)]
    public async Task<IActionResult> UploadMealPhoto(IFormFile? photo, CancellationToken cancellationToken)
    {
        if (photo is null || photo.Length == 0) // Boş dosya
            throw new AppException("Fotograf dosyasi gonderilmedi.");

        if (photo.Length > MaxBytes) // Boyut aşımı
            throw new AppException("Dosya boyutu en fazla 5 MB olabilir.");

        var ext = ResolveImageExtension(photo); // .jpg / .png belirle
        if (string.IsNullOrEmpty(ext))
            throw new AppException("Sadece .jpg, .jpeg ve .png formatlari kabul edilir.");

        var clientId = User.FindFirstValue("UserId") ?? User.FindFirstValue(ClaimTypes.NameIdentifier) ?? string.Empty; // Oturumdan danışan id
        if (string.IsNullOrWhiteSpace(clientId))
            throw new AppException("Oturum bilgisi bulunamadi. Tekrar giris yapin.");

        await using var stream = photo.OpenReadStream(); // Dosya bytes
        var dto = await mealPhotoAnalysisService.UploadAnalyzeAndPersistAsync(clientId, stream, ext, cancellationToken); // Kaydet + Gemini + MealLog
        return Ok(dto); // Analiz sonucu JSON
    }

    /// <summary>Mobil kamera bazen uzantısız dosya gönderir; ContentType'tan çıkar</summary>
    private static string ResolveImageExtension(IFormFile photo)
    {
        var fromName = Path.GetExtension(photo.FileName); // Dosya adından uzantı
        if (!string.IsNullOrEmpty(fromName) && AllowedExt.Contains(fromName))
            return fromName;

        var ct = (photo.ContentType ?? string.Empty).Trim().ToLowerInvariant(); // image/jpeg vb.
        return ct switch
        {
            "image/jpeg" or "image/jpg" or "image/pjpeg" => ".jpg",
            "image/png" or "image/x-png" => ".png",
            _ => string.Empty // Tanınmayan tip
        };
    }
}
