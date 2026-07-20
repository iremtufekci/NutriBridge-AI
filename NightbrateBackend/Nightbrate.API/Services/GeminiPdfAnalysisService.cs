using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using Microsoft.Extensions.Options;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Options;

namespace Nightbrate.API.Services;

/// <summary>Gemini cok modlu: PDF inline base64 ile Turkce yapilandirilmis analiz.</summary>
public sealed class GeminiPdfAnalysisService : IPdfAnalysisAiService
{
    private static readonly JsonSerializerOptions JsonParse = new() { PropertyNameCaseInsensitive = true };

    private readonly HttpClient _http;
    private readonly GeminiMealAnalysisOptions _opt;

    public GeminiPdfAnalysisService(HttpClient http, IOptions<GeminiMealAnalysisOptions> options)
    {
        _http = http;
        _opt = options.Value;
        if (_http.BaseAddress is null)
            _http.BaseAddress = new Uri("https://generativelanguage.googleapis.com/");
        _http.Timeout = TimeSpan.FromSeconds(120);
    }

    public async Task<ClientPdfAnalysisResultDto> AnalyzePdfAsync(
        byte[] pdfBytes,
        string originalFileName,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(_opt.ApiKey))
            throw new AppException("Yapay zeka hizmeti için API anahtarı yapılandırılmamış.");

        if (pdfBytes.Length == 0)
            throw new AppException("PDF içeriği boş.");

        const string schemaJsonFixed = """
            {"type":"object","properties":{"documentType":{"type":"string"},"summary":{"type":"string"},"keyFindings":{"type":"array","items":{"type":"string"}},"cautions":{"type":"array","items":{"type":"string"}},"suggestedForDietitian":{"type":"array","items":{"type":"string"}}},"required":["documentType","summary","keyFindings","cautions","suggestedForDietitian"]}
            """;

        var b64 = Convert.ToBase64String(pdfBytes);
        var nameHint = string.IsNullOrWhiteSpace(originalFileName) ? "" : " Dosya adi: " + originalFileName + ".";

        var prompt =
            "Asagidaki PDF belgesi bir danisanin yukledigi laboratuvar, saglik veya beslenme ile ilgili dokuman olabilir." + nameHint
            + " GOREV: Belgedeki SAYISAL laboratuvar ve beslenme degerlerini oku. Her onemli parametre icin diyetisyen perspektifinden profesyonel yorum maddeleri uret. "
            + "Her madde su yapiya yakin olsun: parametre adi ve sayisal deger + referans durumu (normal, ust sinir, alt sinir, yukselmis vb.); "
            + "bu degerin kilo alma, kilo verememe veya kilo kaybi ile olasi iliskisi (neden olabilir veya olmayabilir); "
            + "diyetisyenin verebilecegi somut beslenme onerisi veya takip alani. "
            + "Ornek ton: \"Aksam kan sekeri normal sinirlar icinde olsa da, HbA1c degerinin prediyabet araligina yakinligi goz onune alindiginda karbonhidrat alimi ve ogun planlamasi konusunda danismanlik verilebilir.\" "
            + "KURALLAR: Tibbi tani koyma, ilac veya tedavi onerme. Sadece belgede gordugun degerlere dayan; uydurma. TURKCE yaz. "
            + "suggestedForDietitian alanina 4-8 tam cumle madde yaz (asiri kisa madde kullanma). "
            + "keyFindings ve cautions alanlarini bos dizi olarak don. "
            + "summary alanina yalnizca tek cumle belge turu yaz (ornegin: Tam kan sayimi ve biyokimya laboratuvar sonuclari).";

        var parts = new JsonArray
        {
            new JsonObject { ["text"] = prompt },
            new JsonObject
            {
                ["inline_data"] = new JsonObject
                {
                    ["mime_type"] = "application/pdf",
                    ["data"] = b64
                }
            }
        };

        var body = new JsonObject
        {
            ["contents"] = new JsonArray
            {
                new JsonObject { ["parts"] = parts }
            },
            ["generationConfig"] = new JsonObject
            {
                ["temperature"] = 0.25,
                ["responseMimeType"] = "application/json",
                ["responseSchema"] = JsonNode.Parse(schemaJsonFixed)!
            }
        };

