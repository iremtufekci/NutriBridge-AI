using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.API.Controllers;

[ApiController]
[Route("api/[controller]")] // api/Dietitian
[Authorize(Roles = "Dietitian")] // Sadece onaylı diyetisyen
public class DietitianController(
    IDietitianService dietitianService,
    ICriticalAlertService criticalAlertService,
    IDietitianDailyTaskService dietitianDailyTaskService) : ControllerBase
{
    [HttpGet("daily-tasks/today")] // Bugünkü görev listesi (senkronize)
    public async Task<IActionResult> GetTodayDailyTasks(CancellationToken cancellationToken)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await dietitianDailyTaskService.SyncAndGetTodayAsync(dietitianId, cancellationToken));
    }

    [HttpPatch("daily-tasks/{taskId}/complete")] // Görev tamamlandı/geri al
    public async Task<IActionResult> SetDailyTaskComplete(
        string taskId,
        [FromBody] SetDietitianTaskCompleteDto dto,
        CancellationToken cancellationToken)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        await dietitianDailyTaskService.SetTaskCompletedAsync(dietitianId, taskId, dto.IsCompleted, cancellationToken);
        return Ok(new { message = "Gorev guncellendi." });
    }

    [HttpGet("critical-alerts")] // Kalori/uyum kritik uyarıları
    public async Task<IActionResult> GetCriticalAlerts(CancellationToken cancellationToken = default)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await criticalAlertService.GetCriticalAlertsAsync(dietitianId, cancellationToken));
    }

    [HttpPost("acknowledge-critical-alert")] // Uyarıyı incelendi işaretle
    public async Task<IActionResult> AcknowledgeCriticalAlert([FromBody] AckCriticalAlertDto dto, CancellationToken cancellationToken = default)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        await criticalAlertService.AcknowledgeAsync(dietitianId, dto, cancellationToken);
        return Ok(new { message = "Uyari incelendi olarak kaydedildi." });
    }

    [HttpGet("client-brief")] // Kısa danışan kartı (?clientId=)
    public async Task<IActionResult> GetClientBrief([FromQuery] string clientId, CancellationToken cancellationToken = default)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        var b = await dietitianService.GetClientBriefAsync(dietitianId, clientId, cancellationToken);
        if (b is null) return NotFound(); // Bu diyetisyene ait değil veya yok
        return Ok(b);
    }

    [HttpGet("my-clients")] // Danışan listesi (sıralama + sekme)
    public async Task<IActionResult> GetMyClients(
        [FromQuery] string sort = "nameAsc",
        [FromQuery] string tab = "all",
        CancellationToken cancellationToken = default)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await dietitianService.GetMyClientsAsync(dietitianId, sort, tab, cancellationToken));
    }

    [HttpGet("client-overview")] // Danışan detay özeti
    public async Task<IActionResult> GetClientOverview([FromQuery] string clientId, CancellationToken cancellationToken = default)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        var dto = await dietitianService.GetClientOverviewAsync(dietitianId, clientId, cancellationToken);
        if (dto is null) return NotFound();
        return Ok(dto);
    }

    [HttpGet("clients-with-last-meal")] // Dashboard: danışan + son öğün
    public async Task<IActionResult> GetClientsWithLastMeal()
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await dietitianService.GetClientsWithLastMealAsync(dietitianId));
    }

    [HttpGet("diet-program-dates")] // Program yazılmış günler listesi
    public async Task<IActionResult> GetDietProgramDates([FromQuery] string clientId)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await dietitianService.GetDietProgramDatesAsync(dietitianId, clientId));
    }

    [HttpGet("diet-program")] // Belirli gün programını oku
    public async Task<IActionResult> GetDietProgram([FromQuery] string clientId, [FromQuery] string programDate)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await dietitianService.GetDietProgramAsync(dietitianId, clientId, programDate));
    }

    [HttpGet("client-kitchen-recipe-logs")] // Danışanın AI tarif paylaşımları
    public async Task<IActionResult> GetClientKitchenRecipeLogs(
        [FromQuery] string clientId,
        [FromQuery] int take = 50,
        CancellationToken cancellationToken = default)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(
            await dietitianService.GetClientKitchenRecipeLogsAsync(dietitianId, clientId, take, cancellationToken));
    }

    [HttpGet("meal-analysis-logs")] // Yemek fotoğrafı analiz logları
    public async Task<IActionResult> GetMealAnalysisLogs(
        [FromQuery] string? clientId, // Opsiyonel filtre
        [FromQuery] int take = 80,
        CancellationToken cancellationToken = default)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await dietitianService.GetMealAnalysisLogsAsync(dietitianId, clientId, take, cancellationToken));
    }

    [HttpPost("diet-program")] // Program kaydet/güncelle
    public async Task<IActionResult> SaveDietProgram([FromBody] SaveDietProgramDto dto)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        await dietitianService.SaveDietProgramAsync(dietitianId, dto); // Öğün metinleri + kalori
        return Ok(new { message = "Diyet programi kaydedildi." });
    }
}
