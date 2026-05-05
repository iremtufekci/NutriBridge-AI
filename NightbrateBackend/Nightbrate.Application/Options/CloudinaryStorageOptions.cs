namespace Nightbrate.Application.Options;

/// <summary>Cloudinary hesabi: fotograflar image, PDF raw olarak yuklenir.
/// Ayrica <c>CLOUDINARY_URL=cloudinary://api_key:api_secret@cloud_name</c> ortam degiskeni ile doldurulabilir (bos alanlar uzerine yazilir).</summary>
public sealed class CloudinaryStorageOptions
{
    public string CloudName { get; set; } = string.Empty;
    public string ApiKey { get; set; } = string.Empty;
    public string ApiSecret { get; set; } = string.Empty;

    /// <summary>Ornek: nightbrate/meals</summary>
    public string MealsFolder { get; set; } = "nightbrate/meals";

    /// <summary>Ornek: nightbrate/pdfs</summary>
    public string PdfsFolder { get; set; } = "nightbrate/pdfs";
}
