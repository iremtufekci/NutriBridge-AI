namespace Nightbrate.Application.DTOs;

public class SaveDietProgramDto
{
    public string ClientId { get; set; } = string.Empty;
    public string DayOfWeek { get; set; } = string.Empty;
    public string Breakfast { get; set; } = string.Empty;
    public string Lunch { get; set; } = string.Empty;
    public string Dinner { get; set; } = string.Empty;
    public string Snack { get; set; } = string.Empty;
    public int TotalCalories { get; set; }
}
