namespace Nightbrate.Application.DTOs;

public class AdminDietitianDetailDto
{
    public string Id { get; set; } = string.Empty;
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public string DiplomaNo { get; set; } = string.Empty;
    public string ClinicName { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public bool IsApproved { get; set; }
    public string? DiplomaDocumentUrl { get; set; }
}
