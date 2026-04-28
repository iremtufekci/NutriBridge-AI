using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IWaterLogRepository
{
    Task AddAsync(WaterLog waterLog);
    Task<List<WaterLog>> GetByClientIdsInTimestampRangeAsync(IReadOnlyCollection<string> clientIds, DateTime fromUtcInclusive, DateTime toUtcExclusive, CancellationToken cancellationToken = default);
}
