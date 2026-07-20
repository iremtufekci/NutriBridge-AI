using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Services;
using Nightbrate.Application.Utils;

namespace Nightbrate.API.Services;

public sealed class FallbackPdfAnalysisService(
    IPdfAnalysisAiService primary,
    MockPdfAnalysisService mock,
    ILogger<FallbackPdfAnalysisService> logger) : IPdfAnalysisAiService
{
    public async Task<ClientPdfAnalysisResultDto> AnalyzePdfAsync(
        byte[] pdfBytes,
        string originalFileName,
        CancellationToken cancellationToken = default)
    {
        try
        {
            return await primary.AnalyzePdfAsync(pdfBytes, originalFileName, cancellationToken).ConfigureAwait(false);
        }
        catch (Exception ex) when (AiNetworkExceptionHelper.IsNetworkOrDnsFailure(ex))
        {
            logger.LogWarning(ex, "PDF analizi AI erisilemedi; mock kullaniliyor.");
            var r = await mock.AnalyzePdfAsync(pdfBytes, originalFileName, cancellationToken).ConfigureAwait(false);
            r.AnalysisSource = "mock_network";
            return r;
        }
    }
}
