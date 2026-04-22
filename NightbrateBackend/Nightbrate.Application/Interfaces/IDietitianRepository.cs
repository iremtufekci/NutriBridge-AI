using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IDietitianRepository
{
    Task AddAsync(Dietitian dietitian);
    Task<List<Dietitian>> GetPendingAsync();
    Task<Dietitian?> GetByIdAsync(string id);
    Task UpdateAsync(Dietitian dietitian);
    Task<long> GetTotalAsync();
    Task<long> GetApprovedCountAsync();
}
