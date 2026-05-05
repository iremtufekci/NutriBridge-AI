namespace Nightbrate.Application.DTOs;

public sealed class PdfDocumentSaveResult
{
    public string FullPath { get; set; } = string.Empty;
    public string RelativePublicUrl { get; set; } = string.Empty;
}

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
