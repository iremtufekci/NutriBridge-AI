using MongoDB.Bson; // MongoDB BSON tipleri
using MongoDB.Bson.Serialization.Attributes; // Serileştirme öznitelikleri

namespace Nightbrate.Core.Entities; // Varlık sınıfları ad alanı

public class ActivityLog // Sistem aktivite kaydı varlığı
{
    [BsonId] // MongoDB birincil anahtar
    [BsonRepresentation(BsonType.ObjectId)] // ObjectId string temsili
    public string? Id { get; set; } // Kayıt kimliği

    /// <summary>Kullanici (Users) kimligi; yoksa sistem mesaji</summary>
    public string? UserId { get; set; } // İşlemi yapan kullanıcı (yoksa null)

    public string ActorDisplayName { get; set; } = string.Empty; // Görünen aktör adı
    public string Description { get; set; } = string.Empty; // Aktivite açıklaması
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow; // Oluşturulma zamanı (UTC)
}
