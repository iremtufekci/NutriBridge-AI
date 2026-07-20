namespace Nightbrate.Core.Entities; // Varlık sınıfları ad alanı

public class Dietitian : BaseUser // Diyetisyen kullanıcı varlığı
{
    public string FirstName { get; set; } = string.Empty; // Diyetisyen adı
    public string LastName { get; set; } = string.Empty; // Diyetisyen soyadı
    public string DiplomaNo { get; set; } = string.Empty; // Diploma numarası
    public string ClinicName { get; set; } = string.Empty; // Klinik / muayenehane adı
    public string? DiplomaDocumentUrl { get; set; } // Yüklenen diploma belgesi URL'si
    public bool IsApproved { get; set; } = false; // Admin onayı alındı mı

    /// <summary>Onay sonrasi: 6 hane, büyük harf + rakam, sistemde eşi olmaz; danışan eşleşmesi için.</summary>
    public string? ConnectionCode { get; set; } // Danışan eşleştirme bağlantı kodu
}
