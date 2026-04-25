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
}
