using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface IUserProfileService
{
    Task<CurrentUserProfileDto> GetByUserIdAsync(string userId);
}
