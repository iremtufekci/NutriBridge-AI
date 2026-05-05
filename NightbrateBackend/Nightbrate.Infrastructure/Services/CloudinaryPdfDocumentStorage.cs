using CloudinaryDotNet;
using CloudinaryDotNet.Actions;
using Microsoft.Extensions.Options;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Options;

namespace Nightbrate.Infrastructure.Services;

public sealed class CloudinaryPdfDocumentStorage(
    Cloudinary cloudinary,
    IOptions<CloudinaryStorageOptions> options) : IPdfDocumentStorage
{
    private readonly CloudinaryStorageOptions _opt = options.Value;

    public async Task<PdfDocumentSaveResult> SavePdfAsync(Stream fileStream, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(_opt.CloudName))
            throw new InvalidOperationException("CloudinaryStorageOptions yapilandirilmadi.");

        var fileName = $"{Guid.NewGuid():N}.pdf";
        var folder = string.IsNullOrWhiteSpace(_opt.PdfsFolder) ? null : _opt.PdfsFolder.Trim().Trim('/');

        var uploadParams = new RawUploadParams
        {
            File = new FileDescription(fileName, fileStream),
            Folder = folder,
            UniqueFilename = true,
            Overwrite = false
        };

        var result = await cloudinary.UploadAsync(uploadParams).ConfigureAwait(false);

        if (result.Error is not null)
            throw new AppException($"Cloudinary PDF yuklemesi basarisiz: {result.Error.Message}");

        var url = result.SecureUrl?.AbsoluteUri ?? result.Url?.AbsoluteUri;
        if (string.IsNullOrWhiteSpace(url))
            throw new AppException("Cloudinary yanitinda PDF adresi yok.");

        return new PdfDocumentSaveResult { FullPath = string.Empty, RelativePublicUrl = url };
    }
}
