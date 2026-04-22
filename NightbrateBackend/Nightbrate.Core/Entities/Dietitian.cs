namespace Nightbrate.Core.Entities;

public class Dietitian : BaseUser
{
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public string DiplomaNo { get; set; } = string.Empty;
    public string ClinicName { get; set; } = string.Empty;
    public string? DiplomaDocumentUrl { get; set; }
    public bool IsApproved { get; set; } = false;
}