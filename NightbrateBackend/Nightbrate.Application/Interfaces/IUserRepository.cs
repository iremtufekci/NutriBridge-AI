using Nightbrate.Application.DTOs;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IUserRepository
{
    Task<BaseUser?> GetByEmailAsync(string email);
    Task<BaseUser?> GetByIdAsync(string id);
    Task AddAsync(BaseUser user);
    Task<long> GetTotalUsersAsync();
    Task<long> CountByRoleAsync(UserRole role);
    Task<IReadOnlyList<MonthlyRegistrationDto>> GetMonthlyUserRegistrationsAsync(int monthsBack);

    Task SetDietitianIsApprovedInUsersCollectionAsync(string dietitianId, bool isApproved);
    Task SetDietitianConnectionCodeInUsersCollectionAsync(string dietitianId, string connectionCode);
    Task SetClientDietitianIdInUsersCollectionAsync(string clientId, string dietitianId);
    /// <summary>Users Bson'unda ConnectionCode (takip kodu) — tipli Dietitian'da bosa dustugunda.</summary>
    Task<string?> GetConnectionCodeFromUsersBsonByUserIdAsync(string userId);

    /// <summary>Users + ilgili rol koleksiyonlarında (Clients/Dietitians) tema alanini gunceller.</summary>
    Task UpdateThemePreferenceAllStoresAsync(string userId, string themePreference);

    Task<List<BaseUser>> GetAllUsersForAdminAsync();
    Task SetUserSuspensionAllStoresAsync(string userId, bool isSuspended, string? message, DateTime? suspendedAt);

    /// <summary>Users koleksiyonundaki Client belgesinde ad, soyad, boy, kilo, hedef kaloriyi gunceller.</summary>
    Task UpdateClientProfileInUsersCollectionAsync(
        string clientId,
        string firstName,
        string lastName,
        double weight,
        double height,
        int targetCalories);

    /// <summary>Users Bson'unda ad/soyad (camelCase veya PascalCase) — tipli deser. kaçırırsa.</summary>
    Task<(string? FirstName, string? LastName)> GetAdminNameFromUsersBsonAsync(string userId);
}
