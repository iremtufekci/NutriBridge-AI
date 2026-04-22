using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace Nightbrate.Core.Entities;

public class WaterLog
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string ClientId { get; set; } = string.Empty;
    public int Ml { get; set; }
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;
}
