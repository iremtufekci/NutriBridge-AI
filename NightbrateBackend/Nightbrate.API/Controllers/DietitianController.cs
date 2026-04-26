using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize(Roles = "Dietitian")]
public class DietitianController(IDietitianService dietitianService) : ControllerBase
{
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

    [HttpPost("diet-program")]
    public async Task<IActionResult> SaveDietProgram([FromBody] SaveDietProgramDto dto)
    {
        var dietitianId = User.FindFirstValue("UserId") ?? string.Empty;
        await dietitianService.SaveDietProgramAsync(dietitianId, dto);
        return Ok(new { message = "Diyet programi kaydedildi." });
    }
}
