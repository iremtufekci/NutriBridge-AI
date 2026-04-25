using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface IAdminService
{
    Task<List<PendingDietitianListItemDto>> GetPendingDietitiansAsync();
    Task<AdminDietitianDetailDto> GetDietitianDetailAsync(string dietitianId);
    Task ApproveDietitianAsync(string dietitianId, string? adminUserId, string adminDisplayName);
    Task<DashboardStatsDto> GetStatsAsync();
}
