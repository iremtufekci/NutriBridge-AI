namespace Nightbrate.Application.Options;

public sealed class DiplomaUploadOptions
{
    public string DiplomasDirectory { get; set; } = string.Empty;
    public string PublicRelativePath { get; set; } = "/uploads/diplomas";
    public int MaxBytes { get; set; } = 10 * 1024 * 1024;
}
