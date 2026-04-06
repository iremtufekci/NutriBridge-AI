using MongoDB.Bson; // Bunu ekle
using MongoDB.Bson.Serialization.Attributes; // Bunu ekle

namespace Nightbrate.Core.Entities;

public class Dietitian 
{
    [BsonId] // MongoDB'ye bunun "Primary Key" olduğunu söyler
    [BsonRepresentation(BsonType.ObjectId)] // Veritabanındaki ObjectId'yi C# tarafında string olarak yönetmeni sağlar
    public string? Id { get; set; } 

    public string FirstName { get; set; } = null!;
    public string LastName { get; set; } = null!;
    public string Email { get; set; } = null!;
    
    public byte[] PasswordHash { get; set; } = null!; 
    public byte[] PasswordSalt { get; set; } = null!; 
    
    public string DiplomaNo { get; set; } = null!;
    public string ClinicName { get; set; } = null!;
    public bool IsApproved { get; set; }
    
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}