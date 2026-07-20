namespace Nightbrate.Application.Interfaces;

public sealed class DiplomaDocumentSaveResult
{
    public string RelativePublicUrl { get; set; } = string.Empty;
}

public interface IDiplomaDocumentStorage
{
    Task<DiplomaDocumentSaveResult> SaveAsync(
        Stream fileStream,
        string originalFileName,
        CancellationToken cancellationToken = default);
}
