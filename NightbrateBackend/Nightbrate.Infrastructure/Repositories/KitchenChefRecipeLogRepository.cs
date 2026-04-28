using MongoDB.Bson;
using MongoDB.Driver;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class KitchenChefRecipeLogRepository(MongoDbContext context) : IKitchenChefRecipeLogRepository
{
    public async Task AddAsync(KitchenChefRecipeLog log, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrEmpty(log.Id))
            log.Id = ObjectId.GenerateNewId().ToString();
        await context.KitchenChefRecipeLogs.InsertOneAsync(log, cancellationToken: cancellationToken);
    }

    public async Task<IReadOnlyList<KitchenChefRecipeLog>> GetByClientIdAsync(string clientId, int take, CancellationToken cancellationToken = default)
    {
        if (take < 1) take = 50;
        if (take > 200) take = 200;
        return await context.KitchenChefRecipeLogs
            .Find(x => x.ClientId == clientId)
            .SortByDescending(x => x.CreatedAtUtc)
            .Limit(take)
            .ToListAsync(cancellationToken);
    }

    public async Task<IReadOnlyList<KitchenChefRecipeLog>> GetByClientIdFilteredAsync(
        string clientId,
        DateTime? fromUtcInclusive,
        DateTime? toUtcExclusive,
        string? sourceFilter,
        int skip,
        int take,
        CancellationToken cancellationToken = default)
    {
        if (take < 1) take = 50;
        if (take > 500) take = 500;
        if (skip < 0) skip = 0;

        var builder = Builders<KitchenChefRecipeLog>.Filter;
        var f = builder.Eq(x => x.ClientId, clientId);
        if (fromUtcInclusive.HasValue)
            f &= builder.Gte(x => x.CreatedAtUtc, fromUtcInclusive.Value);
        if (toUtcExclusive.HasValue)
            f &= builder.Lt(x => x.CreatedAtUtc, toUtcExclusive.Value);
        if (!string.IsNullOrWhiteSpace(sourceFilter) &&
            !string.Equals(sourceFilter, "all", StringComparison.OrdinalIgnoreCase))
        {
            var s = sourceFilter.Trim().ToLowerInvariant();
            if (s is "gemini" or "mock")
                f &= builder.Eq(x => x.Source, s);
        }

        return await context.KitchenChefRecipeLogs
            .Find(f)
            .SortByDescending(x => x.CreatedAtUtc)
            .Skip(skip)
            .Limit(take)
            .ToListAsync(cancellationToken);
    }
}
