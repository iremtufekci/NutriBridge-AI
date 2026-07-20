using MongoDB.Bson; // MongoDB BSON tipleri
using MongoDB.Bson.Serialization.Attributes; // Serileştirme öznitelikleri

namespace Nightbrate.Core.Entities; // Varlık sınıfları ad alanı

public class DietProgram // Günlük diyet programı varlığı
{
    [BsonId] // MongoDB birincil anahtar
    [BsonRepresentation(BsonType.ObjectId)] // ObjectId string temsili
    public string? Id { get; set; } // Program kaydı kimliği

    public string DietitianId { get; set; } = string.Empty; // Programı oluşturan diyetisyen
    public string ClientId { get; set; } = string.Empty; // Programın atandığı danışan

    /// <summary>Atama tarihi, yyyy-MM-dd (ör. diyetisyenin seçtiği gün, UTC günü ile hizalanmış).</summary>
    public string ProgramDate { get; set; } = string.Empty; // Program günü (yyyy-MM-dd)

    public string Breakfast { get; set; } = string.Empty; // Kahvaltı içeriği
    public string Lunch { get; set; } = string.Empty; // Öğle yemeği içeriği
    public string Dinner { get; set; } = string.Empty; // Akşam yemeği içeriği
    public string Snack { get; set; } = string.Empty; // Ara öğün içeriği

    public int BreakfastCalories { get; set; } // Kahvaltı kalorisi
    public int LunchCalories { get; set; } // Öğle kalorisi
    public int DinnerCalories { get; set; } // Akşam kalorisi
    public int SnackCalories { get; set; } // Ara öğün kalorisi

    /// <summary>Öğün kalorileri toplamı (sunucu tarafında senkron tutulur).</summary>
    public int TotalCalories { get; set; } // Toplam günlük kalori

    public bool BreakfastCompleted { get; set; } // Kahvaltı tamamlandı mı
    public bool LunchCompleted { get; set; } // Öğle tamamlandı mı
    public bool DinnerCompleted { get; set; } // Akşam tamamlandı mı
    public bool SnackCompleted { get; set; } // Ara öğün tamamlandı mı

    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow; // Son güncelleme zamanı (UTC)
}
