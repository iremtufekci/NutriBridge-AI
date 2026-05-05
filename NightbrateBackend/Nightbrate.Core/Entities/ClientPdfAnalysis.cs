using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace Nightbrate.Core.Entities;

public class ClientPdfAnalysis
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string ClientId { get; set; } = string.Empty;

    public string OriginalFileName { get; set; } = string.Empty;

    /// <summary>Ornegin /uploads/pdfs/xxx.pdf</summary>
    public string PdfRelativeUrl { get; set; } = string.Empty;

    public string DocumentType { get; set; } = string.Empty;

    public string Summary { get; set; } = string.Empty;

    public List<string> KeyFindings { get; set; } = new();

    public List<string> Cautions { get; set; } = new();

    public List<string> SuggestedForDietitian { get; set; } = new();

    /// <summary>gemini | mock</summary>
    public string AnalysisSource { get; set; } = string.Empty;

    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;
}
