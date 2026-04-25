namespace Nightbrate.Application.DTOs;

public class ActivityItemDto
{
    public string Id { get; set; } = string.Empty;
    public string Initial { get; set; } = "?";
    public string ActorDisplayName { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
}
