using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface IMealPhotoAnalysisService
{
    Task<MealPhotoAnalysisResponseDto> UploadAnalyzeAndPersistAsync(
        string clientId,
        Stream fileStream,
        string extensionWithDot,
        CancellationToken cancellationToken = default);
}
