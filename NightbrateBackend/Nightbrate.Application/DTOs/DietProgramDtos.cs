namespace Nightbrate.Application.DTOs;

public class SaveDietProgramDto
{
    public string ClientId { get; set; } = string.Empty;

    /// <summary>Atama günü yyyy-MM-dd</summary>
    public string ProgramDate { get; set; } = string.Empty;
    public string Breakfast { get; set; } = string.Empty;
    public string Lunch { get; set; } = string.Empty;
    public string Dinner { get; set; } = string.Empty;
    public string Snack { get; set; } = string.Empty;
    public int BreakfastCalories { get; set; }
    public int LunchCalories { get; set; }
    public int DinnerCalories { get; set; }
    public int SnackCalories { get; set; }
    /// <summary>İstemci günün toplamını gönderebilir; sunucu öğün toplamına eşitler.</summary>
    public int TotalCalories { get; set; }
}

/// <summary>GET diet-program: MongoDB DietPrograms koleksiyonundan (danışan + gün) kaydı.</summary>
public class DietProgramViewDto
{
    public string ClientId { get; set; } = string.Empty;
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
    public bool HasSavedProgram { get; set; }
    public DateTime? UpdatedAt { get; set; }
    public bool BreakfastCompleted { get; set; }
    public bool LunchCompleted { get; set; }
    public bool DinnerCompleted { get; set; }
    public bool SnackCompleted { get; set; }
}

/// <summary>Danışan: kendisine atanan haftalık gün programları (DietPrograms).</summary>
public class ClientDietProgramDayDto
{
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
    public bool HasProgram { get; set; }
    public DateTime? UpdatedAt { get; set; }
    public string? DietitianName { get; set; }
    public bool BreakfastCompleted { get; set; }
    public bool LunchCompleted { get; set; }
    public bool DinnerCompleted { get; set; }
    public bool SnackCompleted { get; set; }
}

public class SetMealCompletedDto
{
    /// <summary>Atama günü yyyy-MM-dd</summary>
    public string ProgramDate { get; set; } = string.Empty;

    /// <summary>breakfast, lunch, dinner, snack</summary>
    public string Meal { get; set; } = string.Empty;
}
