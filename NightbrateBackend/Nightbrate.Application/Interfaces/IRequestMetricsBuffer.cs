namespace Nightbrate.Application.Interfaces;

public record RequestMetricEvent(
    DateTime Utc,
    string PathKey,
    string Method,
    int Status,
    long DurationMs);

public interface IRequestMetricsBuffer
{
    void Record(in RequestMetricEvent e);
    IReadOnlyList<RequestMetricEvent> Snapshot();
}
