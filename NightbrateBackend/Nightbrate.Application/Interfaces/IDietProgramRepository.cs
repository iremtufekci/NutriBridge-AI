using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IDietProgramRepository
{
    Task<DietProgram?> GetByClientAndDayAsync(string clientId, string dayOfWeek);
    Task UpsertAsync(DietProgram dietProgram);
}
