using Microsoft.Extensions.Options;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Options;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Services;

/// <summary>
/// PDF akışı: dosyayı diske/Cloudinary'e yazar → yapay zeka analizi → MongoDB'ye kaydeder.
/// Web ve mobil aynı servisi kullanır.
/// </summary>
public class ClientPdfAnalysisService(
    IClientPdfAnalysisRepository repository,
    IPdfDocumentStorage pdfStorage,
    IPdfAnalysisAiService pdfAnalysisAi,
    IOptions<PdfUploadOptions> pdfOptions) : IClientPdfAnalysisService
{
    public async Task<ClientPdfAnalysisUploadResponseDto> UploadAnalyzeAndPersistAsync(
        string clientId,
        Stream pdfStream,
        string originalFileName,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(clientId)) throw new AppException("Oturum bulunamadi.");

        // Belleğe al: hem depolama hem Gemini aynı bayt dizisini kullanır
        var maxBytes = Math.Max(256 * 1024, pdfOptions.Value.MaxPdfBytes);
        await using var ms = new MemoryStream();
        await pdfStream.CopyToAsync(ms, cancellationToken).ConfigureAwait(false);
        var bytes = ms.ToArray();
        if (bytes.Length == 0) throw new AppException("PDF dosyasi bos.");
        if (bytes.Length > maxBytes)
            throw new AppException($"PDF boyutu en fazla {maxBytes / (1024 * 1024)} MB olabilir.");

        var safeName = string.IsNullOrWhiteSpace(originalFileName) ? "belge.pdf" : Path.GetFileName(originalFileName);
        if (!safeName.EndsWith(".pdf", StringComparison.OrdinalIgnoreCase))
            safeName += ".pdf";

        // 1) PDF dosyasını kaydet (yerel wwwroot veya Cloudinary)
        await using var uploadStream = new MemoryStream(bytes, writable: false);
        var saved = await pdfStorage.SavePdfAsync(uploadStream, cancellationToken).ConfigureAwait(false);

        // 2) Gemini (veya mock) ile Türkçe özet / bulgular üret
        var ai = await pdfAnalysisAi.AnalyzePdfAsync(bytes, safeName, cancellationToken).ConfigureAwait(false);

        var source = ai.AnalysisSource?.Trim().ToLowerInvariant() switch
        {
            "groq" => "groq",
            "mock_network" => "mock_network",
            "mock" => "mock",
            _ => !string.IsNullOrWhiteSpace(ai.AnalysisSource) ? ai.AnalysisSource.Trim().ToLowerInvariant() : "mock"
        };

        // 3) Analiz sonucunu danışan ile ilişkilendirerek veritabanına yaz
        var entity = new ClientPdfAnalysis
        {
            ClientId = clientId,
            OriginalFileName = safeName,
            PdfRelativeUrl = saved.RelativePublicUrl,
            DocumentType = ai.DocumentType,
            Summary = ai.Summary,
            KeyFindings = ai.KeyFindings ?? new List<string>(),
            Cautions = ai.Cautions ?? new List<string>(),
            SuggestedForDietitian = ai.SuggestedForDietitian ?? new List<string>(),
            AnalysisSource = source
        };

        await repository.InsertAsync(entity, cancellationToken).ConfigureAwait(false);

        return ToUploadDto(entity);
    }

    /// <summary>Geçmiş ekranı: tam analiz metni dahil tüm alanlar listelenir.</summary>
    public async Task<IReadOnlyList<ClientPdfAnalysisListItemDto>> GetMyAnalysesAsync(
        string clientId,
        int take,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(clientId)) return Array.Empty<ClientPdfAnalysisListItemDto>();
        var rows = await repository.GetByClientIdAsync(clientId, take, cancellationToken).ConfigureAwait(false);
        return rows.Select(
                r => new ClientPdfAnalysisListItemDto
                {
                    Id = r.Id ?? string.Empty,
                    PdfUrl = r.PdfRelativeUrl,
                    OriginalFileName = r.OriginalFileName,
                    DocumentType = r.DocumentType,
                    Summary = r.Summary,
                    KeyFindings = r.KeyFindings,
                    Cautions = r.Cautions,
                    SuggestedForDietitian = r.SuggestedForDietitian,
                    AnalysisSource = r.AnalysisSource,
                    CreatedAtUtc = r.CreatedAtUtc
                })
            .ToList();
    }

    private static ClientPdfAnalysisUploadResponseDto ToUploadDto(ClientPdfAnalysis r) =>
        new()
        {
            Id = r.Id ?? string.Empty,
            PdfUrl = r.PdfRelativeUrl,
            OriginalFileName = r.OriginalFileName,
            DocumentType = r.DocumentType,
            Summary = r.Summary,
            KeyFindings = r.KeyFindings,
            Cautions = r.Cautions,
            SuggestedForDietitian = r.SuggestedForDietitian,
            AnalysisSource = r.AnalysisSource,
            CreatedAtUtc = r.CreatedAtUtc
        };
}
