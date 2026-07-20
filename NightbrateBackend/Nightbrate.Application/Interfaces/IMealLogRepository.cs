using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IMealLogRepository
{
    Task AddAsync(MealLog mealLog);
    Task<MealLog?> GetLastByClientIdAsync(string clientId);
    Task<IReadOnlyList<MealLog>> GetByClientIdAsync(string clientId, int take, CancellationToken cancellationToken = default);
    Task<IReadOnlyList<MealLog>> GetByClientIdsAsync(IReadOnlyCollection<string> clientIds, int take, CancellationToken cancellationToken = default);
    Task<List<MealLog>> GetByClientIdsInTimestampRangeAsync(IReadOnlyCollection<string> clientIds, DateTime fromUtcInclusive, DateTime toUtcExclusive, CancellationToken cancellationToken = default);
}
