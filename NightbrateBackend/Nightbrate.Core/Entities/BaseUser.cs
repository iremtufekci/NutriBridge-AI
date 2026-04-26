using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace Nightbrate.Core.Entities;

[BsonDiscriminator(RootClass = true)]
[BsonKnownTypes(typeof(Dietitian), typeof(Client), typeof(Admin))]
public abstract class BaseUser
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string Email { get; set; } = string.Empty;
    public byte[] PasswordHash { get; set; } = Array.Empty<byte>();
    public byte[] PasswordSalt { get; set; } = Array.Empty<byte>();
    public UserRole Role { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    /// <summary>Arama / iletişim listesi; boş bırakılabilir.</summary>
    public string Phone { get; set; } = string.Empty;

    public bool IsSuspended { get; set; }
    public string? SuspensionMessage { get; set; }
    public DateTime? SuspendedAt { get; set; }

    /// <summary>light | dark — tüm rol hesaplarında veritabanında saklanır.</summary>
    public string ThemePreference { get; set; } = "light";
}
