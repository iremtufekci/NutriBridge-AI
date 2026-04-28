using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace Nightbrate.Core.Entities;

public class DietitianDailyTask
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string DietitianId { get; set; } = string.Empty;

    /// <summary>yyyy-MM-dd (UTC takvimi, mevcut kritik uyarı mantığı ile aynı).</summary>
    public string TaskDate { get; set; } = string.Empty;

    /// <summary>Diyetisyen + TaskDate icinde benzersiz anahtar (ornegin sys:crit:...).</summary>
    public string TaskKey { get; set; } = string.Empty;

    public string Title { get; set; } = string.Empty;
    public string Subtitle { get; set; } = string.Empty;

    /// <summary>Critical, MealLog, ProgramReview</summary>
    public string Category { get; set; } = string.Empty;

    public string? ClientId { get; set; }

    public bool IsSystemGenerated { get; set; } = true;

    public bool IsCompleted { get; set; }

    public DateTime? CompletedAtUtc { get; set; }

    public int SortPriority { get; set; }

    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;

    public DateTime UpdatedAtUtc { get; set; } = DateTime.UtcNow;
}
