using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface IDietitianService
{
    Task<object> GetClientsWithLastMealAsync(string dietitianId);
    Task<DietitianMyClientsResponseDto> GetMyClientsAsync(
        string dietitianId,
        string sort,
        string tab,
        CancellationToken cancellationToken = default);
    Task<DietitianClientOverviewDto?> GetClientOverviewAsync(
        string dietitianId,
        string clientId,
        CancellationToken cancellationToken = default);
    Task<IReadOnlyList<string>> GetDietProgramDatesAsync(string dietitianId, string clientId);
    Task<DietProgramViewDto> GetDietProgramAsync(string dietitianId, string clientId, string programDate);
    Task SaveDietProgramAsync(string dietitianId, SaveDietProgramDto dto);
    Task<IReadOnlyList<KitchenChefRecipeLogItemDto>> GetClientKitchenRecipeLogsAsync(string dietitianId, string clientId, int take, CancellationToken cancellationToken = default);
    Task<ClientBriefDto?> GetClientBriefAsync(string dietitianId, string clientId, CancellationToken cancellationToken = default);
}
