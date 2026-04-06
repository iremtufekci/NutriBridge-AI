using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace Nightbrate.Core.Entities;

public class User
{
    [BsonId] // Bu alanın anahtar (ID) olduğunu belirtir
    [BsonRepresentation(BsonType.ObjectId)] // String Id'yi MongoDB'nin ObjectId formatına çevirir
    public string? Id { get; set; }

    public string Username { get; set; } = null!;
    public string Email { get; set; } = null!;
    
    public byte[] PasswordHash { get; set; } = null!;
    public byte[] PasswordSalt { get; set; } = null!;

    // Kullanıcının rolü
    public UserRole Role { get; set; } = UserRole.Client;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}