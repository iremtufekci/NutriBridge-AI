namespace Nightbrate.Application.DTOs;

public class UserManagementStatsDto
{
    public int TotalUsers { get; set; }
    public int Admins { get; set; }
    public int Dietitians { get; set; }
    public int Clients { get; set; }
    public int Active { get; set; }
    public int Pending { get; set; }
}

public class AdminUserRowDto
{
    public string Id { get; set; } = string.Empty;
    public string DisplayName { get; set; } = string.Empty;
    public string Initial { get; set; } = "A";
    public string Email { get; set; } = string.Empty;
    public string Phone { get; set; } = string.Empty;
    public string Role { get; set; } = string.Empty;
    public string RoleKey { get; set; } = string.Empty;
    public string StatusKey { get; set; } = string.Empty;
    public string StatusLabel { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public string? LastActivityAt { get; set; }
    public bool IsSuspended { get; set; }
}

public class SetUserSuspensionRequestDto
{
    public string Message { get; set; } = string.Empty;
}
