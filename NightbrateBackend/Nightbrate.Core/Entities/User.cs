using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace Nightbrate.Core.Entities;

public class User
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string Username { get; set; } = null!;
    public string Email { get; set; } = null!;
    
    // Şifreyi açık metin olarak değil, hashlenmiş halde saklayacağız
    public byte[] PasswordHash { get; set; } = null!;
    public byte[] PasswordSalt { get; set; } = null!;

    // Kullanıcının rolü (Varsayılan olarak normal kullanıcı)
    public UserRole Role { get; set; } = UserRole.User;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}