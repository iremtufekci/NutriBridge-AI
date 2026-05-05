using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.API.Controllers;

[ApiController]
[Route("api/Client/pdf-analyses")]
[Authorize(Roles = "Client")]
public class ClientPdfAnalysisController(IClientPdfAnalysisService pdfAnalysisService) : ControllerBase
{
    private const long MaxBytes = 10 * 1024 * 1024;

    /// <summary>PDF yukler, Gemini ile analiz eder, MongoDB'ye kaydeder.</summary>
    [HttpPost("upload")]
    [Consumes("multipart/form-data")]
    [RequestSizeLimit(MaxBytes)]
    public async Task<IActionResult> Upload(IFormFile? pdf, CancellationToken cancellationToken)
    {
        if (pdf is null || pdf.Length == 0)
            throw new AppException("PDF dosyasi gonderilmedi.");

        if (pdf.Length > MaxBytes)
            throw new AppException("PDF boyutu en fazla 10 MB olabilir.");

        var ct = (pdf.ContentType ?? string.Empty).Trim().ToLowerInvariant();
        var ext = Path.GetExtension(pdf.FileName ?? string.Empty);
        var okByType = ct is "application/pdf" or "application/x-pdf";
        var okByExt = string.Equals(ext, ".pdf", StringComparison.OrdinalIgnoreCase);
        if (!okByType && !okByExt)
            throw new AppException("Sadece PDF dosyasi yukleyebilirsiniz.");

        var clientId = User.FindFirstValue("UserId") ?? User.FindFirstValue(ClaimTypes.NameIdentifier) ?? string.Empty;
        if (string.IsNullOrWhiteSpace(clientId))
            throw new AppException("Oturum bulunamadi. Tekrar giris yapin.");

        await using var stream = pdf.OpenReadStream();
        var dto = await pdfAnalysisService.UploadAnalyzeAndPersistAsync(clientId, stream, pdf.FileName ?? "belge.pdf", cancellationToken);
        return Ok(dto);
    }

    [HttpGet]
    public async Task<IActionResult> List([FromQuery] int take = 50, CancellationToken cancellationToken = default)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await pdfAnalysisService.GetMyAnalysesAsync(clientId, take, cancellationToken));
    }
}