        var model = string.IsNullOrWhiteSpace(_opt.Model) ? "gemini-2.5-flash" : _opt.Model.Trim();
        var path = $"v1beta/models/{model}:generateContent?key={Uri.EscapeDataString(_opt.ApiKey)}";

        using var opCts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        opCts.CancelAfter(TimeSpan.FromMinutes(4));

        string raw;
        try
        {
            using var content = new StringContent(body.ToJsonString(), Encoding.UTF8, "application/json");
            content.Headers.ContentType = new MediaTypeHeaderValue("application/json");
            using var response = await _http.PostAsync(path, content, opCts.Token).ConfigureAwait(false);
            raw = await response.Content.ReadAsStringAsync(opCts.Token).ConfigureAwait(false);

            if (!response.IsSuccessStatusCode)
            {
                if (IsQuotaOrRateLimited(response.StatusCode, raw))
                {
                    throw new AppException(
                        "Yapay zeka hizmetinde kota veya geçici yoğunluk. Daha sonra tekrar deneyin.");
                }

                var hint = TryExtractGeminiErrorMessage(raw);
                throw new AppException(
                    string.IsNullOrWhiteSpace(hint)
                        ? "PDF analizi tamamlanamadı. API anahtarını ve model adını kontrol edin."
                        : $"PDF analizi başarısız: {hint}");
            }
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            throw new AppException("PDF analizi zaman aşımına uğradı. Daha küçük bir dosya deneyin.");
        }

        var text = ExtractResponseText(raw);
        if (string.IsNullOrWhiteSpace(text))
            throw new AppException("Yapay zeka yanıtı boş. Başka bir PDF deneyin.");

        PdfRootJson? root;
        try
        {
            root = JsonSerializer.Deserialize<PdfRootJson>(text, JsonParse);
        }
        catch (JsonException)
        {
            throw new AppException("Yapay zeka yanıtı çözümlenemedi.");
        }

        var comments = CleanList(root.SuggestedForDietitian);
        if (root is null || (string.IsNullOrWhiteSpace(root.Summary) && comments.Count == 0))
            throw new AppException("Geçerli analiz metni alınamadı.");

        return new ClientPdfAnalysisResultDto
        {
            AnalysisSource = "gemini",
            DocumentType = (root.DocumentType ?? "Belge").Trim(),
            Summary = string.IsNullOrWhiteSpace(root.Summary) ? (comments.FirstOrDefault() ?? "Laboratuvar / rapor belgesi") : root.Summary.Trim(),
            KeyFindings = new List<string>(),
            Cautions = new List<string>(),
            SuggestedForDietitian = comments
        };
    }

    private static List<string> CleanList(List<string>? raw) =>
        raw?.Where(s => !string.IsNullOrWhiteSpace(s)).Select(s => s.Trim()).Take(24).ToList() ?? new List<string>();

    private static bool IsQuotaOrRateLimited(HttpStatusCode status, string raw)
    {
        if (status == HttpStatusCode.TooManyRequests) return true;
        if (string.IsNullOrEmpty(raw)) return false;
        return raw.Contains("quota", StringComparison.OrdinalIgnoreCase)
               || raw.Contains("RESOURCE_EXHAUSTED", StringComparison.OrdinalIgnoreCase);
    }

    private static string? TryExtractGeminiErrorMessage(string raw)
    {
        try
        {
            using var doc = JsonDocument.Parse(raw);
            if (doc.RootElement.TryGetProperty("error", out var err) && err.TryGetProperty("message", out var msg))
                return msg.GetString();
        }
        catch (JsonException)
        {
            // ignore
        }

        return null;
    }

    private static string? ExtractResponseText(string raw)
    {
        using var doc = JsonDocument.Parse(raw);
        if (!doc.RootElement.TryGetProperty("candidates", out var candidates) || candidates.GetArrayLength() == 0)
            return null;
        var first = candidates[0];
        if (!first.TryGetProperty("content", out var content) || !content.TryGetProperty("parts", out var partsEl))
            return null;
        foreach (var part in partsEl.EnumerateArray())
        {
            if (part.TryGetProperty("text", out var textEl))
                return textEl.GetString();
        }

        return null;
    }

    private sealed class PdfRootJson
    {
        public string? DocumentType { get; set; }
        public string? Summary { get; set; }
        public List<string>? KeyFindings { get; set; }
        public List<string>? Cautions { get; set; }
        public List<string>? SuggestedForDietitian { get; set; }
    }
}
