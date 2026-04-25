using MongoDB.Bson;
using MongoDB.Driver;
using Nightbrate.Application.DTOs;
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
        if (string.IsNullOrWhiteSpace(id)) return null;

        var inUsers = await context.Users.Find(x => x.Id == id).FirstOrDefaultAsync()!;
        if (inUsers is not null) return inUsers;

        // Eski/yan koleksiyonlarda sadece duran belgeler icin
        var client = await context.Clients.Find(x => x.Id == id).FirstOrDefaultAsync()!;
        if (client is not null) return client;
        return await context.Dietitians.Find(x => x.Id == id).FirstOrDefaultAsync()!;
    }

    public Task AddAsync(BaseUser user) => context.Users.InsertOneAsync(user);

    public Task<long> GetTotalUsersAsync() =>
        context.Users.CountDocumentsAsync(Builders<BaseUser>.Filter.Empty);

    public Task<long> CountByRoleAsync(UserRole role) =>
        context.Users.CountDocumentsAsync(x => x.Role == role);

    public async Task<IReadOnlyList<MonthlyRegistrationDto>> GetMonthlyUserRegistrationsAsync(int monthsBack)
    {
        if (monthsBack < 1) monthsBack = 6;
        if (monthsBack > 24) monthsBack = 24;

        var now = DateTime.UtcNow;
        var firstThisMonth = new DateTime(now.Year, now.Month, 1, 0, 0, 0, DateTimeKind.Utc);
        var rangeStart = firstThisMonth.AddMonths(-(monthsBack - 1));

        var filter = Builders<BaseUser>.Filter.Gte(x => x.CreatedAt, rangeStart);
        var all = await context.Users.Find(filter).ToListAsync();
        var counts = new Dictionary<(int y, int m), long>();
        foreach (var u in all)
        {
            var t = u.CreatedAt.Kind == DateTimeKind.Unspecified
                ? DateTime.SpecifyKind(u.CreatedAt, DateTimeKind.Utc)
                : u.CreatedAt.ToUniversalTime();
            var key = (t.Year, t.Month);
            counts[key] = counts.GetValueOrDefault(key) + 1;
        }

        var list = new List<MonthlyRegistrationDto>(monthsBack);
        for (var i = 0; i < monthsBack; i++)
        {
            var d = rangeStart.AddMonths(i);
            list.Add(new MonthlyRegistrationDto
            {
                Year = d.Year,
                Month = d.Month,
                Count = counts.GetValueOrDefault((d.Year, d.Month), 0L)
            });
        }
        return list;
    }

    public Task SetDietitianIsApprovedInUsersCollectionAsync(string dietitianId, bool isApproved)
    {
        var filter = Builders<BaseUser>.Filter.Eq(x => x.Id, dietitianId);
        var update = Builders<BaseUser>.Update.Set(nameof(Dietitian.IsApproved), isApproved);
        return context.Users.UpdateOneAsync(filter, update);
    }

    public Task SetDietitianConnectionCodeInUsersCollectionAsync(string dietitianId, string connectionCode)
    {
        var filter = Builders<BaseUser>.Filter.Eq(x => x.Id, dietitianId);
        var update = Builders<BaseUser>.Update.Set(nameof(Dietitian.ConnectionCode), connectionCode);
        return context.Users.UpdateOneAsync(filter, update);
    }

    public Task SetClientDietitianIdInUsersCollectionAsync(string clientId, string dietitianId)
    {
        var filter = Builders<BaseUser>.Filter.And(
            Builders<BaseUser>.Filter.Eq(x => x.Id, clientId),
            Builders<BaseUser>.Filter.Eq(x => x.Role, UserRole.Client)
        );
        var update = Builders<BaseUser>.Update.Set(nameof(Client.DietitianId), dietitianId);
        return context.Users.UpdateOneAsync(filter, update);
    }

    public async Task<string?> GetConnectionCodeFromUsersBsonByUserIdAsync(string userId)
    {
        if (string.IsNullOrWhiteSpace(userId)) return null;
        if (!MongoDB.Bson.ObjectId.TryParse(userId, out var oid)) return null;
        var doc = await context.UsersBson
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
