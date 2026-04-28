using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface IMealAnalysisService
{
    Task<MealAnalysisResultDto> AnalyzeImageAsync(string imageFilePath, CancellationToken cancellationToken = default);
}
