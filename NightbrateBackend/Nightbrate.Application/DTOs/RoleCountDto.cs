using Nightbrate.Core.Entities;

namespace Nightbrate.Application.DTOs;

public class RoleCountDto
{
    public UserRole Role { get; set; }
    public long Count { get; set; }
}
