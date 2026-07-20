using MongoDB.Bson; // MongoDB BSON tipleri
using MongoDB.Bson.Serialization.Attributes; // Serileştirme öznitelikleri

namespace Nightbrate.Core.Entities; // Varlık sınıfları ad alanı

public class DietitianDailyTask // Diyetisyen günlük görev kaydı
{
    [BsonId] // MongoDB birincil anahtar
    [BsonRepresentation(BsonType.ObjectId)] // ObjectId string temsili
    public string? Id { get; set; } // Görev kimliği

    public string DietitianId { get; set; } = string.Empty; // Görevin sahibi diyetisyen

    /// <summary>yyyy-MM-dd (UTC takvimi, mevcut kritik uyarı mantığı ile aynı).</summary>
    public string TaskDate { get; set; } = string.Empty; // Görev günü (yyyy-MM-dd)

    /// <summary>Diyetisyen + TaskDate icinde benzersiz anahtar (ornegin sys:crit:...).</summary>
    public string TaskKey { get; set; } = string.Empty; // Benzersiz görev anahtarı

    public string Title { get; set; } = string.Empty; // Görev başlığı
    public string Subtitle { get; set; } = string.Empty; // Görev alt başlığı

    /// <summary>Critical, MealLog, ProgramReview</summary>
    public string Category { get; set; } = string.Empty; // Görev kategorisi

    public string? ClientId { get; set; } // İlgili danışan (varsa)

    public bool IsSystemGenerated { get; set; } = true; // Sistem tarafından mı oluşturuldu

    public bool IsCompleted { get; set; } // Görev tamamlandı mı

    public DateTime? CompletedAtUtc { get; set; } // Tamamlanma zamanı (UTC, varsa)

    public int SortPriority { get; set; } // Listeleme önceliği (küçük = üstte)

    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow; // Oluşturulma zamanı (UTC)

    public DateTime UpdatedAtUtc { get; set; } = DateTime.UtcNow; // Son güncelleme zamanı (UTC)
}
