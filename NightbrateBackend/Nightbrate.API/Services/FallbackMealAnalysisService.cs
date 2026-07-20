using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Services;
using Nightbrate.Application.Utils;

namespace Nightbrate.API.Services;

/// <summary>AI erişilemezse (DNS/ağ) otomatik mock analize düşer.</summary>
public sealed class FallbackMealAnalysisService(
    IMealAnalysisService primary,
    MockMealAnalysisService mock,
    ILogger<FallbackMealAnalysisService> logger) : IMealAnalysisService
{
    public async Task<MealAnalysisResultDto> AnalyzeImageAsync(string imageFilePath, CancellationToken cancellationToken = default)
    {
        try
        {
            return await primary.AnalyzeImageAsync(imageFilePath, cancellationToken).ConfigureAwait(false);
        }
        catch (Exception ex) when (AiNetworkExceptionHelper.IsNetworkOrDnsFailure(ex))
        {
            logger.LogWarning(ex, "Yemek analizi AI erisilemedi; mock kullaniliyor.");
            var r = await mock.AnalyzeImageAsync(imageFilePath, cancellationToken).ConfigureAwait(false);
            r.AnalysisSource = "mock_network";
            return r;
        }
    }

    public async Task<MealAnalysisResultDto> AnalyzeImageBytesAsync(byte[] imageBytes, string mimeType, CancellationToken cancellationToken = default)
    {
        try
        {
            return await primary.AnalyzeImageBytesAsync(imageBytes, mimeType, cancellationToken).ConfigureAwait(false);
        }
        catch (Exception ex) when (AiNetworkExceptionHelper.IsNetworkOrDnsFailure(ex))
        {
            logger.LogWarning(ex, "Yemek analizi AI erisilemedi; mock kullaniliyor.");
            var r = await mock.AnalyzeImageBytesAsync(imageBytes, mimeType, cancellationToken).ConfigureAwait(false);
            r.AnalysisSource = "mock_network";
            return r;
        }
    }
}
