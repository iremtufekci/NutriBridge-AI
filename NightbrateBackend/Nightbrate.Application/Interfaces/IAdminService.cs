using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface IAdminService
{
    Task<List<PendingDietitianListItemDto>> GetPendingDietitiansAsync();
    Task<AdminDietitianDetailDto> GetDietitianDetailAsync(string dietitianId);
    Task ApproveDietitianAsync(string dietitianId, string? adminUserId, string adminDisplayName);
    Task<DashboardStatsDto> GetStatsAsync();
    Task<UserManagementStatsDto> GetUserManagementStatsAsync();
    Task<List<AdminUserRowDto>> GetUsersListAsync(
        string? search,
        string? roleFilter,
        string? statusFilter);
    Task SuspendUserAsync(
        string targetUserId,
        string message,
        string? adminUserId,
        string adminDisplayName);
    Task UnsuspendUserAsync(
        string targetUserId,
        string? adminUserId,
        string adminDisplayName);
}
