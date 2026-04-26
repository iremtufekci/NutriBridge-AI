namespace Nightbrate.Application.Interfaces;

public interface ISystemResourceProvider
{
    /// <summary>İşlemci (API süreci) tahmini kullanım %, bellek, disk doluluk, ağ hızı türevi.</summary>
    SystemResourceSample GetSample();
}

public record SystemResourceSample(
    double CpuPercent,
    double MemoryPercent,
    string MemoryRefLabel,
    double DiskActivityPercent,
    string DiskRefLabel,
    double NetworkTotalMbps,
    double NetworkUpMbps,
    double NetworkDownMbps,
    string NetworkNote);
