namespace Nightbrate.Application.DTOs;

public class UserRegisterDto
{
    public string Username { get; set; } = null!;
    public string Email { get; set; } = null!;
    public string Password { get; set; } = null!;
    public int Role { get; set; } // 0: Admin, 1: Dietitian, 2: User
}