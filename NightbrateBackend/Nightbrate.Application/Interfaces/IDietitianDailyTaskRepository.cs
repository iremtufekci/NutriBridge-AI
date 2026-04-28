using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IDietitianDailyTaskRepository
{
    Task<IReadOnlyList<DietitianDailyTask>> GetByDietitianAndTaskDateAsync(
        string dietitianId,
        string taskDateYmd,
        CancellationToken cancellationToken = default);

    Task<DietitianDailyTask?> GetByIdAsync(string id, CancellationToken cancellationToken = default);

    Task InsertAsync(DietitianDailyTask task, CancellationToken cancellationToken = default);

    Task UpdateContentAsync(
        string id,
        string title,
        string subtitle,
        int sortPriority,
        DateTime updatedAtUtc,
        CancellationToken cancellationToken = default);

    Task UpdateCompletionAsync(
        string id,
        bool isCompleted,
        DateTime? completedAtUtc,
        DateTime updatedAtUtc,
        CancellationToken cancellationToken = default);

    Task DeleteManyByIdsAsync(IReadOnlyCollection<string> ids, CancellationToken cancellationToken = default);
}
