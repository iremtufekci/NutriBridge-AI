using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IDietitianRepository
{
    Task AddAsync(Dietitian dietitian);
    Task<List<Dietitian>> GetPendingAsync();
    Task<Dietitian?> GetByIdAsync(string id);
    Task<Dietitian?> GetByEmailAsync(string email);
    Task UpdateAsync(Dietitian dietitian);
    Task<long> GetTotalAsync();
    Task<long> GetApprovedCountAsync();
    Task<bool> ConnectionCodeExistsAsync(string connectionCode);
    Task<Dietitian?> GetApprovedByConnectionCodeAsync(string connectionCode);
    /// <summary>Tip eslemesi bosa dustugunde: BSON'da ConnectionCode / connectionCode alanindan yukler.</summary>
    Task<string?> GetConnectionCodeByDietitianIdRawAsync(string dietitianId);
}
