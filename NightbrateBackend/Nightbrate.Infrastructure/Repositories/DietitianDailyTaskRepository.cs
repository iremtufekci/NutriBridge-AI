using MongoDB.Bson;
using MongoDB.Driver;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class DietitianDailyTaskRepository(MongoDbContext context) : IDietitianDailyTaskRepository
{
    public async Task<IReadOnlyList<DietitianDailyTask>> GetByDietitianAndTaskDateAsync(
        string dietitianId,
        string taskDateYmd,
        CancellationToken cancellationToken = default)
    {
        var list = await context.DietitianDailyTasks
            .Find(x => x.DietitianId == dietitianId && x.TaskDate == taskDateYmd)
            .SortBy(x => x.SortPriority)
            .ThenBy(x => x.Title)
            .ToListAsync(cancellationToken);
        return list;
    }

    public Task<DietitianDailyTask?> GetByIdAsync(string id, CancellationToken cancellationToken = default) =>
        context.DietitianDailyTasks.Find(x => x.Id == id).FirstOrDefaultAsync(cancellationToken)!;

    public async Task InsertAsync(DietitianDailyTask task, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrEmpty(task.Id))
            task.Id = ObjectId.GenerateNewId().ToString();
        await context.DietitianDailyTasks.InsertOneAsync(task, cancellationToken: cancellationToken);
    }

    public Task UpdateContentAsync(
        string id,
        string title,
        string subtitle,
        int sortPriority,
        DateTime updatedAtUtc,
        CancellationToken cancellationToken = default)
    {
        var u = Builders<DietitianDailyTask>.Update
            .Set(x => x.Title, title)
            .Set(x => x.Subtitle, subtitle)
            .Set(x => x.SortPriority, sortPriority)
            .Set(x => x.UpdatedAtUtc, updatedAtUtc);
        return context.DietitianDailyTasks.UpdateOneAsync(x => x.Id == id, u, cancellationToken: cancellationToken);
    }

    public Task UpdateCompletionAsync(
        string id,
        bool isCompleted,
        DateTime? completedAtUtc,
        DateTime updatedAtUtc,
        CancellationToken cancellationToken = default)
    {
        var u = Builders<DietitianDailyTask>.Update
            .Set(x => x.IsCompleted, isCompleted)
            .Set(x => x.CompletedAtUtc, completedAtUtc)
            .Set(x => x.UpdatedAtUtc, updatedAtUtc);
        return context.DietitianDailyTasks.UpdateOneAsync(x => x.Id == id, u, cancellationToken: cancellationToken);
    }

    public Task DeleteManyByIdsAsync(IReadOnlyCollection<string> ids, CancellationToken cancellationToken = default)
    {
        if (ids.Count == 0) return Task.CompletedTask;
        return context.DietitianDailyTasks.DeleteManyAsync(x => ids.Contains(x.Id!), cancellationToken);
    }
}
