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
}
