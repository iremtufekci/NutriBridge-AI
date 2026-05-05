namespace Nightbrate.Application.DTOs;

public sealed class DietitianMyClientsResponseDto
{
    public DietitianClientTabCountsDto TabCounts { get; set; } = new();
    public List<DietitianClientCardDto> Clients { get; set; } = new();
}

public sealed class DietitianClientTabCountsDto
{
    public int All { get; set; }
    public int Active { get; set; }
    public int Critical { get; set; }
    public int Passive { get; set; }
}

/// <summary>critical | active | passive — uyarı ve son aktiviteye göre.</summary>
public sealed class DietitianClientCardDto
{
    public string Id { get; set; } = string.Empty;
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public string DisplayName { get; set; } = string.Empty;
    public DateTime StartedAtUtc { get; set; }
    public DateTime? LastActivityUtc { get; set; }
    /// <summary>0–100: son 3 günden birinde diyet programı öğün tamamlama oranı.</summary>
    public int CompliancePercent { get; set; }
    public string Segment { get; set; } = "passive";
    public bool IsCritical { get; set; }
}

public sealed class DietitianClientOverviewDto
{
    public ClientBriefDto Client { get; set; } = new();
    public int CompliancePercent { get; set; }
    public string? ComplianceReferenceDate { get; set; }
    /// <summary>Bu haftanın (Pzt–Paz, UTC takvimine göre) günlük öğünleri ve tamamlanma durumu.</summary>
    public List<DietitianProgramDayOverviewDto> WeeklyProgramDays { get; set; } = new();
    public IReadOnlyList<KitchenChefRecipeLogItemDto> KitchenRecipeLogs { get; set; } = Array.Empty<KitchenChefRecipeLogItemDto>();
    public IReadOnlyList<ClientPdfAnalysisListItemDto> PdfAnalyses { get; set; } = Array.Empty<ClientPdfAnalysisListItemDto>();
}

public sealed class DietitianProgramDayOverviewDto
{
    public string ProgramDate { get; set; } = string.Empty;
    public string WeekdayLabel { get; set; } = string.Empty;
    public List<DietitianProgramMealOverviewDto> Meals { get; set; } = new();
}

public sealed class DietitianProgramMealOverviewDto
{
    public string MealKey { get; set; } = string.Empty;
    public string Label { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public int Calories { get; set; }
    public bool Completed { get; set; }
}
