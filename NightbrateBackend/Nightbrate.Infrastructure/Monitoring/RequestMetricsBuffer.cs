using Nightbrate.Application.Interfaces;

namespace Nightbrate.Infrastructure.Monitoring;

/// <summary>API isteklerini bellekte tutar; pencere ~25 saat / üst sınır kayıt.</summary>
public sealed class RequestMetricsBuffer : IRequestMetricsBuffer
{
    private readonly object _lock = new();
    private readonly List<RequestMetricEvent> _events = new();
    private const int MaxEvents = 200_000;
    private static readonly TimeSpan Retention = TimeSpan.FromHours(25);

    public void Record(in RequestMetricEvent e)
    {
        lock (_lock)
        {
            _events.Add(e);
            TrimLocked();
        }
    }

    public IReadOnlyList<RequestMetricEvent> Snapshot()
    {
        lock (_lock)
        {
            TrimLocked();
            return _events.ToArray();
        }
    }

    private void TrimLocked()
    {
        var cutoff = DateTime.UtcNow - Retention;
        var remove = 0;
        for (var i = 0; i < _events.Count; i++)
        {
            if (_events[i].Utc < cutoff) remove++;
            else break;
        }
        if (remove > 0) _events.RemoveRange(0, remove);
        while (_events.Count > MaxEvents) _events.RemoveAt(0);
    }
}
