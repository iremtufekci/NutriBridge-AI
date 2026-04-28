using System.Globalization;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Utils;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Services;

public class DietitianDailyTaskService(
    IClientRepository clientRepository,
    IMealLogRepository mealLogRepository,
    IDietProgramRepository dietProgramRepository,
    ICriticalAlertService criticalAlertService,
    IDietitianDailyTaskRepository taskRepository) : IDietitianDailyTaskService
{
    public async Task<DietitianTodayTasksBundleDto> SyncAndGetTodayAsync(string dietitianId, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(dietitianId))
            return EmptyBundle();

        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        var taskDate = today.ToString(ProgramDateHelper.JsonFormat, CultureInfo.InvariantCulture);

        var desired = await BuildDesiredSystemTasksAsync(dietitianId, taskDate, cancellationToken).ConfigureAwait(false);
        var desiredKeys = desired.Select(d => d.TaskKey).ToHashSet(StringComparer.Ordinal);

        var existing = (await taskRepository
                .GetByDietitianAndTaskDateAsync(dietitianId, taskDate, cancellationToken)
                .ConfigureAwait(false))
            .ToList();

        var obsoleteIds = existing
            .Where(e => e.IsSystemGenerated && !e.IsCompleted && !desiredKeys.Contains(e.TaskKey))
            .Select(e => e.Id!)
            .Where(id => !string.IsNullOrEmpty(id))
            .ToList();

        if (obsoleteIds.Count > 0)
            await taskRepository.DeleteManyByIdsAsync(obsoleteIds, cancellationToken).ConfigureAwait(false);

        var survivingByKey = existing
            .Where(e => e.Id is not null && !obsoleteIds.Contains(e.Id))
            .ToDictionary(e => e.TaskKey, StringComparer.Ordinal);

        var now = DateTime.UtcNow;
        foreach (var spec in desired)
        {
            if (survivingByKey.TryGetValue(spec.TaskKey, out var row))
            {
                if (row.IsCompleted)
                    continue;
                if (!string.Equals(row.Title, spec.Title, StringComparison.Ordinal)
                    || !string.Equals(row.Subtitle, spec.Subtitle, StringComparison.Ordinal)
                    || row.SortPriority != spec.SortPriority)
                {
                    await taskRepository
                        .UpdateContentAsync(row.Id!, spec.Title, spec.Subtitle, spec.SortPriority, now, cancellationToken)
                        .ConfigureAwait(false);
                }
            }
            else
            {
                await taskRepository.InsertAsync(
                        new DietitianDailyTask
                        {
                            DietitianId = dietitianId,
                            TaskDate = taskDate,
                            TaskKey = spec.TaskKey,
                            Title = spec.Title,
                            Subtitle = spec.Subtitle,
                            Category = spec.Category,
                            ClientId = spec.ClientId,
                            IsSystemGenerated = true,
                            SortPriority = spec.SortPriority,
                            CreatedAtUtc = now,
                            UpdatedAtUtc = now
                        },
                        cancellationToken)
                    .ConfigureAwait(false);
            }
        }

        var finalRows = (await taskRepository
                .GetByDietitianAndTaskDateAsync(dietitianId, taskDate, cancellationToken)
                .ConfigureAwait(false))
            .ToList();

        return ToBundle(taskDate, finalRows);
    }

    public async Task SetTaskCompletedAsync(
        string dietitianId,
        string taskId,
        bool isCompleted,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(dietitianId)) throw new AppException("Oturum bulunamadi.");
        if (string.IsNullOrWhiteSpace(taskId)) throw new AppException("Gorev secin.");

        var row = await taskRepository.GetByIdAsync(taskId, cancellationToken).ConfigureAwait(false);
        if (row is null || row.DietitianId != dietitianId)
            throw new AppException("Gorev bulunamadi.");

        var now = DateTime.UtcNow;
        await taskRepository
            .UpdateCompletionAsync(taskId, isCompleted, isCompleted ? now : null, now, cancellationToken)
            .ConfigureAwait(false);
    }

    private async Task<List<TaskSpec>> BuildDesiredSystemTasksAsync(
        string dietitianId,
        string taskDateYmd,
        CancellationToken cancellationToken)
    {
        var list = new List<TaskSpec>();
        var clients = await clientRepository.GetByDietitianIdAsync(dietitianId).ConfigureAwait(false);

        var alerts = await criticalAlertService.GetCriticalAlertsAsync(dietitianId, cancellationToken).ConfigureAwait(false);
        foreach (var a in alerts)
        {
            var key = $"sys:crit:{a.ClientId}:{a.AlertType}:{a.ReferenceDate}";
            list.Add(
                new TaskSpec(
                    key,
                    $"{a.ClientName} — Kritik uyarı",
                    a.Message,
                    "Critical",
                    a.ClientId,
                    0));
        }

        if (clients.Count == 0) return list;

        var clientIds = clients.Where(c => !string.IsNullOrEmpty(c.Id)).Select(c => c.Id!).ToList();
        var dayStart = ProgramDateHelper.UtcStartOfYmdString(taskDateYmd);
        var dayEnd = dayStart.AddDays(1);

        var meals = await mealLogRepository
            .GetByClientIdsInTimestampRangeAsync(clientIds, dayStart, dayEnd, cancellationToken)
            .ConfigureAwait(false);
        var hasMealToday = new HashSet<string>(meals.Where(m => !string.IsNullOrEmpty(m.ClientId)).Select(m => m.ClientId!), StringComparer.Ordinal);

        foreach (var c in clients)
        {
            if (string.IsNullOrEmpty(c.Id)) continue;
            var name = $"{c.FirstName} {c.LastName}".Trim();
            if (name.Length == 0) name = c.Email;
            if (name.Length == 0) name = "Danışan";

            if (!hasMealToday.Contains(c.Id))
            {
                var key = $"sys:meal:{c.Id}:{taskDateYmd}";
                list.Add(
                    new TaskSpec(
                        key,
                        $"{name} — Öğün kaydı",
                        "Bugün için henüz öğün fotoğrafı / kaydı yok; kontrol edin.",
                        "MealLog",
                        c.Id,
                        2));
            }

            var program = await dietProgramRepository
                .GetCurrentByClientIdAndProgramDateAsync(c.Id, taskDateYmd)
                .ConfigureAwait(false);
            if (program is not null)
            {
                var key = $"sys:prog:{c.Id}:{taskDateYmd}";
                list.Add(
                    new TaskSpec(
                        key,
                        $"{name} — Günlük program",
                        "Öğün tamamlanma ve plana uyumu gözden geçirin.",
                        "ProgramReview",
                        c.Id,
                        1));
            }
        }

        return list;
    }

    private static DietitianTodayTasksBundleDto ToBundle(string taskDateYmd, IReadOnlyList<DietitianDailyTask> rows)
    {
        var pending = rows.Count(x => !x.IsCompleted);
        var done = rows.Count(x => x.IsCompleted);
        var dtos = rows
            .OrderBy(x => x.IsCompleted ? 1 : 0)
            .ThenBy(x => x.SortPriority)
            .ThenBy(x => x.Title)
            .Select(
                x => new DietitianDailyTaskItemDto
                {
                    Id = x.Id ?? string.Empty,
                    TaskKey = x.TaskKey,
                    Title = x.Title,
                    Subtitle = x.Subtitle,
                    Category = x.Category,
                    ClientId = x.ClientId,
                    IsCompleted = x.IsCompleted,
                    DueLabel = "Bugün"
                })
            .ToList();

        return new DietitianTodayTasksBundleDto
        {
            TaskDate = taskDateYmd,
            PendingCount = pending,
            CompletedCount = done,
            TotalCount = rows.Count,
            Tasks = dtos
        };
    }

    private static DietitianTodayTasksBundleDto EmptyBundle() =>
        new()
        {
            TaskDate = DateOnly.FromDateTime(DateTime.UtcNow)
                .ToString(ProgramDateHelper.JsonFormat, CultureInfo.InvariantCulture),
            Tasks = Array.Empty<DietitianDailyTaskItemDto>()
        };

    private sealed record TaskSpec(
        string TaskKey,
        string Title,
        string Subtitle,
        string Category,
        string? ClientId,
        int SortPriority);
}
