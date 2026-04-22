using MongoDB.Driver;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class MealLogRepository(MongoDbContext context) : IMealLogRepository
{
    public Task AddAsync(MealLog mealLog) => context.MealLogs.InsertOneAsync(mealLog);

    public Task<MealLog?> GetLastByClientIdAsync(string clientId) =>
        context.MealLogs.Find(x => x.ClientId == clientId)
            .SortByDescending(x => x.Timestamp)
            .FirstOrDefaultAsync()!;
}
