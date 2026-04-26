using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Utils;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Services;

public class UserProfileService(IUserRepository userRepository, IDietitianRepository dietitianRepository)
    : IUserProfileService
{
    public async Task<CurrentUserProfileDto> GetByUserIdAsync(string userId)
    {
        if (string.IsNullOrWhiteSpace(userId)) throw new AppException("Gecersiz kullanici.");

        var user = await userRepository.GetByIdAsync(userId);
        if (user is null) throw new AppException("Kullanici bulunamadi.");

        if (user is Client c)
        {
            return new CurrentUserProfileDto
            {
                Email = c.Email,
                Role = c.Role.ToString(),
                FirstName = c.FirstName,
                LastName = c.LastName,
                DisplayName = BuildName(c.FirstName, c.LastName, c.Email),
                ThemePreference = NormalizeTheme(c.ThemePreference)
            };
        }

        if (user.Role == UserRole.Admin)
        {
            var fn = string.Empty;
            var ln = string.Empty;
            if (user is Admin adm)
            {
                fn = (adm.FirstName ?? string.Empty).Trim();
                ln = (adm.LastName ?? string.Empty).Trim();
            }
            if (string.IsNullOrEmpty(fn) && string.IsNullOrEmpty(ln))
            {
                var fromBson = await userRepository.GetAdminNameFromUsersBsonAsync(userId);
                fn = (fromBson.FirstName ?? string.Empty).Trim();
                ln = (fromBson.LastName ?? string.Empty).Trim();
            }
            return new CurrentUserProfileDto
            {
                Email = user.Email,
                Role = user.Role.ToString(),
                FirstName = fn,
                LastName = ln,
                DisplayName = BuildName(fn, ln, user.Email),
                ThemePreference = NormalizeTheme(user.ThemePreference)
            };
        }

        if (user is Dietitian d)
        {
            // Oncelik Dietitians koleksiyonu: JWT id farki veya kopya tutarsizligi varsa e-posta ile dene
            var fromDb = await dietitianRepository.GetByIdAsync(userId)
                ?? await dietitianRepository.GetByEmailAsync(d.Email);
            if (fromDb is not null
                && fromDb.IsApproved
                && string.IsNullOrWhiteSpace(fromDb.ConnectionCode)
                && !string.IsNullOrEmpty(fromDb.Id))
            {
                // Eski onaylarda kod atanmamissa profil yuklenirken bir kez uret
                string newCode;
                do
                {
                    newCode = ConnectionCodeGenerator.NewCode();
                } while (await dietitianRepository.ConnectionCodeExistsAsync(newCode));

                fromDb.ConnectionCode = newCode;
                await dietitianRepository.UpdateAsync(fromDb);
                await userRepository.SetDietitianConnectionCodeInUsersCollectionAsync(fromDb.Id, newCode);
            }

            var code = fromDb?.ConnectionCode;
            if (string.IsNullOrWhiteSpace(code)) code = d.ConnectionCode;
            if (string.IsNullOrWhiteSpace(code) && fromDb is not null && !string.IsNullOrEmpty(fromDb.Id))
            {
                code = await dietitianRepository.GetConnectionCodeByDietitianIdRawAsync(fromDb.Id);
            }
            if (string.IsNullOrWhiteSpace(code))
            {
                code = await dietitianRepository.GetConnectionCodeByDietitianIdRawAsync(userId);
            }
            if (string.IsNullOrWhiteSpace(code))
            {
                code = await userRepository.GetConnectionCodeFromUsersBsonByUserIdAsync(userId);
            }
            return new CurrentUserProfileDto
            {
                Email = d.Email,
                Role = d.Role.ToString(),
                FirstName = fromDb?.FirstName ?? d.FirstName,
                LastName = fromDb?.LastName ?? d.LastName,
                DisplayName = BuildName(
                    fromDb?.FirstName ?? d.FirstName,
                    fromDb?.LastName ?? d.LastName,
                    d.Email),
                ClinicName = fromDb?.ClinicName ?? d.ClinicName,
                DiplomaNo = fromDb?.DiplomaNo ?? d.DiplomaNo,
                ConnectionCode = string.IsNullOrWhiteSpace(code) ? null : code.Trim(),
                ThemePreference = NormalizeTheme(d.ThemePreference)
            };
        }

        return new CurrentUserProfileDto
        {
            Email = user.Email,
            Role = user.Role.ToString(),
            FirstName = string.Empty,
            LastName = string.Empty,
            DisplayName = BuildName(string.Empty, string.Empty, user.Email),
            ThemePreference = NormalizeTheme(user.ThemePreference)
        };
    }

    private static string NormalizeTheme(string? raw) =>
        string.Equals(raw, "dark", StringComparison.OrdinalIgnoreCase) ? "dark" : "light";

    private static string BuildName(string first, string last, string email)
    {
        var n = $"{first} {last}".Trim();
        if (n.Length > 0) return n;
        var at = email.IndexOf('@');
        return at > 0 ? email[..at] : email;
    }
}
