using MongoDB.Bson; // MongoDB BSON tipleri
using MongoDB.Bson.Serialization.Attributes; // Serileştirme öznitelikleri

namespace Nightbrate.Core.Entities; // Varlık sınıfları ad alanı

/// <summary>Diyetisyen aynı gün planını güncellediğinde eski sürüm burada saklanır; güncel veri DietPrograms'da.</summary>
public class DietProgramHistory // Arşivlenmiş diyet programı sürümü
{
    [BsonId] // MongoDB birincil anahtar
    [BsonRepresentation(BsonType.ObjectId)] // ObjectId string temsili
    public string? Id { get; set; } // Arşiv kaydı kimliği

    public string ClientId { get; set; } = string.Empty; // İlgili danışan
    public string DietitianId { get; set; } = string.Empty; // İlgili diyetisyen
    public string ProgramDate { get; set; } = string.Empty; // Program günü (yyyy-MM-dd)

    public string Breakfast { get; set; } = string.Empty; // Eski kahvaltı içeriği
    public string Lunch { get; set; } = string.Empty; // Eski öğle içeriği
    public string Dinner { get; set; } = string.Empty; // Eski akşam içeriği
    public string Snack { get; set; } = string.Empty; // Eski ara öğün içeriği

    public int BreakfastCalories { get; set; } // Eski kahvaltı kalorisi
    public int LunchCalories { get; set; } // Eski öğle kalorisi
    public int DinnerCalories { get; set; } // Eski akşam kalorisi
    public int SnackCalories { get; set; } // Eski ara öğün kalorisi

    public int TotalCalories { get; set; } // Eski toplam kalori

    /// <summary>Arşivlenen sürümün o zamanki <see cref="DietProgram.UpdatedAt"/> değeri.</summary>
    public DateTime ProgramContentUpdatedAt { get; set; } // Arşivlenen sürümün güncellenme zamanı

    /// <summary>Programın yeni sürümle değiştirildiği an (UTC).</summary>
    public DateTime SupersededAt { get; set; } // Yeni sürümle değiştirilme anı

    /// <summary>Güncel tablodaki belgenin <c>DietPrograms._id</c> (değiştirilmeden önce).</summary>
    public string? SupersededDietProgramId { get; set; } // Değiştirilmeden önceki program id
}
