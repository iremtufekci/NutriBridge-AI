namespace Nightbrate.Application.DTOs;

public sealed class DietitianTodayTasksBundleDto
{
    public string TaskDate { get; set; } = string.Empty;
    public int PendingCount { get; set; }
    public int CompletedCount { get; set; }
    public int TotalCount { get; set; }
    public IReadOnlyList<DietitianDailyTaskItemDto> Tasks { get; set; } = Array.Empty<DietitianDailyTaskItemDto>();
}

public sealed class DietitianDailyTaskItemDto
{
    public string Id { get; set; } = string.Empty;
    public string TaskKey { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public string Subtitle { get; set; } = string.Empty;
    public string Category { get; set; } = string.Empty;
    public string? ClientId { get; set; }
    public bool IsCompleted { get; set; }
    public string DueLabel { get; set; } = "Bugün";
}

public sealed class SetDietitianTaskCompleteDto
{
    public bool IsCompleted { get; set; }
}
