using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IActivityLogRepository
{
    Task AddAsync(ActivityLog log);
    Task<List<ActivityLog>> GetRecentAsync(int take);
}
