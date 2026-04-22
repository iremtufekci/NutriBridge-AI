namespace Nightbrate.Application.DTOs;

public class DashboardStatsDto
{
    public long TotalUsers { get; set; }
    public long TotalClients { get; set; }
    public long TotalDietitians { get; set; }
    public long ActiveDietitians { get; set; }
    public long PendingDietitians { get; set; }
}
