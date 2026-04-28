using Microsoft.Extensions.Options;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Options;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Services;

public class MealPhotoAnalysisService(
    IMealPhotoStorage mealPhotoStorage,
    IMealAnalysisService mealAnalysisService,
    IMealLogRepository mealLogRepository,
    IClientRepository clientRepository,
    IActivityLogService activityLogService,
    IOptions<GeminiMealAnalysisOptions> geminiOptions) : IMealPhotoAnalysisService
{
    public async Task<MealPhotoAnalysisResponseDto> UploadAnalyzeAndPersistAsync(
        string clientId,
        Stream fileStream,
        string extensionWithDot,
        CancellationToken cancellationToken = default)
    {
        var saved = await mealPhotoStorage.SaveMealImageAsync(fileStream, extensionWithDot, cancellationToken)
            .ConfigureAwait(false);
        var analysis = await mealAnalysisService.AnalyzeImageAsync(saved.FullPath, cancellationToken)
            .ConfigureAwait(false);

        var meal = new MealLog
        {
            ClientId = clientId,
            PhotoUrl = saved.RelativePublicUrl,
            Calories = analysis.EstimatedCalories,
            DetectedFoods = analysis.Foods.ToList(),
            Macros = new MacroInfo
            {
                Protein = analysis.Protein,
                Carb = analysis.Carb,
                Fat = analysis.Fat
            },
            Timestamp = DateTime.UtcNow
        };

        await mealLogRepository.AddAsync(meal).ConfigureAwait(false);

        var c = await clientRepository.GetByIdAsync(clientId).ConfigureAwait(false);
        if (c is not null)
        {
            var name = $"{c.FirstName} {c.LastName}".Trim();
            if (name.Length == 0) name = c.Email;
            await activityLogService.LogAsync(clientId, name, "Ogun fotografi yuklendi (AI analizi)").ConfigureAwait(false);
        }

        var analysisSource = string.IsNullOrWhiteSpace(geminiOptions.Value.ApiKey) ? "mock" : "gemini";

        return new MealPhotoAnalysisResponseDto
        {
            MealLogId = meal.Id,
            PhotoUrl = meal.PhotoUrl,
            EstimatedCalories = meal.Calories,
            DetectedFoods = meal.DetectedFoods,
            TimestampUtc = meal.Timestamp,
            AnalysisSource = analysisSource
        };
    }
}
