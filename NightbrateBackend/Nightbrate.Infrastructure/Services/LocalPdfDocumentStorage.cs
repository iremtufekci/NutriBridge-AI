using Microsoft.Extensions.Options; // Yapılandırma seçenekleri enjeksiyonu
using Nightbrate.Application.DTOs; // PdfDocumentSaveResult DTO
using Nightbrate.Application.Interfaces; // IPdfDocumentStorage arayüzü
using Nightbrate.Application.Options; // PdfUploadOptions yapılandırması

namespace Nightbrate.Infrastructure.Services; // Altyapı servisleri ad alanı

public class LocalPdfDocumentStorage(IOptions<PdfUploadOptions> options) : IPdfDocumentStorage // PDF belgesini diske kaydeder
{
    private readonly PdfUploadOptions _opt = options.Value; // Yükleme dizini ve URL ayarları

    public async Task<PdfDocumentSaveResult> SavePdfAsync(Stream fileStream, CancellationToken cancellationToken = default) // PDF akışını kaydet
    {
        if (string.IsNullOrWhiteSpace(_opt.PdfsDirectory)) // Dizin yapılandırılmamışsa
            throw new InvalidOperationException("PdfUploadOptions.PdfsDirectory yapilandirilmadi."); // Hata fırlat

        Directory.CreateDirectory(_opt.PdfsDirectory); // Hedef klasör yoksa oluştur
        var name = $"{Guid.NewGuid():N}.pdf"; // Benzersiz PDF dosya adı
        var fullPath = Path.Combine(_opt.PdfsDirectory, name); // Tam dosya yolu
        await using (var fs = new FileStream(fullPath, FileMode.CreateNew, FileAccess.Write, FileShare.None, 65536, useAsync: true)) // Yeni dosya aç
        {
            await fileStream.CopyToAsync(fs, cancellationToken).ConfigureAwait(false); // Gelen akışı diske yaz
        }

        var rel = $"{_opt.PublicRelativePath.TrimEnd('/')}/{name}"; // İstemciye dönülecek göreli URL
        return new PdfDocumentSaveResult { FullPath = fullPath, RelativePublicUrl = rel }; // Kayıt sonucunu döndür
    }
}
