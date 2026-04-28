namespace Nightbrate.Application.Options;

public class MealUploadOptions
{
    public string MealsDirectory { get; set; } = string.Empty;
    public string PublicRelativePath { get; set; } = "/uploads/meals";
}
