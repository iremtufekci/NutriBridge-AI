using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface ICriticalAlertService
{
    Task<IReadOnlyList<CriticalAlertDto>> GetCriticalAlertsAsync(string dietitianId, CancellationToken cancellationToken = default);
    Task AcknowledgeAsync(string dietitianId, AckCriticalAlertDto dto, CancellationToken cancellationToken = default);
}
