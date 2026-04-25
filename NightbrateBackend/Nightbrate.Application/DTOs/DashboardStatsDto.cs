namespace Nightbrate.Application.DTOs;

public class DashboardStatsDto
{
    /// <summary>Users koleksiyonundaki tüm kayit (onay bekleyen diyetisyen dahil)</summary>
    public long TotalUsers { get; set; }
    public long ActiveUsers { get; set; }
    public long TotalClients { get; set; }
    public long TotalDietitians { get; set; }
    public long ActiveDietitians { get; set; }
    public long PendingDietitians { get; set; }
    public IReadOnlyList<RoleCountDto> RoleDistribution { get; set; } = Array.Empty<RoleCountDto>();
    public IReadOnlyList<MonthlyRegistrationDto> MonthlyRegistrations { get; set; } = Array.Empty<MonthlyRegistrationDto>();
}
