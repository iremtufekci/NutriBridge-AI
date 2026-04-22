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

    public Task UpdateAsync(Dietitian dietitian) =>
        context.Dietitians.ReplaceOneAsync(x => x.Id == dietitian.Id, dietitian);

    public Task<long> GetTotalAsync() =>
        context.Dietitians.CountDocumentsAsync(Builders<Dietitian>.Filter.Empty);

    public Task<long> GetApprovedCountAsync() =>
        context.Dietitians.CountDocumentsAsync(x => x.IsApproved);
}
