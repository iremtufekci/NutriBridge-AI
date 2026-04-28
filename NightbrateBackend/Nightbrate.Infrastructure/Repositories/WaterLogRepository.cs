using MongoDB.Bson;
using MongoDB.Driver;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class WaterLogRepository(MongoDbContext context) : IWaterLogRepository
{
    public Task AddAsync(WaterLog waterLog) => context.WaterLogs.InsertOneAsync(waterLog);

    public async Task<List<WaterLog>> GetByClientIdsInTimestampRangeAsync(
        IReadOnlyCollection<string> clientIds,
        DateTime fromUtcInclusive,
        DateTime toUtcExclusive,
        CancellationToken cancellationToken = default)
    {
        if (clientIds.Count == 0) return new List<WaterLog>();
        var f = Builders<WaterLog>.Filter.In(w => w.ClientId, clientIds)
                & Builders<WaterLog>.Filter.Gte(w => w.Timestamp, fromUtcInclusive)
                & Builders<WaterLog>.Filter.Lt(w => w.Timestamp, toUtcExclusive);
        return await context.WaterLogs.Find(f).ToListAsync(cancellationToken);
    }
}
