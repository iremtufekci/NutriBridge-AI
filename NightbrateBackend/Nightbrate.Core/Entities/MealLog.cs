using MongoDB.Bson; // MongoDB BSON tipleri
using MongoDB.Bson.Serialization.Attributes; // Serileştirme öznitelikleri

namespace Nightbrate.Core.Entities; // Varlık sınıfları ad alanı

public class MealLog // Danışan öğün kaydı varlığı
{
    [BsonId] // MongoDB birincil anahtar
    [BsonRepresentation(BsonType.ObjectId)] // ObjectId string temsili
    public string? Id { get; set; } // Kayıt kimliği

    public string ClientId { get; set; } = string.Empty; // Öğünü kaydeden danışan id
    public string PhotoUrl { get; set; } = string.Empty; // Yemek fotoğrafı URL'si
    public int Calories { get; set; } // Tahmini kalori
    /// <summary>AI veya manuel analizde tespit edilen besin adlari.</summary>
    public List<string> DetectedFoods { get; set; } = new(); // Tespit edilen yiyecekler
    public MacroInfo Macros { get; set; } = new(); // Makro besin değerleri
    public DateTime Timestamp { get; set; } = DateTime.UtcNow; // Kayıt zamanı (UTC)
}

public class MacroInfo // Makro besin bilgisi
{
    public double Protein { get; set; } // Protein (g)
    public double Carb { get; set; } // Karbonhidrat (g)
    public double Fat { get; set; } // Yağ (g)
}
