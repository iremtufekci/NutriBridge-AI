using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using Microsoft.Extensions.Options;
using Nightbrate.Application;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Options;

namespace Nightbrate.API.Services;

/// <summary>Gemini metin API: malzeme + tercih + hedef kaloriye gore Turkce tarif listesi.</summary>
public sealed class GeminiKitchenChefService : IKitchenChefService
{
    private static readonly JsonSerializerOptions JsonParse = new() { PropertyNameCaseInsensitive = true };

    private readonly HttpClient _http;
    private readonly GeminiMealAnalysisOptions _opt;

    public GeminiKitchenChefService(HttpClient http, IOptions<GeminiMealAnalysisOptions> options)
    {
        _http = http;
        _opt = options.Value;
        if (_http.BaseAddress is null)
            _http.BaseAddress = new Uri("https://generativelanguage.googleapis.com/");
        _http.Timeout = TimeSpan.FromSeconds(90);
    }

    public async Task<KitchenChefResponseDto> GenerateRecipesAsync(KitchenChefRequestDto request, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(_opt.ApiKey))
            throw new AppException("Yapay zeka hizmeti için API anahtarı yapılandırılmamış.");

        if (string.IsNullOrWhiteSpace(request.Ingredients))
            throw new AppException("Malzemeler boş olamaz.");

        var preferenceLabel = KitchenChefPreferences.LabelOrDefault(request.Preference);
        var kcal = Math.Clamp(request.TargetCalories, 200, 5000);

        const string schemaJson = """
            {"type":"object","properties":{"recipes":{"type":"array","minItems":2,"maxItems":5,"items":{"type":"object","properties":{"title":{"type":"string"},"description":{"type":"string"},"estimatedCalories":{"type":"integer"},"prepTimeMinutes":{"type":"integer"},"ingredients":{"type":"array","items":{"type":"string"}},"steps":{"type":"array","items":{"type":"string"}}},"required":["title","estimatedCalories","ingredients","steps"]}}},"required":["recipes"]}
            """;

        var prompt =
            "Evde su malzemeler var: " + request.Ingredients.Trim() + ". "
            + "Kullanici tercihi: " + preferenceLabel + ". "
            + "Hedef: toplamda yaklasik " + kcal + " kcal civarinda (porsiyon veya ana ogun) olacak sekilde tarifler uret. "
            + "Malzemeleri munkun oldugunca kullan; eksik baharat icin makul varsay. "
            + "Yanit tamamen Turkce olsun. Ayni yemegi tekrar etmeden, birbirinden farkli 2-5 tarif oner; her birinde tahmini kalori ve adim adim yapilis yaz.";

        var body = new JsonObject
        {
            ["contents"] = new JsonArray
            {
                new JsonObject
                {
                    ["parts"] = new JsonArray { new JsonObject { ["text"] = prompt } }
                }
            },
            ["generationConfig"] = new JsonObject
            {
                ["temperature"] = 0.45,
                ["responseMimeType"] = "application/json",
                ["responseSchema"] = JsonNode.Parse(schemaJson)!
            }
        };

        var model = string.IsNullOrWhiteSpace(_opt.Model) ? "gemini-2.5-flash-lite" : _opt.Model.Trim();
        var path = $"v1beta/models/{model}:generateContent?key={Uri.EscapeDataString(_opt.ApiKey)}";
        var bodyJson = body.ToJsonString();

