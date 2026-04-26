using System.Diagnostics;
using System.Runtime.InteropServices;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.Infrastructure.Monitoring;

public sealed class SystemResourceProvider : ISystemResourceProvider
{
    private TimeSpan _prevCpu;
    private long _prevWallMs;
    private readonly object _cpuLock = new();

    public SystemResourceSample GetSample()
    {
        var cpu = TryCpuPercent();
        var (memPct, memLabel) = MemoryInfo();
        var (diskPct, diskLabel) = TryDisk();
        // Ağ: HTTP isteği toplamına göre servis tarafında hesaplanır; burada 0 bırakılır
        return new SystemResourceSample(
            cpu, memPct, memLabel, diskPct, diskLabel, 0, 0, 0,
            "HTTP trafiğine dayalı ayrıntı aşağıdaki grafiğe yansıtılmıştır.");
    }

    private double TryCpuPercent()
    {
        try
        {
            var p = Process.GetCurrentProcess();
            lock (_cpuLock)
            {
                p.Refresh();
                var wall = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                if (_prevWallMs == 0)
                {
                    _prevWallMs = wall;
                    _prevCpu = p.TotalProcessorTime;
                    return 0;
                }
                var cpuNow = p.TotalProcessorTime;
                var wallDelta = (wall - _prevWallMs) / 1000.0;
                if (wallDelta < 0.05) return 0;
                var cpuDelta = (cpuNow - _prevCpu).TotalSeconds;
                _prevWallMs = wall;
                _prevCpu = cpuNow;
                var n = Math.Max(1, Environment.ProcessorCount);
                var pct = 100.0 * cpuDelta / (n * wallDelta);
                if (double.IsNaN(pct) || double.IsInfinity(pct)) return 0;
                return Math.Clamp(pct, 0, 100);
            }
        }
        catch
        {
            return 0;
        }
    }

    private static (double pct, string label) MemoryInfo()
    {
        try
        {
            var p = Process.GetCurrentProcess();
            p.Refresh();
            var ws = p.WorkingSet64;
            const double capGb = 16;
            var capBytes = (long)(capGb * 1024 * 1024 * 1024);
            var pct = 100.0 * ws / capBytes;
            var gb = ws / (1024.0 * 1024 * 1024);
            return (Math.Min(100, Math.Round(pct, 1)), $"{gb:0.##} / {capGb:0} GB");
        }
        catch
        {
            return (0, "—");
        }
    }

    private static (double pct, string label) TryDisk()
    {
        try
        {
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
            {
                foreach (var di in System.IO.DriveInfo.GetDrives().Where(d => d.IsReady && d.DriveType == System.IO.DriveType.Fixed))
                {
                    var t = di.TotalSize;
                    if (t <= 0) continue;
                    var u = 100.0 * (t - di.AvailableFreeSpace) / t;
                    return (Math.Min(100, Math.Round(u, 0)), di.Name);
                }
            }
            else
            {
                var di = new System.IO.DriveInfo("/");
                if (di.IsReady && di.TotalSize > 0)
                {
                    var u = 100.0 * (di.TotalSize - di.AvailableFreeSpace) / di.TotalSize;
                    return (Math.Min(100, Math.Round(u, 0)), "disk");
                }
            }
        }
        catch
        {
            // ignore
        }
        return (0, "—");
    }
}
