using MongoDB.Bson; // MongoDB BSON tipleri
using MongoDB.Bson.Serialization.Attributes; // Serileştirme öznitelikleri

namespace Nightbrate.Core.Entities; // Varlık sınıfları ad alanı

/// <summary>Diyetisyenin bir kritik uyarıyı "onayladı / inceledi" kaydı; aynı gün+tür tekrar listelenmez.</summary>
public class CriticalAlertAcknowledgment // Kritik uyarı onay kaydı
{
    [BsonId] // MongoDB birincil anahtar
    [BsonRepresentation(BsonType.ObjectId)] // ObjectId string temsili
    public string? Id { get; set; } // Onay kaydı kimliği

    public string DietitianId { get; set; } = string.Empty; // Uyarıyı onaylayan diyetisyen
    public string ClientId { get; set; } = string.Empty; // İlgili danışan
    /// <summary>MissedMeals | HighCalories | LowWater</summary>
    public string AlertType { get; set; } = string.Empty; // Uyarı türü
    /// <summary>yyyy-MM-dd (UTC takvimi)</summary>
    public string ReferenceDate { get; set; } = string.Empty; // Uyarının referans günü
    public DateTime AcknowledgedAtUtc { get; set; } = DateTime.UtcNow; // Onaylanma zamanı (UTC)
}
