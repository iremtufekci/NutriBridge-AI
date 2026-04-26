using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface IClientService
{
    Task LogMealAsync(string clientId, LogMealDto dto);
    Task AddWaterAsync(string clientId, AddWaterDto dto);
    Task AddWeightAsync(string clientId, AddWeightDto dto);
    Task<ClientProfileDto> GetProfileAsync(string clientId);
    Task UpdateProfileAsync(string clientId, UpdateClientProfileDto? dto);
    Task UpdateThemePreferenceAsync(string clientId, string themePreference);
    Task<ConnectToDietitianResultDto> ConnectToDietitianAsync(string clientId, ConnectToDietitianRequestDto dto);
    Task<PreviewDietitianByCodeResultDto> PreviewDietitianByCodeAsync(ConnectToDietitianRequestDto dto);
    Task<IReadOnlyList<ClientDietProgramDayDto>> GetMyDietProgramsAsync(string clientId);

    /// <summary>Seçilen günün güncel programı (DietPrograms; tek gün, en son güncellenen sürüm).</summary>
    Task<ClientDietProgramDayDto?> GetMyDietProgramForDateAsync(string clientId, string programDate);

    /// <summary>Danışan tarafında bir öğünü kalıcı olarak tamamlandı işaretler.</summary>
    Task SetMyMealCompletedAsync(string clientId, string programDate, string meal);
}
