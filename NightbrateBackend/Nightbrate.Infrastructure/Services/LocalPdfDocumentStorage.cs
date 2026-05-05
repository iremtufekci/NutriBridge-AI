using Microsoft.Extensions.Options;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Options;

namespace Nightbrate.Infrastructure.Services;

public class LocalPdfDocumentStorage(IOptions<PdfUploadOptions> options) : IPdfDocumentStorage
{
    private readonly PdfUploadOptions _opt = options.Value;

    public async Task<PdfDocumentSaveResult> SavePdfAsync(Stream fileStream, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(_opt.PdfsDirectory))
            throw new InvalidOperationException("PdfUploadOptions.PdfsDirectory yapilandirilmadi.");

        Directory.CreateDirectory(_opt.PdfsDirectory);
        var name = $"{Guid.NewGuid():N}.pdf";
        var fullPath = Path.Combine(_opt.PdfsDirectory, name);
        await using (var fs = new FileStream(fullPath, FileMode.CreateNew, FileAccess.Write, FileShare.None, 65536, useAsync: true))
        {
            await fileStream.CopyToAsync(fs, cancellationToken).ConfigureAwait(false);
        }

        var rel = $"{_opt.PublicRelativePath.TrimEnd('/')}/{name}";
        return new PdfDocumentSaveResult { FullPath = fullPath, RelativePublicUrl = rel };
    }
}
