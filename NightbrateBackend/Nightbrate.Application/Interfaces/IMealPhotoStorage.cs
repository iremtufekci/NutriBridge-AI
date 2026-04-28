using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface IMealPhotoStorage
{
    Task<MealPhotoSaveResult> SaveMealImageAsync(Stream fileStream, string extensionWithDot, CancellationToken cancellationToken = default);
}
