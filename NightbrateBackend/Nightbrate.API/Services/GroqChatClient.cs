using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Utils;

namespace Nightbrate.API.Services;

/// <summary>Groq OpenAI uyumlu chat/completions istemcisi.</summary>
internal static class GroqChatClient
{
    private static readonly JsonSerializerOptions JsonParse = new() { PropertyNameCaseInsensitive = true };

    public static async Task<string> CompleteTextJsonAsync(
        HttpClient http,
        string apiKey,
        string model,
        string userPrompt,
        double temperature,
        CancellationToken cancellationToken)
    {
        var body = new JsonObject
        {
            ["model"] = model,
            ["temperature"] = temperature,
            ["response_format"] = new JsonObject { ["type"] = "json_object" },
            ["messages"] = new JsonArray
            {
                new JsonObject
                {
                    ["role"] = "system",
                    ["content"] = "Sen bir beslenme asistanısın. Yanıtın yalnızca geçerli JSON olsun; markdown veya açıklama ekleme."
                },
                new JsonObject
                {
                    ["role"] = "user",
                    ["content"] = userPrompt
                }
            }
        };

        return await PostAndExtractContentAsync(http, apiKey, body, cancellationToken).ConfigureAwait(false);
    }

    public static async Task<string> CompleteVisionJsonAsync(
        HttpClient http,
        string apiKey,
        string model,
        string userPrompt,
        byte[] imageBytes,
        string mimeType,
        double temperature,
        CancellationToken cancellationToken)
    {
        var b64 = Convert.ToBase64String(imageBytes);
        var dataUrl = $"data:{mimeType};base64,{b64}";

        var body = new JsonObject
        {
            ["model"] = model,
            ["temperature"] = temperature,
            ["response_format"] = new JsonObject { ["type"] = "json_object" },
            ["messages"] = new JsonArray
            {
                new JsonObject
                {
                    ["role"] = "system",
                    ["content"] = "Sen bir beslenme asistanısın. Yanıtın yalnızca geçerli JSON olsun."
                },
                new JsonObject
                {
                    ["role"] = "user",
                    ["content"] = new JsonArray
                    {
                        new JsonObject { ["type"] = "text", ["text"] = userPrompt },
                        new JsonObject
                        {
                            ["type"] = "image_url",
                            ["image_url"] = new JsonObject { ["url"] = dataUrl }
                        }
                    }
                }
            }
        };

        return await PostAndExtractContentAsync(http, apiKey, body, cancellationToken).ConfigureAwait(false);
    }

    private static async Task<string> PostAndExtractContentAsync(
        HttpClient http,
        string apiKey,
        JsonObject body,
        CancellationToken cancellationToken)
    {
        const int maxAttempts = 4;
        Exception? lastNetwork = null;

        for (var attempt = 1; attempt <= maxAttempts; attempt++)
        {
            try
            {
                return await PostOnceAsync(http, apiKey, body, cancellationToken).ConfigureAwait(false);
            }
            catch (Exception ex) when (attempt < maxAttempts && AiNetworkExceptionHelper.IsNetworkOrDnsFailure(ex))
            {
                lastNetwork = ex;
                await Task.Delay(TimeSpan.FromMilliseconds(750 * attempt), cancellationToken).ConfigureAwait(false);
            }
        }

        throw lastNetwork ?? new HttpRequestException("Groq API isteği başarısız.");
    }

    private static async Task<string> PostOnceAsync(
        HttpClient http,
        string apiKey,
        JsonObject body,
        CancellationToken cancellationToken)
    {
        using var opCts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        opCts.CancelAfter(TimeSpan.FromMinutes(3));

        using var request = new HttpRequestMessage(HttpMethod.Post, "chat/completions");
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", apiKey);
        request.Content = new StringContent(body.ToJsonString(), Encoding.UTF8, "application/json");

        using var response = await http.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, opCts.Token)
            .ConfigureAwait(false);
        var raw = await response.Content.ReadAsStringAsync(opCts.Token).ConfigureAwait(false);

        if (!response.IsSuccessStatusCode)
        {
            if (response.StatusCode == HttpStatusCode.TooManyRequests)
                throw new AppException("Groq API kotası veya hız sınırı aşıldı. Bir süre sonra tekrar deneyin.");

            var hint = TryExtractErrorMessage(raw);
            throw new AppException(
                string.IsNullOrWhiteSpace(hint)
                    ? "Groq yapay zeka isteği başarısız. API anahtarını ve model adını kontrol edin."
                    : $"Groq hatası: {hint}");
        }

        var text = ExtractAssistantContent(raw);
        if (string.IsNullOrWhiteSpace(text))
            throw new AppException("Groq yanıtı boş döndü.");

        return StripJsonFence(text);
    }

    private static string StripJsonFence(string text)
    {
        var t = text.Trim();
        if (t.StartsWith("```", StringComparison.Ordinal))
        {
            var nl = t.IndexOf('\n');
            if (nl >= 0) t = t[(nl + 1)..];
            if (t.EndsWith("```", StringComparison.Ordinal)) t = t[..^3];
        }
        return t.Trim();
    }

    private static string? ExtractAssistantContent(string raw)
    {
        using var doc = JsonDocument.Parse(raw);
        if (!doc.RootElement.TryGetProperty("choices", out var choices) || choices.GetArrayLength() == 0)
            return null;
        var msg = choices[0];
        if (!msg.TryGetProperty("message", out var message))
            return null;
        if (message.TryGetProperty("content", out var contentEl))
            return contentEl.GetString();
        return null;
    }

    private static string? TryExtractErrorMessage(string raw)
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

    public static T DeserializeJson<T>(string json) where T : class
    {
        try
        {
            return JsonSerializer.Deserialize<T>(json, JsonParse)
                   ?? throw new AppException("Yapay zeka yanıtı çözümlenemedi.");
        }
        catch (JsonException)
        {
            throw new AppException("Yapay zeka yanıtı geçerli JSON değil.");
        }
    }
}
