using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace Nightbrate.Core.Entities;

/// <summary>Diyetisyen aynı gün planını güncellediğinde eski sürüm burada saklanır; güncel veri DietPrograms’da.</summary>
public class DietProgramHistory
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string ClientId { get; set; } = string.Empty;
    public string DietitianId { get; set; } = string.Empty;
    public string ProgramDate { get; set; } = string.Empty;

    public string Breakfast { get; set; } = string.Empty;
    public string Lunch { get; set; } = string.Empty;
    public string Dinner { get; set; } = string.Empty;
    public string Snack { get; set; } = string.Empty;

    public int BreakfastCalories { get; set; }
    public int LunchCalories { get; set; }
    public int DinnerCalories { get; set; }
    public int SnackCalories { get; set; }

    public int TotalCalories { get; set; }

    /// <summary>Arşivlenen sürümün o zamanki <see cref="DietProgram.UpdatedAt"/> değeri.</summary>
    public DateTime ProgramContentUpdatedAt { get; set; }

    /// <summary>Programın yeni sürümle değiştirildiği an (UTC).</summary>
    public DateTime SupersededAt { get; set; }

    /// <summary>Güncel tablodaki belgenin <c>DietPrograms._id</c> (değiştirilmeden önce).</summary>
    public string? SupersededDietProgramId { get; set; }
}
