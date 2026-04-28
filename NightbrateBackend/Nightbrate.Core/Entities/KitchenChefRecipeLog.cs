using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace Nightbrate.Core.Entities;

/// <summary>Danışanın AI Mutfak’ta seçip paylaştığı tarifler (diyetisyen panelinde listelenir).</summary>
public class KitchenChefRecipeLog
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string ClientId { get; set; } = string.Empty;
    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;
    public string Ingredients { get; set; } = string.Empty;
    public string Preference { get; set; } = string.Empty;
    public int TargetCalories { get; set; }
    /// <summary>gemini | mock</summary>
    public string Source { get; set; } = "mock";
    public List<KitchenChefRecipeSnapshot> SelectedRecipes { get; set; } = new();
}

public class KitchenChefRecipeSnapshot
{
    public string Title { get; set; } = string.Empty;
    public string? Description { get; set; }
    public int EstimatedCalories { get; set; }
    public int? PrepTimeMinutes { get; set; }
    public List<string> Ingredients { get; set; } = new();
    public List<string> Steps { get; set; } = new();
}
