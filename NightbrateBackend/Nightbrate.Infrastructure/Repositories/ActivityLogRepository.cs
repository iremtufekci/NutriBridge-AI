using MongoDB.Driver;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class ActivityLogRepository(MongoDbContext context) : IActivityLogRepository
{
    public Task AddAsync(ActivityLog log) => context.ActivityLogs.InsertOneAsync(log);

    public async Task<List<ActivityLog>> GetRecentAsync(int take)
    {
        if (take < 1) take = 20;
        if (take > 100) take = 100;
        return await context.ActivityLogs
            .Find(_ => true)
            .SortByDescending(x => x.CreatedAt)
            .Limit(take)
            .ToListAsync();
    }

    public async Task<List<ActivityLog>> GetByUserIdAsync(string userId, int take)
    {
        if (string.IsNullOrWhiteSpace(userId)) return new List<ActivityLog>();
        if (take < 1) take = 20;
        if (take > 200) take = 200;
        return await context.ActivityLogs
            .Find(x => x.UserId == userId)
            .SortByDescending(x => x.CreatedAt)
            .Limit(take)
            .ToListAsync();
    }

    public async Task<Dictionary<string, DateTime>> GetLastActivityByUserIdsAsync(IReadOnlyList<string> userIds)
    {
        if (userIds.Count == 0) return new Dictionary<string, DateTime>();
        var set = new HashSet<string>(userIds.Where(s => !string.IsNullOrWhiteSpace(s))!);
        if (set.Count == 0) return new Dictionary<string, DateTime>();

        var filter = Builders<ActivityLog>.Filter.In(x => x.UserId, set);
        var logs = await context.ActivityLogs.Find(filter).ToListAsync();
        var dict = new Dictionary<string, DateTime>(StringComparer.Ordinal);
        foreach (var l in logs)
        {
            if (string.IsNullOrEmpty(l.UserId)) continue;
            if (!set.Contains(l.UserId)) continue;
            if (!dict.TryGetValue(l.UserId, out var prev) || l.CreatedAt > prev)
                dict[l.UserId] = l.CreatedAt;
        }
        return dict;
    }
}
