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
                DocumentType = "Ornek laboratuvar sonuclari",
                Summary = "Ornek laboratuvar sonuclari (Gemini anahtari yapilandirilmamis)",
                KeyFindings = [],
                Cautions = [],
                SuggestedForDietitian =
                [
                    "Aksam kan sekeri normal sinirlar icinde olsa da, HbA1c degerinin prediyabet araligina yakinligi goz onune alindiginda karbonhidrat alimi ve ogun planlamasi konusunda danismanlik verilebilir; bu durum kilo vermeyi zorlastirabilir.",
                    "HDL kolesterol degeri normal sinirlar icinde olsa da, genel kardiyovaskuler saglik icin doymamis yag asitleri acisindan zengin bir beslenme onerilebilir.",
                    "Ferritin degeri referans araliginin alt sinirina yakin oldugundan, demir acisindan zengin besinlerin (kirmizi et, baklagiller, yesil yaprakli sebzeler) tuketimi tesvik edilebilir; dusuk demir yorgunluk ve iştah dalgalanmalariyla kilo kontrolunu etkileyebilir.",
                    "Vitamin B12 duzeyleri normal sinirlar icindedir; vegan veya vejetaryen danisanlar icin B12 destegi veya zenginlestirilmis gidalara yonelik bilgilendirme yapilabilir.",
                    "Genel olarak tum degerlerin referans araliklari icinde olmasi memnuniyet vericidir; ancak HbA1c egilimi dikkate alinarak saglikli beslenme aliskanliklarinin surdurulmesi ve gelistirilmesi konusunda destek saglanabilir."
                ]
            });
    }
}
