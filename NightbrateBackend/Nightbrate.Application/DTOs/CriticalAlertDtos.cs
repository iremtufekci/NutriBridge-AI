namespace Nightbrate.Application.DTOs;

public static class CriticalAlertTypes
{
    public const string MissedMeals = "MissedMeals";
    public const string HighCalories = "HighCalories";
    /// <summary>Artık kritik uyarıda kullanılmıyor; eski kayıtlar için tutulur.</summary>
    public const string LowWater = "LowWater";
}

public class CriticalAlertDto
{
    public string Id { get; set; } = string.Empty;
    public string ClientId { get; set; } = string.Empty;
    public string ClientName { get; set; } = string.Empty;
    public string AlertType { get; set; } = string.Empty;
    /// <summary>High | Medium</summary>
    public string Severity { get; set; } = string.Empty;
    public string Message { get; set; } = string.Empty;
    public DateTime Date { get; set; }
    /// <summary>yyyy-MM-dd (UTC günü)</summary>
    public string ReferenceDate { get; set; } = string.Empty;
}

public class AckCriticalAlertDto
{
    public string ClientId { get; set; } = string.Empty;
    public string AlertType { get; set; } = string.Empty;
    public string ReferenceDate { get; set; } = string.Empty;
}

public class ClientBriefDto
{
    public string ClientId { get; set; } = string.Empty;
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public int TargetCalories { get; set; }
    public double Weight { get; set; }
    public double Height { get; set; }
    public string Phone { get; set; } = string.Empty;
}
