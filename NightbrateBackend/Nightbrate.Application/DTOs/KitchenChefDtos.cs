namespace Nightbrate.Application.DTOs;

public class KitchenChefRequestDto
{
    public string Ingredients { get; set; } = string.Empty;
    public string Preference { get; set; } = string.Empty;
    public int TargetCalories { get; set; }
}

public class KitchenChefResponseDto
{
    public List<KitchenChefRecipeDto> Recipes { get; set; } = new();
    /// <summary>gemini | mock</summary>
    public string Source { get; set; } = "mock";
}

public class KitchenChefRecipeDto
{
    public string Title { get; set; } = string.Empty;
    public string? Description { get; set; }
    public int EstimatedCalories { get; set; }
    public int? PrepTimeMinutes { get; set; }
    public List<string> Ingredients { get; set; } = new();
    public List<string> Steps { get; set; } = new();
}

/// <summary>Danışanın diyetisyenle paylaşmak üzere kaydettiği tarif listesi.</summary>
public class KitchenChefSaveRequestDto
{
    public string Ingredients { get; set; } = string.Empty;
    public string Preference { get; set; } = string.Empty;
    public int TargetCalories { get; set; }
    public string Source { get; set; } = "mock";
    public List<KitchenChefRecipeDto> SelectedRecipes { get; set; } = new();
}

public class KitchenChefRecipeLogItemDto
{
    public string? Id { get; set; }
    public DateTime CreatedAtUtc { get; set; }
    public string Ingredients { get; set; } = string.Empty;
    public string Preference { get; set; } = string.Empty;
    public int TargetCalories { get; set; }
    public string Source { get; set; } = "mock";
    public List<KitchenChefRecipeDto> SelectedRecipes { get; set; } = new();
}
