namespace Nightbrate.Application.DTOs;

public class SystemAnalyticsDto
{
    public SystemKpiBlockDto Kpis { get; set; } = new();
    public List<EndpointPerformanceRowDto> EndpointPerformance { get; set; } = new();
    public List<HourlySeriesDto> DatabaseHourly { get; set; } = new();
    public List<CacheHourlyDto> CacheHourly { get; set; } = new();
    public List<NetworkHourlyDto> NetworkHourly { get; set; } = new();
    public SystemResourcesBlockDto SystemResources { get; set; } = new();
    public List<ErrorLogEntryDto> ErrorLogs { get; set; } = new();
    public List<SecurityEventEntryDto> SecurityEvents { get; set; } = new();
    public int DataWindowHours { get; set; } = 24;
    public DateTime GeneratedAtUtc { get; set; }
    public string DataNote { get; set; } = "";
}

public class SystemKpiBlockDto
{
    public int ApiRequestsPerHour { get; set; }
    public double ApiRequestsPerHourDeltaPercent { get; set; }
    public int AvgQueryTimeMs { get; set; }
    public double AvgQueryTimeDeltaPercent { get; set; }
    public double SecurityScore { get; set; }
    public int SecurityOpenIssues { get; set; }
    public double CacheHitRatioPercent { get; set; }
    public string CacheStatusLabel { get; set; } = "Good";
}

public class EndpointPerformanceRowDto
{
    public string Endpoint { get; set; } = "";
    public int Calls { get; set; }
    public int AvgTimeMs { get; set; }
    public int Errors { get; set; }
    public double SuccessRatePercent { get; set; }
}

public class HourlySeriesDto
{
    public int Hour { get; set; }
    public string Label { get; set; } = "";
    public int Reads { get; set; }
    public int Writes { get; set; }
    public int SlowQueries { get; set; }
}

public class CacheHourlyDto
{
    public int Hour { get; set; }
    public string Label { get; set; } = "";
    public int Hits { get; set; }
    public int Misses { get; set; }
}

public class NetworkHourlyDto
{
    public int Hour { get; set; }
    public string Label { get; set; } = "";
    public double IncomingMbps { get; set; }
    public double OutgoingMbps { get; set; }
}

public class SystemResourcesBlockDto
{
    public double CpuPercent { get; set; }
    public double MemoryPercent { get; set; }
    public string MemoryRefLabel { get; set; } = "";
    public double DiskIoPercent { get; set; }
    public string DiskRefLabel { get; set; } = "";
    public double NetworkMbps { get; set; }
    public double NetworkUp { get; set; }
    public double NetworkDown { get; set; }
    public string NetworkNote { get; set; } = "";
}

public class ErrorLogEntryDto
{
    public int StatusCode { get; set; }
    public string Time { get; set; } = "";
    public string Endpoint { get; set; } = "";
    public string Message { get; set; } = "";
    public int Count { get; set; }
}

public class SecurityEventEntryDto
{
    public string Severity { get; set; } = "Low";
    public string Time { get; set; } = "";
    public string Name { get; set; } = "";
    public string ObfuscatedSource { get; set; } = "";
    public string CountLabel { get; set; } = "";
    public string Tone { get; set; } = "low";
}
