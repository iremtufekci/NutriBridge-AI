using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface IDietitianDailyTaskService
{
    Task<DietitianTodayTasksBundleDto> SyncAndGetTodayAsync(string dietitianId, CancellationToken cancellationToken = default);

    Task SetTaskCompletedAsync(
        string dietitianId,
        string taskId,
        bool isCompleted,
        CancellationToken cancellationToken = default);
}
