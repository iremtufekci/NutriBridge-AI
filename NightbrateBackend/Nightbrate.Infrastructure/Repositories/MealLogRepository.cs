using MongoDB.Bson;
using MongoDB.Driver;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class MealLogRepository(MongoDbContext context) : IMealLogRepository
{
    public async Task AddAsync(MealLog mealLog)
    {
        if (string.IsNullOrEmpty(mealLog.Id))
            mealLog.Id = ObjectId.GenerateNewId().ToString();
        await context.MealLogs.InsertOneAsync(mealLog);
    }

    public Task<MealLog?> GetLastByClientIdAsync(string clientId) =>
        context.MealLogs.Find(x => x.ClientId == clientId)
            .SortByDescending(x => x.Timestamp)
            .FirstOrDefaultAsync()!;

    public async Task<List<MealLog>> GetByClientIdsInTimestampRangeAsync(
        IReadOnlyCollection<string> clientIds,
        DateTime fromUtcInclusive,
        DateTime toUtcExclusive,
        CancellationToken cancellationToken = default)
    {
        if (clientIds.Count == 0) return new List<MealLog>();
        var f = Builders<MealLog>.Filter.In(m => m.ClientId, clientIds)
                & Builders<MealLog>.Filter.Gte(m => m.Timestamp, fromUtcInclusive)
                & Builders<MealLog>.Filter.Lt(m => m.Timestamp, toUtcExclusive);
        return await context.MealLogs.Find(f).ToListAsync(cancellationToken);
    }
}
