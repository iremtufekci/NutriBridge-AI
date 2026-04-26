using System.Text.Json.Serialization;

namespace Nightbrate.Application.DTOs;

public class CurrentUserProfileDto
{
    public string Email { get; set; } = string.Empty;
    public string Role { get; set; } = string.Empty;
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public string DisplayName { get; set; } = string.Empty;
    public string? ClinicName { get; set; }
    public string? DiplomaNo { get; set; }

    [JsonPropertyName("connectionCode")]
    public string? ConnectionCode { get; set; }

    public string ThemePreference { get; set; } = "light";
}
