using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.Application.Services;

public sealed class MockPdfAnalysisService : IPdfAnalysisAiService
{
    public Task<ClientPdfAnalysisResultDto> AnalyzePdfAsync(
        byte[] pdfBytes,
        string originalFileName,
        CancellationToken cancellationToken = default)
    {
        _ = pdfBytes;
        _ = originalFileName;
        return Task.FromResult(
            new ClientPdfAnalysisResultDto
            {
                AnalysisSource = "mock",
                DocumentType = "Deneme / API yapilandirmasi",
                Summary =
                    "Gemini API anahtari yapilandirilmamis; bu sabit metin gosteriliyor. "
                    + "Google AI Studio anahtarini appsettings.json icindeki Gemini:ApiKey alanına eklediginizde "
                    + "PDF icerigi yapay zeka ile ozetlenir. Bu cikti tibbi tavsiye degildir.",
                KeyFindings =
                [
                    "Ornek: PDF yuklemesi ve kayit akisi calisiyor.",
                    "Gercek analiz icin sunucuda Gemini anahtari gerekli."
                ],
                Cautions =
                [
                    "AI ozetleri hata yapabilir; kritik kararlarda mutlaka doktorunuzla paylasin.",
                    "Tibbi tani veya tedavi icin yalnizca bu sonuca guvenmeyin."
                ],
                SuggestedForDietitian =
                [
                    "Ornek: \"Gercek analiz sonrasinda diyetisyeninizle paylasilacak laboratuvar / rapor PDF'i.\""
                ]
            });
    }
}
