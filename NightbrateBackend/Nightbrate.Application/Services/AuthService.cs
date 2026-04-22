using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Utils;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Services;

public class AuthService(
    IUserRepository userRepository,
    IClientRepository clientRepository,
    IDietitianRepository dietitianRepository,
    IJwtTokenService jwtTokenService) : IAuthService
{
    public async Task RegisterClientAsync(ClientRegisterDto dto)
    {
        var email = dto.Email.Trim().ToLowerInvariant();
        var existing = await userRepository.GetByEmailAsync(email);
        if (existing is not null) throw new AppException("Bu e-posta zaten kayıtlı.");

        PasswordHasher.CreatePasswordHash(dto.Password, out var hash, out var salt);
        var client = new Client
        {
            Email = email,
            PasswordHash = hash,
            PasswordSalt = salt,
            Role = UserRole.Client,
            FirstName = dto.FirstName,
            LastName = dto.LastName,
            Weight = dto.Weight,
            Height = dto.Height,
            TargetCalories = dto.TargetCalories,
            DietitianId = dto.DietitianId
        };

        await clientRepository.AddAsync(client);
        await userRepository.AddAsync(client);
    }

    public async Task RegisterDietitianAsync(DietitianRegisterDto dto)
    {
        var email = dto.Email.Trim().ToLowerInvariant();
        var existing = await userRepository.GetByEmailAsync(email);
        if (existing is not null) throw new AppException("Bu e-posta zaten kayıtlı.");

        PasswordHasher.CreatePasswordHash(dto.Password, out var hash, out var salt);
        var dietitian = new Dietitian
        {
            Email = email,
            PasswordHash = hash,
            PasswordSalt = salt,
            Role = UserRole.Dietitian,
            FirstName = dto.FirstName,
            LastName = dto.LastName,
            DiplomaNo = dto.DiplomaNo,
            ClinicName = dto.ClinicName,
            IsApproved = false
        };

        await dietitianRepository.AddAsync(dietitian);
        await userRepository.AddAsync(dietitian);
    }

    public async Task<(string Token, string Role)> LoginAsync(LoginDto dto)
    {
        var email = dto.Email.Trim().ToLowerInvariant();
        var user = await userRepository.GetByEmailAsync(email);
        if (user is null) throw new AppException("E-posta veya şifre hatalı.");

        var verified = PasswordHasher.VerifyPasswordHash(dto.Password, user.PasswordHash, user.PasswordSalt);
        if (!verified) throw new AppException("E-posta veya şifre hatalı.");

        if (user is Dietitian dietitian && !dietitian.IsApproved)
        {
            throw new AppException("Diyetisyen hesabı henüz onaylanmadı.");
        }

        return (jwtTokenService.CreateToken(user), user.Role.ToString());
    }
}
