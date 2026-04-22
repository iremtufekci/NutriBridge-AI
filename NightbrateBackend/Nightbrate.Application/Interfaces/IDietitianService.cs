using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface IDietitianService
{
    Task<object> GetClientsWithLastMealAsync(string dietitianId);
    Task SaveDietProgramAsync(string dietitianId, SaveDietProgramDto dto);
}
