using Microsoft.Extensions.Options;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Options;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Services;

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

        await using var uploadStream = new MemoryStream(bytes, writable: false);
        var saved = await pdfStorage.SavePdfAsync(uploadStream, cancellationToken).ConfigureAwait(false);

        var ai = await pdfAnalysisAi.AnalyzePdfAsync(bytes, safeName, cancellationToken).ConfigureAwait(false);

        var source = string.Equals(ai.AnalysisSource, "mock", StringComparison.OrdinalIgnoreCase) ? "mock" : "gemini";

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
