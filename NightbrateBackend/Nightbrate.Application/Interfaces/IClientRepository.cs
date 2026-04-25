using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IClientRepository
{
    Task AddAsync(Client client);
    Task<Client?> GetByIdAsync(string id);
    Task UpdateAsync(Client client);
    /// <summary>DietitianId bostaysa ayarla; aksi halde false (atomik filtre).</summary>
    Task<bool> TryAssignDietitianIfUnassignedAsync(string clientId, string dietitianId);
    Task<List<Client>> GetByDietitianIdAsync(string dietitianId);
    Task<long> GetTotalAsync();
}
