using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface IAuthService
{
    Task RegisterClientAsync(ClientRegisterDto dto);
    Task RegisterDietitianAsync(DietitianRegisterDto dto);
    Task<(string Token, string Role)> LoginAsync(LoginDto dto);
}
