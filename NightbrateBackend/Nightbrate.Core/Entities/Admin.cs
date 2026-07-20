namespace Nightbrate.Core.Entities; // Varlık sınıfları ad alanı

/// <summary>Admin hesapları Users koleksiyonunda (polimorfik serileştirme) saklanır.</summary>
public class Admin : BaseUser // Yönetici kullanıcı varlığı
{
    public string FirstName { get; set; } = string.Empty; // Admin adı
    public string LastName { get; set; } = string.Empty; // Admin soyadı
}
