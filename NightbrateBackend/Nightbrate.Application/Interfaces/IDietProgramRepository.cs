using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IDietProgramRepository
{
    Task<DietProgram?> GetByDietitianClientAndProgramDateAsync(string dietitianId, string clientId, string programDate);
    Task<List<string>> GetProgramDatesByDietitianAndClientAsync(string dietitianId, string clientId);
    Task<List<DietProgram>> GetAllByClientIdAsync(string clientId);

    /// <summary>Aynı gün birden fazla satır (eski eşleşmeler) varsa en güncel <see cref="DietProgram.UpdatedAt"/> alınır.</summary>
    Task<DietProgram?> GetCurrentByClientIdAndProgramDateAsync(string clientId, string programDate);

    Task UpsertAsync(DietProgram dietProgram);

    /// <summary>Aynı danışan+gün için birden fazla kayıt dönebilir; çağıran en güncel UpdatedAt seçer.</summary>
    Task<List<DietProgram>> GetByDietitianClientsAndProgramDatesAsync(string dietitianId, IReadOnlyCollection<string> clientIds, IReadOnlyCollection<string> programDates, CancellationToken cancellationToken = default);
}
