using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class MealController(IMealPhotoAnalysisService mealPhotoAnalysisService) : ControllerBase
{
    private const long MaxBytes = 5 * 1024 * 1024;
    private static readonly HashSet<string> AllowedExt = new(StringComparer.OrdinalIgnoreCase) { ".jpg", ".jpeg", ".png" };

    /// <summary>Fotografi kaydeder, mock AI analizi calistirir, MealLog olarak MongoDB'ye yazar.</summary>
    [HttpPost("upload-meal-photo")]
    [Authorize(Roles = "Client")]
    [Consumes("multipart/form-data")]
    [RequestSizeLimit(MaxBytes)]
    public async Task<IActionResult> UploadMealPhoto(IFormFile? photo, CancellationToken cancellationToken)
    {
        if (photo is null || photo.Length == 0)
            throw new AppException("Fotograf dosyasi gonderilmedi.");

        if (photo.Length > MaxBytes)
            throw new AppException("Dosya boyutu en fazla 5 MB olabilir.");

        var ext = ResolveImageExtension(photo);
        if (string.IsNullOrEmpty(ext))
            throw new AppException("Sadece .jpg, .jpeg ve .png formatlari kabul edilir.");

        var clientId = User.FindFirstValue("UserId") ?? User.FindFirstValue(ClaimTypes.NameIdentifier) ?? string.Empty;
        if (string.IsNullOrWhiteSpace(clientId))
            throw new AppException("Oturum bilgisi bulunamadi. Tekrar giris yapin.");

        await using var stream = photo.OpenReadStream();
        var dto = await mealPhotoAnalysisService.UploadAnalyzeAndPersistAsync(clientId, stream, ext, cancellationToken);
        return Ok(dto);
    }

    /// <summary>
    /// Mobil/kamera: dosya adi uzantisiz gelebilir; bu durumda ContentType uzerinden .jpg / .png kabul edilir.
    /// </summary>
    private static string ResolveImageExtension(IFormFile photo)
    {
        var fromName = Path.GetExtension(photo.FileName);
        if (!string.IsNullOrEmpty(fromName) && AllowedExt.Contains(fromName))
            return fromName;

        var ct = (photo.ContentType ?? string.Empty).Trim().ToLowerInvariant();
        return ct switch
        {
            "image/jpeg" or "image/jpg" or "image/pjpeg" => ".jpg",
            "image/png" or "image/x-png" => ".png",
            _ => string.Empty
        };
    }
}
