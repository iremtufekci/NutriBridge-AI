using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.API.Controllers;

[ApiController]
[Route("api/[controller]")] // api/Client
[Authorize(Roles = "Client")] // Tüm endpoint'ler danışan rolü ister
public class ClientController(IClientService clientService) : ControllerBase
{
    [HttpGet("profile")] // GET api/Client/profile — profil kartı
    public async Task<IActionResult> Profile()
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty; // JWT'den id
        return Ok(await clientService.GetProfileAsync(clientId));
    }

    [HttpPost("profile")] // Profil güncelle (eski istemciler POST kullanır)
    [HttpPut("profile")] // REST uyumlu PUT da kabul
    public async Task<IActionResult> UpdateProfile([FromBody] UpdateClientProfileDto? dto)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        await clientService.UpdateProfileAsync(clientId, dto); // Ad, hedef kalori vb.
        return Ok(new { message = "Profil bilgileriniz guncellendi." });
    }

    [HttpPost("theme")] // Danışan tema tercihi
    public async Task<IActionResult> UpdateTheme([FromBody] UpdateThemePreferenceDto dto)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        await clientService.UpdateThemePreferenceAsync(clientId, dto.ThemePreference);
        return Ok(new { message = "Tema tercihi kaydedildi." });
    }

    [HttpPost("log-meal")] // Manuel öğün kaydı (foto dışı)
    public async Task<IActionResult> LogMeal([FromBody] LogMealDto dto)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        await clientService.LogMealAsync(clientId, dto);
        return Ok(new { message = "Ogun kaydedildi." });
    }

    [HttpPost("water")] // Su takibi ekle
    public async Task<IActionResult> AddWater([FromBody] AddWaterDto dto)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        await clientService.AddWaterAsync(clientId, dto);
        return Ok(new { message = "Su takibi kaydedildi." });
    }

    [HttpPost("weight")] // Kilo girişi
    public async Task<IActionResult> AddWeight([FromBody] AddWeightDto dto)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        await clientService.AddWeightAsync(clientId, dto);
        return Ok(new { message = "Kilo girisi kaydedildi." });
    }

    [HttpPost("preview-dietitian-by-code")] // Bağlanmadan önce kodu doğrula
    public async Task<IActionResult> PreviewDietitianByCode([FromBody] ConnectToDietitianRequestDto dto) =>
        Ok(await clientService.PreviewDietitianByCodeAsync(dto));

    [HttpPost("connect-to-dietitian")] // Diyetisyen kodu ile eşleş
    public async Task<IActionResult> ConnectToDietitian([FromBody] ConnectToDietitianRequestDto dto)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await clientService.ConnectToDietitianAsync(clientId, dto));
    }

    [HttpGet("diet-programs")] // Geçmiş tüm program günleri listesi
    public async Task<IActionResult> MyDietPrograms()
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await clientService.GetMyDietProgramsAsync(clientId));
    }

    [HttpGet("diet-program")] // Tek gün programı (?programDate=2026-06-07)
    public async Task<IActionResult> MyDietProgramForDate([FromQuery] string programDate)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await clientService.GetMyDietProgramForDateAsync(clientId, programDate));
    }

    [HttpPost("diet-program/meal-completed")] // Öğünü tamamladım işareti
    public async Task<IActionResult> MarkMealCompleted([FromBody] SetMealCompletedDto dto)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        await clientService.SetMyMealCompletedAsync(clientId, dto.ProgramDate, dto.Meal); // kahvaltı/öğle/akşam
        return Ok(new { message = "Ogun tamamlandi olarak kaydedildi." });
    }
}
