using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize(Roles = "Admin")]
public class AdminController(IAdminService adminService) : ControllerBase
{
    [HttpGet("pending-dietitians")]
    public async Task<IActionResult> PendingDietitians() =>
        Ok(await adminService.GetPendingDietitiansAsync());

    [HttpGet("dietitian/{dietitianId}")]
    public async Task<IActionResult> DietitianDetail(string dietitianId) =>
        Ok(await adminService.GetDietitianDetailAsync(dietitianId));

    [HttpPost("approve-dietitian/{dietitianId}")]
    public async Task<IActionResult> ApproveDietitian(string dietitianId)
    {
        await adminService.ApproveDietitianAsync(dietitianId);
        return Ok(new { message = "Diyetisyen onaylandı." });
    }

    [HttpGet("dashboard-stats")]
    public async Task<IActionResult> DashboardStats() => Ok(await adminService.GetStatsAsync());
}
