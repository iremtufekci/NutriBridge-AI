using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface IActivityLogService
{
    Task LogAsync(string? userId, string actorDisplayName, string description);
    Task<IReadOnlyList<ActivityItemDto>> GetRecentAsync(int take);
}
