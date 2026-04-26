using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize(Roles = "Client")]
public class ClientController(IClientService clientService) : ControllerBase
{
    [HttpGet("profile")]
    public async Task<IActionResult> Profile()
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await clientService.GetProfileAsync(clientId));
    }

    /// <summary>POST ve PUT: proxy / eski surumler icin POST, REST icin PUT desteklenir.</summary>
    [HttpPost("profile")]
    [HttpPut("profile")]
    public async Task<IActionResult> UpdateProfile([FromBody] UpdateClientProfileDto? dto)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        await clientService.UpdateProfileAsync(clientId, dto);
        return Ok(new { message = "Profil bilgileriniz guncellendi." });
    }

    [HttpPost("theme")]
    public async Task<IActionResult> UpdateTheme([FromBody] UpdateThemePreferenceDto dto)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        await clientService.UpdateThemePreferenceAsync(clientId, dto.ThemePreference);
        return Ok(new { message = "Tema tercihi kaydedildi." });
    }

    [HttpPost("log-meal")]
    public async Task<IActionResult> LogMeal([FromBody] LogMealDto dto)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        await clientService.LogMealAsync(clientId, dto);
        return Ok(new { message = "Ogun kaydedildi." });
    }

    [HttpPost("water")]
    public async Task<IActionResult> AddWater([FromBody] AddWaterDto dto)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        await clientService.AddWaterAsync(clientId, dto);
        return Ok(new { message = "Su takibi kaydedildi." });
    }

    [HttpPost("weight")]
    public async Task<IActionResult> AddWeight([FromBody] AddWeightDto dto)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        await clientService.AddWeightAsync(clientId, dto);
        return Ok(new { message = "Kilo girisi kaydedildi." });
    }

    [HttpPost("preview-dietitian-by-code")]
    public async Task<IActionResult> PreviewDietitianByCode([FromBody] ConnectToDietitianRequestDto dto) =>
        Ok(await clientService.PreviewDietitianByCodeAsync(dto));

    [HttpPost("connect-to-dietitian")]
    public async Task<IActionResult> ConnectToDietitian([FromBody] ConnectToDietitianRequestDto dto)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await clientService.ConnectToDietitianAsync(clientId, dto));
    }

    [HttpGet("diet-programs")]
    public async Task<IActionResult> MyDietPrograms()
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await clientService.GetMyDietProgramsAsync(clientId));
    }

    /// <summary>Belirli bir günün güncel programı (takvimde gün değişince / yenileme için).</summary>
    [HttpGet("diet-program")]
    public async Task<IActionResult> MyDietProgramForDate([FromQuery] string programDate)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await clientService.GetMyDietProgramForDateAsync(clientId, programDate));
    }

    [HttpPost("diet-program/meal-completed")]
    public async Task<IActionResult> MarkMealCompleted([FromBody] SetMealCompletedDto dto)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        await clientService.SetMyMealCompletedAsync(clientId, dto.ProgramDate, dto.Meal);
        return Ok(new { message = "Ogun tamamlandi olarak kaydedildi." });
    }
}
