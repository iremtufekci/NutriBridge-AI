using MongoDB.Bson;
using MongoDB.Driver;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class DietitianRepository(MongoDbContext context) : IDietitianRepository
{
    public Task AddAsync(Dietitian dietitian) => context.Dietitians.InsertOneAsync(dietitian);

    public Task<List<Dietitian>> GetPendingAsync() =>
        context.Dietitians.Find(x => !x.IsApproved).ToListAsync();

    public Task<Dietitian?> GetByIdAsync(string id) =>
        context.Dietitians.Find(x => x.Id == id).FirstOrDefaultAsync()!;

    public Task<Dietitian?> GetByEmailAsync(string email)
    {
        if (string.IsNullOrWhiteSpace(email)) return Task.FromResult<Dietitian?>(null);
        var e = email.Trim().ToLowerInvariant();
        return context.Dietitians.Find(x => x.Email == e).FirstOrDefaultAsync()!;
    }

    public Task UpdateAsync(Dietitian dietitian) =>
        context.Dietitians.ReplaceOneAsync(x => x.Id == dietitian.Id, dietitian);

    public Task<long> GetTotalAsync() =>
        context.Dietitians.CountDocumentsAsync(Builders<Dietitian>.Filter.Empty);

    public Task<long> GetApprovedCountAsync() =>
        context.Dietitians.CountDocumentsAsync(x => x.IsApproved);

    public async Task<bool> ConnectionCodeExistsAsync(string connectionCode) =>
        await context.Dietitians.CountDocumentsAsync(x => x.ConnectionCode == connectionCode) > 0;

    public Task<Dietitian?> GetApprovedByConnectionCodeAsync(string connectionCode) =>
        context.Dietitians
            .Find(x => x.IsApproved && x.ConnectionCode == connectionCode)
            .FirstOrDefaultAsync()!;

    public async Task<string?> GetConnectionCodeByDietitianIdRawAsync(string dietitianId)
    {
        if (string.IsNullOrWhiteSpace(dietitianId)) return null;
        if (!ObjectId.TryParse(dietitianId, out var oid)) return null;
        var doc = await context.DietitiansBson
            .Find(Builders<BsonDocument>.Filter.Eq("_id", oid))
            .FirstOrDefaultAsync();
        if (doc is null) return null;
        foreach (var key in new[] { "ConnectionCode", "connectionCode" })
        {
            if (!doc.Contains(key) || doc[key] is not BsonValue v || v.IsBsonNull) continue;
            if (v.IsString) return v.AsString;
            return v.ToString() ?? null;
        }
        return null;
    }
}
