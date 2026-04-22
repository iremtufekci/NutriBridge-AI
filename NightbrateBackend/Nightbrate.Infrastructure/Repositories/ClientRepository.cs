using MongoDB.Driver;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class ClientRepository(MongoDbContext context) : IClientRepository
{
    public Task AddAsync(Client client) => context.Clients.InsertOneAsync(client);

    public Task<Client?> GetByIdAsync(string id) =>
        context.Clients.Find(x => x.Id == id).FirstOrDefaultAsync()!;

    public Task UpdateAsync(Client client) =>
        context.Clients.ReplaceOneAsync(x => x.Id == client.Id, client);

    public Task<List<Client>> GetByDietitianIdAsync(string dietitianId) =>
        context.Clients.Find(x => x.DietitianId == dietitianId).ToListAsync();

    public Task<long> GetTotalAsync() =>
        context.Clients.CountDocumentsAsync(Builders<Client>.Filter.Empty);
}
