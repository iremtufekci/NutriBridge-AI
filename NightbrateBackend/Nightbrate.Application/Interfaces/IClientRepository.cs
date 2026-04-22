using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IClientRepository
{
    Task AddAsync(Client client);
    Task<Client?> GetByIdAsync(string id);
    Task UpdateAsync(Client client);
    Task<List<Client>> GetByDietitianIdAsync(string dietitianId);
    Task<long> GetTotalAsync();
}
