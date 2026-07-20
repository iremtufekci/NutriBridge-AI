using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.API.Controllers;

/// <summary>Danışan PDF yükleme ve geçmiş analiz listesi</summary>
[ApiController]
[Route("api/Client/pdf-analyses")] // Tam yol (ClientController altında değil)
[Authorize(Roles = "Client")]
public class ClientPdfAnalysisController(IClientPdfAnalysisService pdfAnalysisService) : ControllerBase
{
    private const long MaxBytes = 10 * 1024 * 1024; // Max 10 MB PDF

    [HttpPost("upload")] // POST api/Client/pdf-analyses/upload
    [Consumes("multipart/form-data")]
    [RequestSizeLimit(MaxBytes)]
    public async Task<IActionResult> Upload(IFormFile? pdf, CancellationToken cancellationToken)
    {
        if (pdf is null || pdf.Length == 0) // Dosya yok
            throw new AppException("PDF dosyasi gonderilmedi.");

        if (pdf.Length > MaxBytes) // Boyut limiti
            throw new AppException("PDF boyutu en fazla 10 MB olabilir.");

        var ct = (pdf.ContentType ?? string.Empty).Trim().ToLowerInvariant();
        var ext = Path.GetExtension(pdf.FileName ?? string.Empty);
        var okByType = ct is "application/pdf" or "application/x-pdf"; // Sadece PDF MIME
        var okByExt = string.Equals(ext, ".pdf", StringComparison.OrdinalIgnoreCase);
        if (!okByType && !okByExt)
            throw new AppException("Sadece PDF dosyasi yukleyebilirsiniz.");

        var clientId = User.FindFirstValue("UserId") ?? User.FindFirstValue(ClaimTypes.NameIdentifier) ?? string.Empty;
        if (string.IsNullOrWhiteSpace(clientId))
            throw new AppException("Oturum bulunamadi. Tekrar giris yapin.");

        await using var stream = pdf.OpenReadStream();
        var dto = await pdfAnalysisService.UploadAnalyzeAndPersistAsync(clientId, stream, pdf.FileName ?? "belge.pdf", cancellationToken); // Disk + Gemini + Mongo
        return Ok(dto); // Analiz özeti + tam metin
    }

    [HttpGet] // GET api/Client/pdf-analyses?take=50
    public async Task<IActionResult> List([FromQuery] int take = 50, CancellationToken cancellationToken = default)
    {
        var clientId = User.FindFirstValue("UserId") ?? string.Empty;
        return Ok(await pdfAnalysisService.GetMyAnalysesAsync(clientId, take, cancellationToken)); // Geçmiş liste
    }
}
