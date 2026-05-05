using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface IMealAnalysisService
{
    Task<MealAnalysisResultDto> AnalyzeImageAsync(string imageFilePath, CancellationToken cancellationToken = default);

    /// <summary>Gemini gorsel analizi (Cloudinary vb. icin dosya yolu yerine bayt).</summary>
    Task<MealAnalysisResultDto> AnalyzeImageBytesAsync(byte[] imageBytes, string mimeType, CancellationToken cancellationToken = default);
}
