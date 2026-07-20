using Microsoft.Extensions.Options;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Options;
using UglyToad.PdfPig;

namespace Nightbrate.API.Services;

/// <summary>PDF metnini PdfPig ile çıkarır, Groq metin modeli ile Türkçe analiz üretir.</summary>
public sealed class GroqPdfAnalysisService(HttpClient http, IOptions<GroqAiOptions> options) : IPdfAnalysisAiService
{
    private readonly GroqAiOptions _opt = options.Value;
    private const int MaxPdfTextChars = 28_000;

    public async Task<ClientPdfAnalysisResultDto> AnalyzePdfAsync(
        byte[] pdfBytes,
        string originalFileName,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(_opt.ApiKey))
            throw new AppException("Groq API anahtarı yapılandırılmamış.");
        if (pdfBytes.Length == 0)
            throw new AppException("PDF içeriği boş.");

        var extracted = ExtractPdfText(pdfBytes);
        if (string.IsNullOrWhiteSpace(extracted))
            throw new AppException("PDF'den metin okunamadı. Taranmış (görüntü) PDF'ler desteklenmeyebilir.");

        var model = string.IsNullOrWhiteSpace(_opt.TextModel) ? "llama-3.3-70b-versatile" : _opt.TextModel.Trim();
        var nameHint = string.IsNullOrWhiteSpace(originalFileName) ? "" : $" Dosya: {originalFileName}.";

        var prompt =
            "Asagidaki laboratuvar / saglik belgesi metnini diyetisyen perspektifinden analiz et." + nameHint + " "
            + "Tani koyma, ilac onerme. Sadece metindeki degerlere dayan. Turkce yaz. "
            + "Yalnizca JSON don: {\"documentType\":\"...\",\"summary\":\"...\",\"keyFindings\":[],"
            + "\"cautions\":[],\"suggestedForDietitian\":[\"...\"]}\n\nBELGE METNI:\n" + extracted;

        var json = await GroqChatClient.CompleteTextJsonAsync(http, _opt.ApiKey, model, prompt, 0.25, cancellationToken).ConfigureAwait(false);
        var root = GroqChatClient.DeserializeJson<PdfRootJson>(json);
        var comments = root.SuggestedForDietitian?.Where(s => !string.IsNullOrWhiteSpace(s)).Select(s => s.Trim()).Take(24).ToList() ?? [];

        if (string.IsNullOrWhiteSpace(root.Summary) && comments.Count == 0)
            throw new AppException("Geçerli analiz metni alınamadı.");

        return new ClientPdfAnalysisResultDto
        {
            AnalysisSource = "groq",
            DocumentType = (root.DocumentType ?? "Belge").Trim(),
            Summary = string.IsNullOrWhiteSpace(root.Summary)
                ? comments.FirstOrDefault() ?? "Laboratuvar / rapor belgesi"
                : root.Summary.Trim(),
            KeyFindings = [],
            Cautions = [],
            SuggestedForDietitian = comments
        };
    }

    private static string ExtractPdfText(byte[] pdfBytes)
    {
        using var doc = PdfDocument.Open(pdfBytes);
        var sb = new System.Text.StringBuilder();
        foreach (var page in doc.GetPages())
        {
            sb.AppendLine(page.Text);
            if (sb.Length >= MaxPdfTextChars) break;
        }
        var text = sb.ToString().Trim();
        return text.Length > MaxPdfTextChars ? text[..MaxPdfTextChars] : text;
    }

    private sealed class PdfRootJson
    {
        public string? DocumentType { get; set; }
        public string? Summary { get; set; }
        public List<string>? KeyFindings { get; set; }
        public List<string>? Cautions { get; set; }
        public List<string>? SuggestedForDietitian { get; set; }
    }
}