        using var opCts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        opCts.CancelAfter(TimeSpan.FromMinutes(3));

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
                    await Task.Delay(TimeSpan.FromSeconds(Math.Min(delaySec, 15)), opCts.Token).ConfigureAwait(false);
                }

                response?.Dispose();
                using var content = new StringContent(bodyJson, Encoding.UTF8, "application/json");
                content.Headers.ContentType = new MediaTypeHeaderValue("application/json");
                response = await _http.PostAsync(path, content, opCts.Token).ConfigureAwait(false);
                raw = await response.Content.ReadAsStringAsync(opCts.Token).ConfigureAwait(false);

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
                        "Tarif şu an üretilemedi: yapay zeka hizmetinde geçici yoğunluk var. " +
                        "Birkaç dakika sonra tekrar deneyin.");
                }

                if (IsQuotaOrRateLimited(response.StatusCode, raw))
                {
                    throw new AppException(
                        "Yapay zeka hizmeti kotası doldu veya seçilen model için ücretsiz kullanım kapalı. " +
                        "Bir süre sonra tekrar deneyin veya yapılandırmada daha hafif bir model seçin.");
                }

                var hint = TryExtractGeminiErrorMessage(raw);
                throw new AppException(
                    string.IsNullOrWhiteSpace(hint)
                        ? "Tarif üretilemedi. API anahtarını ve model adını kontrol edin."
                        : $"Tarif üretilemedi: {ToUserFriendlyHint(hint)}");
            }

            if (response is null || !response.IsSuccessStatusCode)
                throw new AppException("Tarif üretilemedi. Lütfen tekrar deneyin.");
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            throw new AppException("Tarif üretimi zaman aşımına uğradı. Kısa bir süre sonra tekrar deneyin.");
        }
        finally
        {
            response?.Dispose();
        }

        var text = ExtractResponseText(raw);
        if (string.IsNullOrWhiteSpace(text))
            throw new AppException("Yapay zeka yanıtı boş. Lütfen tekrar deneyin.");

        KitchenChefRootJson? root;
        try
        {
            root = JsonSerializer.Deserialize<KitchenChefRootJson>(text, JsonParse);
        }
        catch (JsonException)
        {
            throw new AppException("Yapay zeka yanıtı çözümlenemedi. Lütfen tekrar deneyin.");
        }

        if (root?.Recipes is not { Count: > 0 })
            throw new AppException("Tarif listesi alınamadı.");

        var list = new List<KitchenChefRecipeDto>();
        foreach (var r in root.Recipes)
        {
            if (string.IsNullOrWhiteSpace(r.Title)) continue;
            list.Add(new KitchenChefRecipeDto
            {
                Title = r.Title.Trim(),
                Description = r.Description?.Trim(),
                EstimatedCalories = Math.Clamp(r.EstimatedCalories, 50, 8000),
                PrepTimeMinutes = r.PrepTimeMinutes is > 0 and < 1000 ? r.PrepTimeMinutes : null,
                Ingredients = r.Ingredients?.Where(s => !string.IsNullOrWhiteSpace(s)).Select(s => s.Trim()).ToList() ?? new List<string>(),
                Steps = r.Steps?.Where(s => !string.IsNullOrWhiteSpace(s)).Select(s => s.Trim()).ToList() ?? new List<string>()
            });
        }

        if (list.Count == 0)
            throw new AppException("Geçerli tarif üretilemedi.");

        return new KitchenChefResponseDto
        {
            Source = "gemini",
            Recipes = list
        };
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
        if (status == HttpStatusCode.TooManyRequests) return true;
        if (string.IsNullOrEmpty(raw)) return false;
        return raw.Contains("quota", StringComparison.OrdinalIgnoreCase)
               || raw.Contains("Quota exceeded", StringComparison.OrdinalIgnoreCase)
               || raw.Contains("RESOURCE_EXHAUSTED", StringComparison.OrdinalIgnoreCase)
               || raw.Contains("rate limit", StringComparison.OrdinalIgnoreCase);
    }

    private static string ToUserFriendlyHint(string hint)
    {
        if (hint.Contains("high demand", StringComparison.OrdinalIgnoreCase) ||
            hint.Contains("overloaded", StringComparison.OrdinalIgnoreCase))
        {
            return "Yapay zeka hizmetinde geçici yoğunluk var. Birkaç dakika sonra tekrar deneyin.";
        }

        return hint;
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

    private sealed class KitchenChefRootJson
    {
        public List<KitchenChefRecipeJson>? Recipes { get; set; }
    }

    private sealed class KitchenChefRecipeJson
    {
        public string? Title { get; set; }
        public string? Description { get; set; }
        public int EstimatedCalories { get; set; }
        public int? PrepTimeMinutes { get; set; }
        public List<string>? Ingredients { get; set; }
        public List<string>? Steps { get; set; }
    }
}
