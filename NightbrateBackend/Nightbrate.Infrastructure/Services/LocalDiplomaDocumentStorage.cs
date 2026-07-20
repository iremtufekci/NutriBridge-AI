using Microsoft.Extensions.Options;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Options;

namespace Nightbrate.Infrastructure.Services;

public sealed class LocalDiplomaDocumentStorage(IOptions<DiplomaUploadOptions> options) : IDiplomaDocumentStorage
{
    private readonly DiplomaUploadOptions _opt = options.Value;

    public async Task<DiplomaDocumentSaveResult> SaveAsync(
        Stream fileStream,
        string originalFileName,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(_opt.DiplomasDirectory))
            throw new InvalidOperationException("DiplomaUploadOptions.DiplomasDirectory yapilandirilmadi.");

        Directory.CreateDirectory(_opt.DiplomasDirectory);

        var ext = Path.GetExtension(originalFileName);
        if (string.IsNullOrWhiteSpace(ext))
            ext = ".pdf";
        ext = ext.ToLowerInvariant();
        if (ext is not ".pdf" and not ".jpg" and not ".jpeg" and not ".png")
            ext = ".pdf";

        var name = $"{Guid.NewGuid():N}{ext}";
        var fullPath = Path.Combine(_opt.DiplomasDirectory, name);
        await using (var fs = new FileStream(fullPath, FileMode.CreateNew, FileAccess.Write, FileShare.None, 65536, useAsync: true))
        {
            await fileStream.CopyToAsync(fs, cancellationToken).ConfigureAwait(false);
        }

        var rel = $"{_opt.PublicRelativePath.TrimEnd('/')}/{name}";
        return new DiplomaDocumentSaveResult { RelativePublicUrl = rel };
    }
}
