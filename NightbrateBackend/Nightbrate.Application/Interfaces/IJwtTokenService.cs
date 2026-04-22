using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IJwtTokenService
{
    string CreateToken(BaseUser user);
}
