using System.IdentityModel.Tokens.Jwt; // JWT e-posta claim'i
using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.API.Controllers;

[ApiController]
[Route("api/[controller]")] // api/Admin
[Authorize(Roles = "Admin")] // Sadece yönetici
public class AdminController(
    IAdminService adminService,
    IActivityLogService activityLogService,
    ISystemAnalyticsService systemAnalyticsService) : ControllerBase
{
    [HttpGet("pending-dietitians")] // Onay bekleyen diyetisyen listesi
    public async Task<IActionResult> PendingDietitians() =>
        Ok(await adminService.GetPendingDietitiansAsync());

    [HttpGet("dietitian/{dietitianId}")] // Tek diyetisyen detayı (diploma vb.)
    public async Task<IActionResult> DietitianDetail(string dietitianId) =>
        Ok(await adminService.GetDietitianDetailAsync(dietitianId));

    [HttpPost("approve-dietitian/{dietitianId}")] // Diyetisyeni aktif et
    public async Task<IActionResult> ApproveDietitian(string dietitianId)
    {
        var adminId = User.FindFirstValue("UserId"); // İşlemi yapan admin
        var email = User.FindFirstValue(JwtRegisteredClaimNames.Email)
            ?? User.FindFirstValue(ClaimTypes.Email)
            ?? "";
        var display = string.IsNullOrEmpty(email) // Aktivite logu için görünen ad
            ? "Yönetici"
            : (email.Contains('@', StringComparison.Ordinal) ? email.Split('@')[0] : email);
        await adminService.ApproveDietitianAsync(dietitianId, adminId, display);
        return Ok(new { message = "Diyetisyen onaylandı." });
    }

    [HttpGet("dashboard-stats")] // Özet kart sayıları
    public async Task<IActionResult> DashboardStats() => Ok(await adminService.GetStatsAsync());

    [HttpGet("recent-activities")] // Son sistem olayları
    public async Task<IActionResult> RecentActivities([FromQuery] int take = 15) =>
        Ok(await activityLogService.GetRecentAsync(take));

    [HttpGet("system-analytics")] // Grafik verileri
    public async Task<IActionResult> SystemAnalytics() =>
        Ok(await systemAnalyticsService.GetAsync());

    [HttpGet("user-management/stats")] // Kullanıcı sayı özeti
    public async Task<IActionResult> UserManagementStats() =>
        Ok(await adminService.GetUserManagementStatsAsync());

    [HttpGet("user-management/users")] // Filtrelenebilir kullanıcı tablosu
    public async Task<IActionResult> UserManagementUsers(
        [FromQuery] string? q, // Arama
        [FromQuery] string? role, // client/dietitian/admin
        [FromQuery] string? status) => // active/suspended
        Ok(await adminService.GetUsersListAsync(q, role, status));

    [HttpGet("user-management/{userId}/activity-logs")] // Kullanıcı işlem geçmişi
    public async Task<IActionResult> UserActivityLogs(string userId, [FromQuery] int take = 30) =>
        Ok(await activityLogService.GetByUserIdAsync(userId, take));

    [HttpPost("user-management/{userId}/suspend")] // Hesabı askıya al
    public async Task<IActionResult> SuspendUser(string userId, [FromBody] SetUserSuspensionRequestDto? body)
    {
        var adminId = User.FindFirstValue("UserId");
        var email = User.FindFirstValue(JwtRegisteredClaimNames.Email)
            ?? User.FindFirstValue(ClaimTypes.Email)
            ?? "";
        var display = string.IsNullOrEmpty(email)
            ? "Yönetici"
            : (email.Contains('@', StringComparison.Ordinal) ? email.Split('@')[0] : email);
        await adminService.SuspendUserAsync(userId, body?.Message ?? "", adminId, display);
        return Ok(new { message = "Kullanıcı askıya alındı." });
    }

    [HttpPost("user-management/{userId}/unsuspend")] // Askıyı kaldır
    public async Task<IActionResult> UnsuspendUser(string userId)
    {
        var adminId = User.FindFirstValue("UserId");
        var email = User.FindFirstValue(JwtRegisteredClaimNames.Email)
            ?? User.FindFirstValue(ClaimTypes.Email)
            ?? "";
        var display = string.IsNullOrEmpty(email)
            ? "Yönetici"
            : (email.Contains('@', StringComparison.Ordinal) ? email.Split('@')[0] : email);
        await adminService.UnsuspendUserAsync(userId, adminId, display);
        return Ok(new { message = "Askı kaldırıldı." });
    }
}
