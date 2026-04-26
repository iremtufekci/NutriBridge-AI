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

    public async Task UpdateThemePreferenceAllStoresAsync(string userId, string themePreference)
    {
        if (string.IsNullOrWhiteSpace(userId)) return;

        var t = string.Equals(themePreference, "dark", StringComparison.OrdinalIgnoreCase) ? "dark" : "light";

        var f = Builders<BaseUser>.Filter.Eq(x => x.Id, userId);
        var u = Builders<BaseUser>.Update.Set(x => x.ThemePreference, t);
        await context.Users.UpdateOneAsync(f, u);

        _ = await context.Clients.UpdateOneAsync(
            Builders<Client>.Filter.Eq(c => c.Id, userId),
            Builders<Client>.Update.Set(c => c.ThemePreference, t));

        _ = await context.Dietitians.UpdateOneAsync(
            Builders<Dietitian>.Filter.Eq(d => d.Id, userId),
            Builders<Dietitian>.Update.Set(d => d.ThemePreference, t));
    }

    public Task<List<BaseUser>> GetAllUsersForAdminAsync() =>
        context.Users.Find(_ => true).ToListAsync()!;

    public async Task SetUserSuspensionAllStoresAsync(string userId, bool isSuspended, string? message, DateTime? suspendedAt)
    {
        if (string.IsNullOrWhiteSpace(userId)) return;

        var fU = Builders<BaseUser>.Filter.Eq(x => x.Id, userId);
        var uU = Builders<BaseUser>.Update
            .Set(x => x.IsSuspended, isSuspended)
            .Set(x => x.SuspensionMessage, isSuspended ? message : null)
            .Set(x => x.SuspendedAt, suspendedAt);
        await context.Users.UpdateOneAsync(fU, uU);

        _ = await context.Clients.UpdateOneAsync(
            Builders<Client>.Filter.Eq(c => c.Id, userId),
            Builders<Client>.Update
                .Set(c => c.IsSuspended, isSuspended)
                .Set(c => c.SuspensionMessage, isSuspended ? message : null)
                .Set(c => c.SuspendedAt, suspendedAt));

        _ = await context.Dietitians.UpdateOneAsync(
            Builders<Dietitian>.Filter.Eq(d => d.Id, userId),
            Builders<Dietitian>.Update
                .Set(d => d.IsSuspended, isSuspended)
                .Set(d => d.SuspensionMessage, isSuspended ? message : null)
                .Set(d => d.SuspendedAt, suspendedAt));
    }

    public Task UpdateClientProfileInUsersCollectionAsync(
        string clientId,
        string firstName,
        string lastName,
        double weight,
        double height,
        int targetCalories)
    {
        if (string.IsNullOrWhiteSpace(clientId)) return Task.CompletedTask;
        var f = Builders<BaseUser>.Filter.And(
            Builders<BaseUser>.Filter.Eq(x => x.Id, clientId),
            Builders<BaseUser>.Filter.Eq(x => x.Role, UserRole.Client));
        var u = Builders<BaseUser>.Update
            .Set(nameof(Client.FirstName), firstName)
            .Set(nameof(Client.LastName), lastName)
            .Set(nameof(Client.Weight), weight)
            .Set(nameof(Client.Height), height)
            .Set(nameof(Client.TargetCalories), targetCalories);
        return context.Users.UpdateOneAsync(f, u);
    }

    public async Task<(string? FirstName, string? LastName)> GetAdminNameFromUsersBsonAsync(string userId)
    {
        if (string.IsNullOrWhiteSpace(userId) || !MongoDB.Bson.ObjectId.TryParse(userId, out var oid))
            return (null, null);
        var doc = await context.UsersBson
            .Find(Builders<BsonDocument>.Filter.Eq("_id", oid))
            .FirstOrDefaultAsync();
        if (doc is null) return (null, null);

        static string? GetString(BsonDocument d, params string[] keys)
        {
            foreach (var key in keys)
            {
                if (!d.Contains(key)) continue;
                var v = d[key];
                if (v is null || v.IsBsonNull) continue;
                if (v.IsString) return v.AsString;
            }
            return null;
        }

        return (
            GetString(doc, "firstName", "FirstName", "givenName", "ad"),
            GetString(doc, "lastName", "LastName", "familyName", "surname", "soyad"));
    }
}
