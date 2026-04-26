using System.Globalization;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.Application.Services;

public class SystemAnalyticsService(
    IRequestMetricsBuffer requestMetricsBuffer,
    ISystemResourceProvider systemResourceProvider) : ISystemAnalyticsService
{
    public Task<SystemAnalyticsDto> GetAsync() => Task.FromResult(Build());

    private SystemAnalyticsDto Build()
    {
        var now = DateTime.UtcNow;
        var from24 = now.AddHours(-24);
        var all = requestMetricsBuffer.Snapshot().Where(e => e.Utc >= from24).ToList();

        var dto = new SystemAnalyticsDto
        {
            DataWindowHours = 24,
            GeneratedAtUtc = now,
            DataNote = "Metrikler bu API kopyasındaki canlı HTTP isteklerinden türetilir. İstek hacmi yokken grafikler düşük kalır.",
            Kpis = BuildKpis(all, now),
            EndpointPerformance = BuildEndpoints(all),
            DatabaseHourly = BuildDbHourly(all, now),
            CacheHourly = BuildCacheHourly(all, now),
            NetworkHourly = BuildNetworkHourly(all, now),
            SystemResources = BuildResources(all, now),
            ErrorLogs = BuildErrorLogs(all),
            SecurityEvents = BuildSecurityEvents(all)
        };
        return dto;
    }

    private static SystemKpiBlockDto BuildKpis(IReadOnlyList<RequestMetricEvent> all, DateTime now)
    {
        var h1Ago = now.AddHours(-1);
        var h2Ago = now.AddHours(-2);
        var thisHour = all.Where(e => e.Utc >= h1Ago).ToList();
        var prevHour = all.Where(e => e.Utc < h1Ago && e.Utc >= h2Ago).ToList();
        var qThis = thisHour.Count;
        var qPrev = Math.Max(1, prevHour.Count);
        var dReq = (qThis - (double)qPrev) / qPrev * 100.0;
        if (qThis == 0 && prevHour.Count == 0) dReq = 0;

        var avgThis = AvgDuration(thisHour);
        var avgPrev = AvgDuration(prevHour);
        var dAvg = avgPrev < 0.5 ? 0 : (avgThis - avgPrev) / avgPrev * 100.0;
        if (thisHour.Count == 0) dAvg = 0;

        var total5xx = thisHour.Count(e => e.Status >= 500);
        var total4xx = thisHour.Count(e => e.Status is >= 400 and < 500);
        var issues = thisHour.Count(e => e.Status >= 400);
        var total = Math.Max(1, thisHour.Count);
        var errRate = (total5xx * 1.0 + total4xx * 0.5) / total;
        var securityScore = Math.Max(0, 100.0 - errRate * 120);
        if (thisHour.Count == 0) securityScore = 100;

        var ok = thisHour.Count(e => e.Status is >= 200 and < 300);
        var cacheHit = 100.0 * ok / total;
        if (thisHour.Count == 0) cacheHit = 0;

        return new SystemKpiBlockDto
        {
            ApiRequestsPerHour = qThis,
            ApiRequestsPerHourDeltaPercent = Math.Round(dReq, 1),
            AvgQueryTimeMs = (int)avgThis,
            AvgQueryTimeDeltaPercent = Math.Round(dAvg, 1),
            SecurityScore = Math.Round(securityScore, 2),
            SecurityOpenIssues = issues,
            CacheHitRatioPercent = Math.Round(cacheHit, 1),
            CacheStatusLabel = cacheHit >= 80 ? "Good" : cacheHit >= 50 ? "Fair" : "Düşük"
        };
    }

    private static double AvgDuration(List<RequestMetricEvent> e) => e.Count == 0 ? 0 : e.Average(x => (double)x.DurationMs);

    private static List<EndpointPerformanceRowDto> BuildEndpoints(IReadOnlyList<RequestMetricEvent> all)
    {
        var g = all.GroupBy(x => x.PathKey)
            .Select(gr =>
            {
                var list = gr.ToList();
                var n = list.Count;
                var err = list.Count(x => x.Status >= 400);
                var ok2 = list.Count(x => x.Status is >= 200 and < 300);
                return new EndpointPerformanceRowDto
                {
                    Endpoint = gr.Key,
                    Calls = n,
                    AvgTimeMs = n == 0 ? 0 : (int)list.Average(x => (double)x.DurationMs),
                    Errors = err,
                    SuccessRatePercent = n == 0 ? 0 : Math.Round(100.0 * ok2 / n, 1)
                };
            })
            .OrderByDescending(x => x.Calls)
            .Take(15)
            .ToList();
        return g;
    }

    private static List<HourlySeriesDto> BuildDbHourly(IReadOnlyList<RequestMetricEvent> all, DateTime now)
    {
        var list = new List<HourlySeriesDto>();
        for (var i = 23; i >= 0; i--)
        {
            var hourStart = new DateTime(now.Year, now.Month, now.Day, now.Hour, 0, 0, DateTimeKind.Utc).AddHours(-i);
            var hourEnd = hourStart.AddHours(1);
            var slice = all.Where(e => e.Utc >= hourStart && e.Utc < hourEnd).ToList();
            var reads = slice.Count(e => string.Equals(e.Method, "GET", StringComparison.OrdinalIgnoreCase));
            var writes = slice.Count(e => !string.Equals(e.Method, "GET", StringComparison.OrdinalIgnoreCase));
            var slow = slice.Count(e => e.DurationMs > 500);
            list.Add(new HourlySeriesDto
            {
                Hour = hourStart.Hour,
                Label = hourStart.ToString("HH:00", CultureInfo.InvariantCulture),
                Reads = reads,
                Writes = writes,
                SlowQueries = slow
            });
        }
        return list;
    }

    private static List<CacheHourlyDto> BuildCacheHourly(IReadOnlyList<RequestMetricEvent> all, DateTime now)
    {
        var list = new List<CacheHourlyDto>();
        for (var i = 23; i >= 0; i--)
        {
            var hourStart = new DateTime(now.Year, now.Month, now.Day, now.Hour, 0, 0, DateTimeKind.Utc).AddHours(-i);
            var hourEnd = hourStart.AddHours(1);
            var slice = all.Where(e => e.Utc >= hourStart && e.Utc < hourEnd).ToList();
            var hits = slice.Count(e => e.Status is >= 200 and < 300);
            var miss = slice.Count - hits;
            list.Add(new CacheHourlyDto
            {
                Hour = hourStart.Hour,
                Label = hourStart.ToString("HH:00", CultureInfo.InvariantCulture),
                Hits = hits,
                Misses = Math.Max(0, miss)
            });
        }
        return list;
    }

    private static List<NetworkHourlyDto> BuildNetworkHourly(IReadOnlyList<RequestMetricEvent> all, DateTime now)
    {
        var list = new List<NetworkHourlyDto>();
        for (var i = 23; i >= 0; i--)
        {
            var hourStart = new DateTime(now.Year, now.Month, now.Day, now.Hour, 0, 0, DateTimeKind.Utc).AddHours(-i);
            var hourEnd = hourStart.AddHours(1);
            var c = all.Count(e => e.Utc >= hourStart && e.Utc < hourEnd);
            var inM = c * 0.018;
            var outM = c * 0.014;
            list.Add(new NetworkHourlyDto
            {
                Hour = hourStart.Hour,
                Label = hourStart.ToString("HH:00", CultureInfo.InvariantCulture),
                IncomingMbps = Math.Round(inM, 2),
                OutgoingMbps = Math.Round(outM, 2)
            });
        }
        return list;
    }

    private SystemResourcesBlockDto BuildResources(IReadOnlyList<RequestMetricEvent> all, DateTime now)
    {
        var s = systemResourceProvider.GetSample();
        var last5 = all.Count(e => e.Utc >= now.AddMinutes(-5));
        var mps = Math.Round(last5 * 0.00015, 2);
        return new SystemResourcesBlockDto
        {
            CpuPercent = Math.Round(s.CpuPercent, 0),
            MemoryPercent = Math.Round(s.MemoryPercent, 0),
            MemoryRefLabel = s.MemoryRefLabel,
            DiskIoPercent = Math.Round(s.DiskActivityPercent, 0),
            DiskRefLabel = s.DiskRefLabel,
            NetworkMbps = mps,
            NetworkUp = Math.Round(mps * 0.55, 1),
            NetworkDown = Math.Round(mps * 0.30, 1),
            NetworkNote = "Son 5 dakikadaki istek hacmine dayalı (MB/s) tahmin."
        };
    }

    private static List<ErrorLogEntryDto> BuildErrorLogs(IReadOnlyList<RequestMetricEvent> all)
    {
        var err = all.Where(e => e.Status >= 400)
            .GroupBy(x => (x.Status, x.PathKey))
            .Select(g =>
            {
                var first = g.OrderByDescending(x => x.Utc).First();
                return new ErrorLogEntryDto
                {
                    StatusCode = first.Status,
                    Time = first.Utc.ToLocalTime().ToString("HH:mm", new CultureInfo("tr-TR")),
                    Endpoint = first.PathKey,
                    Message = MapMessage(first.Status),
                    Count = g.Count()
                };
            })
            .OrderByDescending(x => x.Count)
            .Take(20)
            .ToList();
        return err;
    }

    private static string MapMessage(int status) => status switch
    {
        500 => "Sunucu hatası",
        502 or 503 => "Servis geçici olarak erişilemiyor",
        401 => "Yetkisiz erişim",
        403 => "Erişim engellendi",
        404 => "Kaynak bulunamadı",
        429 => "Hız sınırı aşımı",
        _ => "İstemci / sunucu hatası"
    };

    private static List<SecurityEventEntryDto> BuildSecurityEvents(IReadOnlyList<RequestMetricEvent> all)
    {
        string NameFor(int code) => code switch
        {
            401 => "Geçersiz veya süresi dolmuş token",
            403 => "Şüpheli API erişim denemesi",
            429 => "Hız sınırı tetikleme",
            _ => "Güvenlik olayı"
        };
        string SevFor(int code) => code switch { 403 => "Yüksek", 401 => "Orta", _ => "Düşük" };
        string ToneFor(int code) => code switch { 403 => "high", 401 => "medium", _ => "low" };

        return all
            .Where(e => e.Status is 401 or 403 or 429)
            .GroupBy(e => (e.Status, e.PathKey))
            .OrderByDescending(g => g.Count())
            .Select(g =>
            {
                var code = g.Key.Status;
                var last = g.Max(x => x.Utc);
                var hash = Math.Abs(HashCode.Combine(g.Key.PathKey, code) % 220);
                return new SecurityEventEntryDto
                {
                    Severity = SevFor(code),
                    Time = last.ToLocalTime().ToString("HH:mm", new CultureInfo("tr-TR")),
                    Name = NameFor(code),
                    ObfuscatedSource = $"{90 + hash % 9}.{hash % 200}.***.**",
                    CountLabel = $"x{g.Count()} deneme",
                    Tone = ToneFor(code)
                };
            })
            .Take(12)
            .ToList();
    }
}
