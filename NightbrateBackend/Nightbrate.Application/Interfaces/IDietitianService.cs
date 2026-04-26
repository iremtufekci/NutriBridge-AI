using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface IDietitianService
{
    Task<object> GetClientsWithLastMealAsync(string dietitianId);
    Task<IReadOnlyList<string>> GetDietProgramDatesAsync(string dietitianId, string clientId);
    Task<DietProgramViewDto> GetDietProgramAsync(string dietitianId, string clientId, string programDate);
    Task SaveDietProgramAsync(string dietitianId, SaveDietProgramDto dto);
}
