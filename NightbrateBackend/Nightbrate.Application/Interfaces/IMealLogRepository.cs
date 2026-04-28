using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IMealLogRepository
{
    Task AddAsync(MealLog mealLog);
    Task<MealLog?> GetLastByClientIdAsync(string clientId);
    Task<List<MealLog>> GetByClientIdsInTimestampRangeAsync(IReadOnlyCollection<string> clientIds, DateTime fromUtcInclusive, DateTime toUtcExclusive, CancellationToken cancellationToken = default);
}
