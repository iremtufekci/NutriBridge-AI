using Microsoft.Extensions.Options; // Yapılandırma seçenekleri enjeksiyonu
using Nightbrate.Application.DTOs; // MealPhotoSaveResult DTO
using Nightbrate.Application.Interfaces; // IMealPhotoStorage arayüzü
using Nightbrate.Application.Options; // MealUploadOptions yapılandırması

namespace Nightbrate.Infrastructure.Services; // Altyapı servisleri ad alanı

public class LocalMealPhotoStorage(IOptions<MealUploadOptions> options) : IMealPhotoStorage // Yemek fotoğrafını diske kaydeder
{
    private readonly MealUploadOptions _opt = options.Value; // Yükleme dizini ve URL ayarları

    public async Task<MealPhotoSaveResult> SaveMealImageAsync(Stream fileStream, string extensionWithDot, CancellationToken cancellationToken = default) // Görseli kaydet
    {
        if (string.IsNullOrWhiteSpace(_opt.MealsDirectory)) // Dizin yapılandırılmamışsa
            throw new InvalidOperationException("MealUploadOptions.MealsDirectory yapilandirilmadi."); // Hata fırlat

        Directory.CreateDirectory(_opt.MealsDirectory); // Hedef klasör yoksa oluştur
        var safeExt = extensionWithDot.StartsWith('.') ? extensionWithDot : "." + extensionWithDot; // Uzantıyı noktalı formata getir
        var name = $"{Guid.NewGuid():N}{safeExt}"; // Benzersiz dosya adı üret
        var fullPath = Path.Combine(_opt.MealsDirectory, name); // Tam dosya yolu
        await using (var fs = new FileStream(fullPath, FileMode.CreateNew, FileAccess.Write, FileShare.None, 65536, useAsync: true)) // Yeni dosya aç
        {
            await fileStream.CopyToAsync(fs, cancellationToken).ConfigureAwait(false); // Gelen akışı diske yaz
        }

        var rel = $"{_opt.PublicRelativePath.TrimEnd('/')}/{name}"; // İstemciye dönülecek göreli URL
        return new MealPhotoSaveResult { FullPath = fullPath, RelativePublicUrl = rel }; // Kayıt sonucunu döndür
    }
}
