using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace Nightbrate.Core.Entities;

public class DietProgram
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string DietitianId { get; set; } = string.Empty;
    public string ClientId { get; set; } = string.Empty;
    public string DayOfWeek { get; set; } = string.Empty;

    public string Breakfast { get; set; } = string.Empty;
    public string Lunch { get; set; } = string.Empty;
    public string Dinner { get; set; } = string.Empty;
    public string Snack { get; set; } = string.Empty;

    public int TotalCalories { get; set; }
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}
