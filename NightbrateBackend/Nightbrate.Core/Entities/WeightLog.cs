using MongoDB.Bson; // MongoDB BSON tipleri
using MongoDB.Bson.Serialization.Attributes; // Serileştirme öznitelikleri

namespace Nightbrate.Core.Entities; // Varlık sınıfları ad alanı

public class WeightLog // Kilo takip kaydı varlığı
{
    [BsonId] // MongoDB birincil anahtar
    [BsonRepresentation(BsonType.ObjectId)] // ObjectId string temsili
    public string? Id { get; set; } // Kayıt kimliği

    public string ClientId { get; set; } = string.Empty; // Danışan kimliği
    public double Weight { get; set; } // Ölçülen kilo (kg)
    public DateTime Timestamp { get; set; } = DateTime.UtcNow; // Ölçüm zamanı (UTC)
}
