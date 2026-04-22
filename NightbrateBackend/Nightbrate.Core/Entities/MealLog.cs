using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace Nightbrate.Core.Entities;

public class MealLog
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string ClientId { get; set; } = string.Empty;
    public string PhotoUrl { get; set; } = string.Empty;
    public int Calories { get; set; }
    public MacroInfo Macros { get; set; } = new();
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;
}

public class MacroInfo
{
    public double Protein { get; set; }
    public double Carb { get; set; }
    public double Fat { get; set; }
}
