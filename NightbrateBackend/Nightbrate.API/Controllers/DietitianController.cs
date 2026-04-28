using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize(Roles = "Dietitian")]
public class DietitianController(
    IDietitianService dietitianService,
    ICriticalAlertService criticalAlertService,
    IDietitianDailyTaskService dietitianDailyTaskService) : ControllerBase
{
    [HttpGet("daily-tasks/today")]
    public async Task<IActionResult> GetTodayDailyTasks(CancellationToken cancellationToken)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await dietitianDailyTaskService.SyncAndGetTodayAsync(dietitianId, cancellationToken));
    }

    [HttpPatch("daily-tasks/{taskId}/complete")]
    public async Task<IActionResult> SetDailyTaskComplete(
        string taskId,
        [FromBody] SetDietitianTaskCompleteDto dto,
        CancellationToken cancellationToken)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        await dietitianDailyTaskService.SetTaskCompletedAsync(dietitianId, taskId, dto.IsCompleted, cancellationToken);
        return Ok(new { message = "Gorev guncellendi." });
    }

    [HttpGet("critical-alerts")]
    public async Task<IActionResult> GetCriticalAlerts(CancellationToken cancellationToken = default)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await criticalAlertService.GetCriticalAlertsAsync(dietitianId, cancellationToken));
    }

    [HttpPost("acknowledge-critical-alert")]
    public async Task<IActionResult> AcknowledgeCriticalAlert([FromBody] AckCriticalAlertDto dto, CancellationToken cancellationToken = default)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        await criticalAlertService.AcknowledgeAsync(dietitianId, dto, cancellationToken);
        return Ok(new { message = "Uyari incelendi olarak kaydedildi." });
    }

    [HttpGet("client-brief")]
    public async Task<IActionResult> GetClientBrief([FromQuery] string clientId, CancellationToken cancellationToken = default)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        var b = await dietitianService.GetClientBriefAsync(dietitianId, clientId, cancellationToken);
        if (b is null) return NotFound();
        return Ok(b);
    }

    [HttpGet("clients-with-last-meal")]
    public async Task<IActionResult> GetClientsWithLastMeal()
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await dietitianService.GetClientsWithLastMealAsync(dietitianId));
    }

    [HttpGet("diet-program-dates")]
    public async Task<IActionResult> GetDietProgramDates([FromQuery] string clientId)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await dietitianService.GetDietProgramDatesAsync(dietitianId, clientId));
    }

    [HttpGet("diet-program")]
    public async Task<IActionResult> GetDietProgram([FromQuery] string clientId, [FromQuery] string programDate)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await dietitianService.GetDietProgramAsync(dietitianId, clientId, programDate));
    }

    [HttpGet("client-kitchen-recipe-logs")]
    public async Task<IActionResult> GetClientKitchenRecipeLogs(
        [FromQuery] string clientId,
        [FromQuery] int take = 50,
        CancellationToken cancellationToken = default)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(
            await dietitianService.GetClientKitchenRecipeLogsAsync(dietitianId, clientId, take, cancellationToken));
    }

    [HttpPost("diet-program")]
    public async Task<IActionResult> SaveDietProgram([FromBody] SaveDietProgramDto dto)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        await dietitianService.SaveDietProgramAsync(dietitianId, dto);
        return Ok(new { message = "Diyet programi kaydedildi." });
    }
}
