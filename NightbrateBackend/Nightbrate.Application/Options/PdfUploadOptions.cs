namespace Nightbrate.Application.Options;

public class PdfUploadOptions
{
    public string PdfsDirectory { get; set; } = string.Empty;
    public string PublicRelativePath { get; set; } = "/uploads/pdfs";

    /// <summary>Gemini inline PDF icin ham bayt üst siniri (asiminda istemci reddedilir).</summary>
    public int MaxPdfBytes { get; set; } = 7 * 1024 * 1024;
}
