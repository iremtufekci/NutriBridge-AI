using MongoDB.Bson; // Bunu ekle
using MongoDB.Bson.Serialization.Attributes; // Bunu ekle

namespace Nightbrate.Core.Entities
{
    public class Client
    {
        [BsonId] // MongoDB'nin bu alanı kimlik olarak kullanmasını sağlar
        [BsonRepresentation(BsonType.ObjectId)] // String Id'yi veritabanındaki ObjectId ile eşleştirir
        public string? Id { get; set; } 

        public string FirstName { get; set; } = null!;
        public string LastName { get; set; } = null!;
        public string Email { get; set; } = null!;
        
        // Giriş güvenliği için gerekenler
        public byte[] PasswordHash { get; set; } = null!;
        public byte[] PasswordSalt { get; set; } = null!;
        
        public double Height { get; set; }
        public double Weight { get; set; }
        public string Goal { get; set; } = null!;
        public string ActivityLevel { get; set; } = null!;
        public DateTime BirthDate { get; set; }
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    }
}