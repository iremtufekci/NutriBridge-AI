using MongoDB.Driver;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class UserRepository(MongoDbContext context) : IUserRepository
{
    public Task<BaseUser?> GetByEmailAsync(string email) =>
        context.Users.Find(x => x.Email == email).FirstOrDefaultAsync()!;

    public async Task<BaseUser?> GetByIdAsync(string id)
    {
        var user = await context.Clients.Find(x => x.Id == id).FirstOrDefaultAsync();
        if (user is not null) return user;
        return await context.Dietitians.Find(x => x.Id == id).FirstOrDefaultAsync()!;
    }

    public Task AddAsync(BaseUser user) => context.Users.InsertOneAsync(user);

    public Task<long> GetTotalUsersAsync() =>
        context.Users.CountDocumentsAsync(Builders<BaseUser>.Filter.Empty);

    public Task SetDietitianIsApprovedInUsersCollectionAsync(string dietitianId, bool isApproved)
    {
        var filter = Builders<BaseUser>.Filter.Eq(x => x.Id, dietitianId);
        var update = Builders<BaseUser>.Update.Set(nameof(Dietitian.IsApproved), isApproved);
        return context.Users.UpdateOneAsync(filter, update);
    }
}
