namespace Nightbrate.Application.DTOs;

public class ConnectToDietitianRequestDto
{
    public string ConnectionCode { get; set; } = string.Empty;
}

public class ConnectToDietitianResultDto
{
    public string Message { get; set; } = string.Empty;
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
}

public class PreviewDietitianByCodeResultDto
{
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public string DisplayName { get; set; } = string.Empty;
}
