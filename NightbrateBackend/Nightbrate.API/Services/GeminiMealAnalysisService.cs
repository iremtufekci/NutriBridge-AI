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

/// <summary>
/// Google Gemini cok modlu API: fotograftaki yemekleri ve tahmini makrolari JSON olarak uretir.
/// </summary>
public sealed class GeminiMealAnalysisService : IMealAnalysisService
{
    private static readonly JsonSerializerOptions JsonParse = new() { PropertyNameCaseInsensitive = true };

    private readonly HttpClient _http;
    private readonly GeminiMealAnalysisOptions _opt;

    public GeminiMealAnalysisService(HttpClient http, IOptions<GeminiMealAnalysisOptions> options)
    {
        _http = http;
        _opt = options.Value;
        if (_http.BaseAddress is null)
            _http.BaseAddress = new Uri("https://generativelanguage.googleapis.com/");
        _http.Timeout = TimeSpan.FromSeconds(90);
    }

    public async Task<MealAnalysisResultDto> AnalyzeImageAsync(string imageFilePath, CancellationToken cancellationToken = default)
    {
        using var opCts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        opCts.CancelAfter(TimeSpan.FromMinutes(4));
        var ct = opCts.Token;

        try
        {
            if (string.IsNullOrWhiteSpace(_opt.ApiKey))
                throw new AppException("Yapay zeka hizmeti için API anahtarı yapılandırılmamış.");

            if (!File.Exists(imageFilePath))
                throw new AppException("Analiz için görüntü dosyası bulunamadı.");

            var ext = Path.GetExtension(imageFilePath).ToLowerInvariant();
            var mime = ext switch
            {
                ".png" => "image/png",
                ".jpg" or ".jpeg" => "image/jpeg",
                _ => "image/jpeg"
            };

            var bytes = await File.ReadAllBytesAsync(imageFilePath, ct).ConfigureAwait(false);
            return await AnalyzeImageBytesCoreAsync(bytes, mime, ct).ConfigureAwait(false);
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            throw new AppException(
                "Görsel analizi zaman aşımına uğradı (işlem uzun sürdü). Daha düşük çözünürlükte bir fotoğraf " +
                "deneyin veya birkaç dakika sonra tekrar deneyin.");
        }
    }

    public async Task<MealAnalysisResultDto> AnalyzeImageBytesAsync(byte[] imageBytes, string mimeType, CancellationToken cancellationToken = default)
    {
        using var opCts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        opCts.CancelAfter(TimeSpan.FromMinutes(4));
        var ct = opCts.Token;

        try
        {
            if (string.IsNullOrWhiteSpace(_opt.ApiKey))
                throw new AppException("Yapay zeka hizmeti için API anahtarı yapılandırılmamış.");

            if (imageBytes is null || imageBytes.Length == 0)
                throw new AppException("Analiz için görüntü verisi boş.");

            var mime = string.IsNullOrWhiteSpace(mimeType) ? "image/jpeg" : mimeType.Trim();
            return await AnalyzeImageBytesCoreAsync(imageBytes, mime, ct).ConfigureAwait(false);
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            throw new AppException(
                "Görsel analizi zaman aşımına uğradı (işlem uzun sürdü). Daha düşük çözünürlükte bir fotoğraf " +
                "deneyin veya birkaç dakika sonra tekrar deneyin.");
        }
    }

