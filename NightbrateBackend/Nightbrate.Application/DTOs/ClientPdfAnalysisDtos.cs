namespace Nightbrate.Application.DTOs;

/// <summary>PDF depolama katmanının döndürdüğü fiziksel ve public URL bilgisi.</summary>
public sealed class PdfDocumentSaveResult
{
    public string FullPath { get; set; } = string.Empty;
    public string RelativePublicUrl { get; set; } = string.Empty;
}

/// <summary>Yalnızca yapay zeka katmanından gelen ham analiz (henüz DB'ye yazılmadan).</summary>
public sealed class ClientPdfAnalysisResultDto
{
    /// <summary>gemini | mock</summary>
    public string AnalysisSource { get; set; } = "gemini";

    public string DocumentType { get; set; } = string.Empty;
    public string Summary { get; set; } = string.Empty;
    public List<string> KeyFindings { get; set; } = new();
    public List<string> Cautions { get; set; } = new();
    public List<string> SuggestedForDietitian { get; set; } = new();
}

/// <summary>POST upload cevabı: kayıt id + PDF linki + tam analiz metni.</summary>
public sealed class ClientPdfAnalysisUploadResponseDto
{
    public string Id { get; set; } = string.Empty;
    public string PdfUrl { get; set; } = string.Empty;
    public string OriginalFileName { get; set; } = string.Empty;
    public string DocumentType { get; set; } = string.Empty;
    public string Summary { get; set; } = string.Empty;
    public List<string> KeyFindings { get; set; } = new();
    public List<string> Cautions { get; set; } = new();
    public List<string> SuggestedForDietitian { get; set; } = new();
    public string AnalysisSource { get; set; } = string.Empty;
    public DateTime CreatedAtUtc { get; set; }
}

/// <summary>GET geçmiş listesi: her satırda tam analiz alanları (web/mobil detay için).</summary>
public sealed class ClientPdfAnalysisListItemDto
{
    public string Id { get; set; } = string.Empty;
    public string PdfUrl { get; set; } = string.Empty;
    public string OriginalFileName { get; set; } = string.Empty;
    public string DocumentType { get; set; } = string.Empty;
    public string Summary { get; set; } = string.Empty;
    public List<string> KeyFindings { get; set; } = new();
    public List<string> Cautions { get; set; } = new();
    public List<string> SuggestedForDietitian { get; set; } = new();
    public string AnalysisSource { get; set; } = string.Empty;
    public DateTime CreatedAtUtc { get; set; }
}
