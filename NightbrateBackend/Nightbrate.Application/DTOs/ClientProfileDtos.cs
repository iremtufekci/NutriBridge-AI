namespace Nightbrate.Application.DTOs;

public class ClientProfileDto
{
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public double Weight { get; set; }
    public double Height { get; set; }
    public int TargetCalories { get; set; }
    public string GoalText { get; set; } = string.Empty;
    public string ThemePreference { get; set; } = "light";
    public string DietitianName { get; set; } = "Atanmadi";
    public DateTime ProgramStartDate { get; set; }
}

public class UpdateThemePreferenceDto
{
    public string ThemePreference { get; set; } = "light";
}
