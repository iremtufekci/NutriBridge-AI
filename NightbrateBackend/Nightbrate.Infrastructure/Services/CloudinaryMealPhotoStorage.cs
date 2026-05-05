using CloudinaryDotNet;
using CloudinaryDotNet.Actions;
using Microsoft.Extensions.Options;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Options;

namespace Nightbrate.Infrastructure.Services;

public sealed class CloudinaryMealPhotoStorage(
    Cloudinary cloudinary,
    IOptions<CloudinaryStorageOptions> options) : IMealPhotoStorage
{
    private readonly CloudinaryStorageOptions _opt = options.Value;

    public async Task<MealPhotoSaveResult> SaveMealImageAsync(Stream fileStream, string extensionWithDot, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(_opt.CloudName))
            throw new InvalidOperationException("CloudinaryStorageOptions yapilandirilmadi.");

        var safeExt = extensionWithDot.StartsWith('.') ? extensionWithDot : "." + extensionWithDot;
        var fileName = $"{Guid.NewGuid():N}{safeExt}";

        var folder = string.IsNullOrWhiteSpace(_opt.MealsFolder) ? null : _opt.MealsFolder.Trim().Trim('/');

        var uploadParams = new ImageUploadParams
        {
            File = new FileDescription(fileName, fileStream),
            Folder = folder,
            UniqueFilename = true,
            Overwrite = false
        };

        var result = await cloudinary.UploadAsync(uploadParams).ConfigureAwait(false);

        if (result.Error is not null)
            throw new AppException($"Cloudinary fotograf yuklemesi basarisiz: {result.Error.Message}");

        var url = result.SecureUrl?.AbsoluteUri ?? result.Url?.AbsoluteUri;
        if (string.IsNullOrWhiteSpace(url))
            throw new AppException("Cloudinary yanitinda dosya adresi yok.");

        return new MealPhotoSaveResult { FullPath = string.Empty, RelativePublicUrl = url };
    }
}
