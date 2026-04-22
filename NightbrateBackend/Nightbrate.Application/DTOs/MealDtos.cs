namespace Nightbrate.Application.DTOs;

public class LogMealDto
{
    public string PhotoUrl { get; set; } = string.Empty;
    public int Calories { get; set; }
    public double Protein { get; set; }
    public double Carb { get; set; }
    public double Fat { get; set; }
}

public class AddWaterDto
{
    public int Ml { get; set; }
}

public class AddWeightDto
{
    public double Weight { get; set; }
}
