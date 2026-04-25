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
}
