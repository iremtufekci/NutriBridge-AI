using Microsoft.Extensions.Options;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Options;

namespace Nightbrate.Infrastructure.Services;

public class LocalMealPhotoStorage(IOptions<MealUploadOptions> options) : IMealPhotoStorage
{
    private readonly MealUploadOptions _opt = options.Value;

    public async Task<MealPhotoSaveResult> SaveMealImageAsync(Stream fileStream, string extensionWithDot, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(_opt.MealsDirectory))
            throw new InvalidOperationException("MealUploadOptions.MealsDirectory yapilandirilmadi.");

        Directory.CreateDirectory(_opt.MealsDirectory);
        var safeExt = extensionWithDot.StartsWith('.') ? extensionWithDot : "." + extensionWithDot;
        var name = $"{Guid.NewGuid():N}{safeExt}";
        var fullPath = Path.Combine(_opt.MealsDirectory, name);
        await using (var fs = new FileStream(fullPath, FileMode.CreateNew, FileAccess.Write, FileShare.None, 65536, useAsync: true))
        {
            await fileStream.CopyToAsync(fs, cancellationToken).ConfigureAwait(false);
        }

        var rel = $"{_opt.PublicRelativePath.TrimEnd('/')}/{name}";
        return new MealPhotoSaveResult { FullPath = fullPath, RelativePublicUrl = rel };
    }
}
