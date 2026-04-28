using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace Nightbrate.Core.Entities;

/// <summary>Diyetisyenin bir kritik uyarıyı "onayladı / inceledi" kaydı; aynı gün+tür tekrar listelenmez.</summary>
public class CriticalAlertAcknowledgment
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string DietitianId { get; set; } = string.Empty;
    public string ClientId { get; set; } = string.Empty;
    /// <summary>MissedMeals | HighCalories | LowWater</summary>
    public string AlertType { get; set; } = string.Empty;
    /// <summary>yyyy-MM-dd (UTC takvimi)</summary>
    public string ReferenceDate { get; set; } = string.Empty;
    public DateTime AcknowledgedAtUtc { get; set; } = DateTime.UtcNow;
}
