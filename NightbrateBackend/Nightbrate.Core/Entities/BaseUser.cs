using MongoDB.Bson; // MongoDB BSON tipleri için
using MongoDB.Bson.Serialization.Attributes; // MongoDB serileştirme öznitelikleri

namespace Nightbrate.Core.Entities; // Varlık sınıfları ad alanı

[BsonDiscriminator(RootClass = true)] // Polimorfik kök sınıf; alt türler ayrı kaydedilir
[BsonKnownTypes(typeof(Dietitian), typeof(Client), typeof(Admin))] // Bilinen alt kullanıcı türleri
public abstract class BaseUser // Tüm kullanıcı rolleri için ortak taban sınıf
{
    [BsonId] // MongoDB birincil anahtar alanı
    [BsonRepresentation(BsonType.ObjectId)] // Id string olarak ObjectId biçiminde saklanır
    public string? Id { get; set; } // Kullanıcının benzersiz kimliği

    public string Email { get; set; } = string.Empty; // Giriş e-posta adresi
    public byte[] PasswordHash { get; set; } = Array.Empty<byte>(); // Şifrenin hash değeri
    public byte[] PasswordSalt { get; set; } = Array.Empty<byte>(); // Hash için tuz (salt)
    public UserRole Role { get; set; } // Kullanıcı rolü (Admin, Dietitian, Client)
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow; // Hesabın oluşturulma zamanı (UTC)

    /// <summary>Arama / iletişim listesi; boş bırakılabilir.</summary>
    public string Phone { get; set; } = string.Empty; // Telefon numarası

    public bool IsSuspended { get; set; } // Hesap askıya alındı mı
    public string? SuspensionMessage { get; set; } // Askıya alma mesajı (varsa)
    public DateTime? SuspendedAt { get; set; } // Askıya alınma zamanı (varsa)

    /// <summary>light | dark — tüm rol hesaplarında veritabanında saklanır.</summary>
    public string ThemePreference { get; set; } = "light"; // UI tema tercihi
}
