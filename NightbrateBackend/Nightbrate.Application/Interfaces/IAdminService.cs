using Nightbrate.Application.DTOs;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IAdminService
{
    Task<List<Dietitian>> GetPendingDietitiansAsync();
    Task<AdminDietitianDetailDto> GetDietitianDetailAsync(string dietitianId);
    Task ApproveDietitianAsync(string dietitianId);
    Task<DashboardStatsDto> GetStatsAsync();
}