    private async Task<MealAnalysisResultDto> AnalyzeImageBytesCoreAsync(byte[] bytes, string mime, CancellationToken cancellationToken)
    {
        var b64 = Convert.ToBase64String(bytes);

        const string schemaJson =
            """{"type":"object","properties":{"foods":{"type":"array","items":{"type":"string"}},"estimatedCalories":{"type":"integer"},"proteinGrams":{"type":"number"},"carbGrams":{"type":"number"},"fatGrams":{"type":"number"}},"required":["foods","estimatedCalories","proteinGrams","carbGrams","fatGrams"]}""";

        var prompt =
            "Bu fotograftaki yemekleri analiz et. Porsiyonlari gercekci varsay. "
            + "Besin adlarini kisa Turkce yaz (ornegin: Izgara tavuk, Bulgur pilavi). "
            + "JSON semasina uy; tahmini toplam kalori ve makrolari (gram) ver.";

        var parts = new JsonArray
        {
            new JsonObject
            {
                ["inline_data"] = new JsonObject
                {
                    ["mime_type"] = mime,
                    ["data"] = b64
                }
            },
            new JsonObject { ["text"] = prompt }
        };

        var body = new JsonObject
        {
            ["contents"] = new JsonArray
            {
                new JsonObject { ["parts"] = parts }
            },
            ["generationConfig"] = new JsonObject
            {
                ["temperature"] = 0.35,
                ["responseMimeType"] = "application/json",
                ["responseSchema"] = JsonNode.Parse(schemaJson)!
            }
        };

        var model = string.IsNullOrWhiteSpace(_opt.Model) ? "gemini-2.5-flash" : _opt.Model.Trim();
        var path = $"v1beta/models/{model}:generateContent?key={Uri.EscapeDataString(_opt.ApiKey)}";
        var bodyJson = body.ToJsonString();

        const int maxAttempts = 3;
        string raw = "";
        HttpResponseMessage? response = null;
        try
        {
            for (var attempt = 0; attempt < maxAttempts; attempt++)
            {
                if (attempt > 0)
                {
                    var delaySec = 2 * (1 << (attempt - 1));
                    await Task.Delay(TimeSpan.FromSeconds(Math.Min(delaySec, 15)), cancellationToken).ConfigureAwait(false);
                }

                response?.Dispose();
                using var requestContent = new StringContent(bodyJson, Encoding.UTF8, "application/json");
                requestContent.Headers.ContentType = new MediaTypeHeaderValue("application/json");

                response = await _http.PostAsync(path, requestContent, cancellationToken).ConfigureAwait(false);
                raw = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);

                if (response.IsSuccessStatusCode)
                    break;

                if (raw.Contains("is not found", StringComparison.OrdinalIgnoreCase) ||
                    raw.Contains("not supported for generateContent", StringComparison.OrdinalIgnoreCase))
                {
                    throw new AppException(
                        "Seçilen yapay zeka modeli bulunamıyor veya bu işlem için kullanılamıyor. " +
                        "Sunucu yapılandırmasındaki model adını güncel bir sürüme çekin.");
                }

                if (IsTransientGeminiOverload(response.StatusCode, raw) && attempt < maxAttempts - 1)
                    continue;

                if (IsTransientGeminiOverload(response.StatusCode, raw))
                {
                    throw new AppException(
                        "Görsel analizi şu an tamamlanamadı: yapay zeka hizmetinde geçici yoğunluk var. " +
                        "Bir süre sonra tekrar deneyin veya yapılandırmada daha hafif bir model deneyin.");
                }

                if (IsQuotaOrRateLimited(response.StatusCode, raw))
                {
                    throw new AppException(
                        "Yapay zeka kotası doldu veya seçilen model için ücretsiz kullanım kapalı. " +
                        "Kota ve faturayı sağlayıcı konsolundan kontrol edin; birkaç dakika sonra tekrar deneyin.");
                }

                var hint = TryExtractGeminiErrorMessage(raw);
                throw new AppException(
                    string.IsNullOrWhiteSpace(hint)
                        ? "Görsel analizi tamamlanamadı. API anahtarını ve model adını kontrol edin."
                        : $"Görsel analizi tamamlanamadı: {hint}");
            }

            if (response is null || !response.IsSuccessStatusCode)
            {
                throw new AppException("Görsel analizi tamamlanamadı. Lütfen tekrar deneyin.");
            }

            var text = ExtractResponseText(raw);
            if (string.IsNullOrWhiteSpace(text))
                throw new AppException("Yapay zeka yanıtı boş veya güvenlik nedeniyle engellendi. Başka bir fotoğraf deneyin.");

            MealGeminiJson? parsed;
            try
            {
                parsed = JsonSerializer.Deserialize<MealGeminiJson>(text, JsonParse);
            }
            catch (JsonException)
            {
                throw new AppException("Yapay zeka yanıtı çözümlenemedi. Lütfen tekrar deneyin.");
            }

            if (parsed is null)
                throw new AppException("Yapay zeka analiz sonucu alınamadı.");

            var foods = parsed.Foods?.Where(static s => !string.IsNullOrWhiteSpace(s)).Select(static s => s.Trim()).Distinct().ToList()
                        ?? new List<string>();

            var kcal = Math.Clamp(parsed.EstimatedCalories, 1, 8000);

            return new MealAnalysisResultDto
            {
                Foods = foods,
                EstimatedCalories = kcal,
                Protein = Math.Max(0, parsed.ProteinGrams),
                Carb = Math.Max(0, parsed.CarbGrams),
                Fat = Math.Max(0, parsed.FatGrams)
            };
        }
        finally
        {
            response?.Dispose();
        }
    }

    private static bool IsTransientGeminiOverload(HttpStatusCode status, string raw)
    {
        if (status == HttpStatusCode.ServiceUnavailable)
            return true;
        if (string.IsNullOrEmpty(raw))
            return false;
        return raw.Contains("high demand", StringComparison.OrdinalIgnoreCase)
               || raw.Contains("overloaded", StringComparison.OrdinalIgnoreCase)
               || raw.Contains("UNAVAILABLE", StringComparison.OrdinalIgnoreCase);
    }

    private static bool IsQuotaOrRateLimited(HttpStatusCode status, string raw)
    {
        if (status == HttpStatusCode.TooManyRequests)
            return true;
        if (string.IsNullOrEmpty(raw))
            return false;
        return raw.Contains("quota", StringComparison.OrdinalIgnoreCase)
               || raw.Contains("Quota exceeded", StringComparison.OrdinalIgnoreCase)
               || raw.Contains("RESOURCE_EXHAUSTED", StringComparison.OrdinalIgnoreCase)
               || raw.Contains("rate limit", StringComparison.OrdinalIgnoreCase);
    }

    private static string? TryExtractGeminiErrorMessage(string raw)
    {
        try
        {
            using var doc = JsonDocument.Parse(raw);
            if (doc.RootElement.TryGetProperty("error", out var err) &&
                err.TryGetProperty("message", out var msg))
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

    private sealed class MealGeminiJson
    {
        public List<string>? Foods { get; set; }
        public int EstimatedCalories { get; set; }
        public double ProteinGrams { get; set; }
        public double CarbGrams { get; set; }
        public double FatGrams { get; set; }
    }
}
