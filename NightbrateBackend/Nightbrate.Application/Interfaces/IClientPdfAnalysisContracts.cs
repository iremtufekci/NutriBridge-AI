using Nightbrate.Application.DTOs;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IPdfDocumentStorage
{
    Task<PdfDocumentSaveResult> SavePdfAsync(Stream fileStream, CancellationToken cancellationToken = default);
}

public interface IPdfAnalysisAiService
{
    Task<ClientPdfAnalysisResultDto> AnalyzePdfAsync(byte[] pdfBytes, string originalFileName, CancellationToken cancellationToken = default);
}

public interface IClientPdfAnalysisRepository
{
    Task InsertAsync(ClientPdfAnalysis doc, CancellationToken cancellationToken = default);

    Task<ClientPdfAnalysis?> GetByIdAsync(string id, CancellationToken cancellationToken = default);

    Task<IReadOnlyList<ClientPdfAnalysis>> GetByClientIdAsync(string clientId, int take, CancellationToken cancellationToken = default);
}

public interface IClientPdfAnalysisService
{
    Task<ClientPdfAnalysisUploadResponseDto> UploadAnalyzeAndPersistAsync(
        string clientId,
        Stream pdfStream,
        string originalFileName,
        CancellationToken cancellationToken = default);

    Task<IReadOnlyList<ClientPdfAnalysisListItemDto>> GetMyAnalysesAsync(string clientId, int take, CancellationToken cancellationToken = default);
}
