using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface IClientService
{
    Task LogMealAsync(string clientId, LogMealDto dto);
    Task AddWaterAsync(string clientId, AddWaterDto dto);
    Task AddWeightAsync(string clientId, AddWeightDto dto);
    Task<ClientProfileDto> GetProfileAsync(string clientId);
    Task UpdateThemePreferenceAsync(string clientId, string themePreference);
    Task<ConnectToDietitianResultDto> ConnectToDietitianAsync(string clientId, ConnectToDietitianRequestDto dto);
    Task<PreviewDietitianByCodeResultDto> PreviewDietitianByCodeAsync(ConnectToDietitianRequestDto dto);
}
