using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface ICriticalAlertAcknowledgmentRepository
{
    Task<IReadOnlyList<CriticalAlertAcknowledgment>> GetByDietitianIdAsync(string dietitianId, CancellationToken cancellationToken = default);
    Task AddAsync(CriticalAlertAcknowledgment doc, CancellationToken cancellationToken = default);
}
