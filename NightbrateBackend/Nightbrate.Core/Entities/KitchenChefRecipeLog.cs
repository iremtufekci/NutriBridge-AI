using MongoDB.Bson; // MongoDB BSON tipleri
using MongoDB.Bson.Serialization.Attributes; // Serileştirme öznitelikleri

namespace Nightbrate.Core.Entities; // Varlık sınıfları ad alanı

/// <summary>Danışanın AI Mutfak'ta seçip paylaştığı tarifler (diyetisyen panelinde listelenir).</summary>
public class KitchenChefRecipeLog // AI mutfak tarif paylaşım kaydı
{
    [BsonId] // MongoDB birincil anahtar
    [BsonRepresentation(BsonType.ObjectId)] // ObjectId string temsili
    public string? Id { get; set; } // Kayıt kimliği

    public string ClientId { get; set; } = string.Empty; // Tarifi paylaşan danışan
    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow; // Oluşturulma zamanı (UTC)
    public string Ingredients { get; set; } = string.Empty; // Kullanıcının girdiği malzemeler (ham metin)
    public string Preference { get; set; } = string.Empty; // Tercih / kısıtlama bilgisi
    public int TargetCalories { get; set; } // Hedef kalori
    /// <summary>gemini | mock</summary>
    public string Source { get; set; } = "mock"; // Tarif kaynağı (AI veya mock)
    public List<KitchenChefRecipeSnapshot> SelectedRecipes { get; set; } = new(); // Seçilen tarif anlık görüntüleri
}

public class KitchenChefRecipeSnapshot // Tek bir tarifin kayıt anındaki kopyası
{
    public string Title { get; set; } = string.Empty; // Tarif adı
    public string? Description { get; set; } // Tarif açıklaması
    public int EstimatedCalories { get; set; } // Tahmini kalori
    public int? PrepTimeMinutes { get; set; } // Hazırlık süresi (dakika)
    public List<string> Ingredients { get; set; } = new(); // Malzeme listesi
    public List<string> Steps { get; set; } = new(); // Yapılış adımları
}
