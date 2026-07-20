using MongoDB.Bson; // MongoDB BSON tipleri
using MongoDB.Bson.Serialization.Attributes; // Serileştirme öznitelikleri

namespace Nightbrate.Core.Entities; // Varlık sınıfları ad alanı

/// <summary>MongoDB'de saklanan danışan PDF analiz kaydı.</summary>
public class ClientPdfAnalysis // Danışan PDF analiz sonucu varlığı
{
    [BsonId] // MongoDB birincil anahtar
    [BsonRepresentation(BsonType.ObjectId)] // ObjectId string temsili
    public string? Id { get; set; } // Analiz kaydı kimliği

    public string ClientId { get; set; } = string.Empty; // PDF yükleyen danışan

    public string OriginalFileName { get; set; } = string.Empty; // Orijinal dosya adı

    /// <summary>Ornegin /uploads/pdfs/xxx.pdf</summary>
    public string PdfRelativeUrl { get; set; } = string.Empty; // PDF'in sunucudaki göreli URL'si

    public string DocumentType { get; set; } = string.Empty; // Belge türü (kan tahlili vb.)

    public string Summary { get; set; } = string.Empty; // AI özet metni

    public List<string> KeyFindings { get; set; } = new(); // Önemli bulgular listesi

    public List<string> Cautions { get; set; } = new(); // Dikkat / uyarı maddeleri

    public List<string> SuggestedForDietitian { get; set; } = new(); // Diyetisyene öneriler

    /// <summary>gemini | mock</summary>
    public string AnalysisSource { get; set; } = string.Empty; // Analiz kaynağı (AI veya mock)

    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow; // Analiz oluşturulma zamanı (UTC)
}
