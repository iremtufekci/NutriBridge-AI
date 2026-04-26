using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace Nightbrate.Core.Entities;

public class DietProgram
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string DietitianId { get; set; } = string.Empty;
    public string ClientId { get; set; } = string.Empty;

    /// <summary>Atama tarihi, yyyy-MM-dd (ör. diyetisyenin seçtiği gün, UTC günü ile hizalanmış).</summary>
    public string ProgramDate { get; set; } = string.Empty;

    public string Breakfast { get; set; } = string.Empty;
    public string Lunch { get; set; } = string.Empty;
    public string Dinner { get; set; } = string.Empty;
    public string Snack { get; set; } = string.Empty;

    public int BreakfastCalories { get; set; }
    public int LunchCalories { get; set; }
    public int DinnerCalories { get; set; }
    public int SnackCalories { get; set; }

    /// <summary>Öğün kalorileri toplamı (sunucu tarafında senkron tutulur).</summary>
    public int TotalCalories { get; set; }

    public bool BreakfastCompleted { get; set; }
    public bool LunchCompleted { get; set; }
    public bool DinnerCompleted { get; set; }
    public bool SnackCompleted { get; set; }

    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}
