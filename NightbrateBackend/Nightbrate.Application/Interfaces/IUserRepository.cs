using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IUserRepository
{
    Task<BaseUser?> GetByEmailAsync(string email);
    Task<BaseUser?> GetByIdAsync(string id);
    Task AddAsync(BaseUser user);
    Task<long> GetTotalUsersAsync();

    Task SetDietitianIsApprovedInUsersCollectionAsync(string dietitianId, bool isApproved);
}
