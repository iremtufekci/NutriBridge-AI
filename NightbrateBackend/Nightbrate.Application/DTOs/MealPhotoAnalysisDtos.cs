namespace Nightbrate.Application.DTOs;

public class MealAnalysisResultDto
{
    public IReadOnlyList<string> Foods { get; init; } = Array.Empty<string>();
    public int EstimatedCalories { get; init; }
    public double Protein { get; init; }
    public double Carb { get; init; }
    public double Fat { get; init; }
}

public class MealPhotoAnalysisResponseDto
{
    public string? MealLogId { get; set; }
    public string PhotoUrl { get; set; } = string.Empty;
    public int EstimatedCalories { get; set; }
    public List<string> DetectedFoods { get; set; } = new();
    public DateTime TimestampUtc { get; set; }

    /// <summary>gemini = Google Gemini API, mock = yerel simulasyon (ApiKey yok).</summary>
    public string AnalysisSource { get; set; } = "mock";
}

public class MealPhotoSaveResult
{
    public string FullPath { get; init; } = string.Empty;
    public string RelativePublicUrl { get; init; } = string.Empty;
}
