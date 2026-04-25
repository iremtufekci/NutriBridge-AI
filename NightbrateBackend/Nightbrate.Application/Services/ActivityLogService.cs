using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Services;

public class ActivityLogService(IActivityLogRepository repository) : IActivityLogService
{
    public async Task LogAsync(string? userId, string actorDisplayName, string description)
    {
        if (string.IsNullOrWhiteSpace(description)) return;
        var name = (actorDisplayName ?? "Kullanici").Trim();
        if (name.Length == 0) name = "Kullanici";

        await repository.AddAsync(new ActivityLog
        {
            UserId = userId,
            ActorDisplayName = name,
            Description = description.Trim(),
            CreatedAt = DateTime.UtcNow
        });
    }

    public async Task<IReadOnlyList<ActivityItemDto>> GetRecentAsync(int take)
    {
        var list = await repository.GetRecentAsync(take);
        return list.Select(Map).ToList();
    }

    private static ActivityItemDto Map(ActivityLog x)
    {
        var name = x.ActorDisplayName ?? "";
        return new ActivityItemDto
        {
            Id = x.Id ?? string.Empty,
            Initial = InitialLetter(name),
            ActorDisplayName = name,
            Description = x.Description,
            CreatedAt = x.CreatedAt
        };
    }

    private static string InitialLetter(string displayName)
    {
        var s = displayName.Trim();
        if (s.Length == 0) return "?";
        foreach (var ch in s)
        {
            if (char.IsLetter(ch)) return ch.ToString().ToUpperInvariant();
        }
        return s[0].ToString().ToUpperInvariant();
    }
}
