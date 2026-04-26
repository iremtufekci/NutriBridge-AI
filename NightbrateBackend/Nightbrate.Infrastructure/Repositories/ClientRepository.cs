using MongoDB.Driver;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class ClientRepository(MongoDbContext context) : IClientRepository
{
    public Task AddAsync(Client client) => context.Clients.InsertOneAsync(client);

    public Task<Client?> GetByIdAsync(string id) =>
        context.Clients.Find(x => x.Id == id).FirstOrDefaultAsync()!;

    public async Task UpdateAsync(Client client)
    {
        if (string.IsNullOrWhiteSpace(client.Id)) throw new AppException("Gecerli danisan profili yok (Id).");
        var r = await context.Clients.ReplaceOneAsync(x => x.Id == client.Id, client);
        if (r.MatchedCount == 0)
            throw new AppException("Danisan profili guncellenemedi: veritabaninda 'Clients' kaydi bulunamadi. Lutfen destekle iletisin.");
    }

    public Task<List<Client>> GetByDietitianIdAsync(string dietitianId) =>
        context.Clients.Find(x => x.DietitianId == dietitianId).ToListAsync();

    public Task<long> GetTotalAsync() =>
        context.Clients.CountDocumentsAsync(Builders<Client>.Filter.Empty);

    public async Task<bool> TryAssignDietitianIfUnassignedAsync(string clientId, string dietitianId)
    {
        var hasNoDietitian = Builders<Client>.Filter.Or(
            Builders<Client>.Filter.Eq(c => c.DietitianId, (string?)null),
            Builders<Client>.Filter.Eq(c => c.DietitianId, string.Empty)
        );
        var filter = Builders<Client>.Filter.Eq(c => c.Id, clientId) & hasNoDietitian;
        var update = Builders<Client>.Update.Set(c => c.DietitianId, dietitianId);
        var result = await context.Clients.UpdateOneAsync(filter, update);
        return result.ModifiedCount == 1;
    }
}
