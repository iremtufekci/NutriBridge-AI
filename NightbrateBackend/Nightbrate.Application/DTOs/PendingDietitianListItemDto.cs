namespace Nightbrate.Application.DTOs;

/// <summary>Admin listesinde sifre/ozel alan gostermemek icin (IsApproved = false)</summary>
public class PendingDietitianListItemDto
{
    public string Id { get; set; } = string.Empty;
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string DiplomaNo { get; set; } = string.Empty;
    public string ClinicName { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public bool IsApproved { get; set; }
}
