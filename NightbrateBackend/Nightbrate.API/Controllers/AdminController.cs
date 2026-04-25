using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize(Roles = "Admin")]
public class AdminController(IAdminService adminService, IActivityLogService activityLogService) : ControllerBase
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
        var adminId = User.FindFirstValue("UserId");
        var email = User.FindFirstValue(JwtRegisteredClaimNames.Email)
            ?? User.FindFirstValue(ClaimTypes.Email)
            ?? "";
        var display = string.IsNullOrEmpty(email)
            ? "Yönetici"
            : (email.Contains('@', StringComparison.Ordinal) ? email.Split('@')[0] : email);
        await adminService.ApproveDietitianAsync(dietitianId, adminId, display);
        return Ok(new { message = "Diyetisyen onaylandı." });
    }

    [HttpGet("dashboard-stats")]
    public async Task<IActionResult> DashboardStats() => Ok(await adminService.GetStatsAsync());

    [HttpGet("recent-activities")]
    public async Task<IActionResult> RecentActivities([FromQuery] int take = 15) =>
        Ok(await activityLogService.GetRecentAsync(take));
}
