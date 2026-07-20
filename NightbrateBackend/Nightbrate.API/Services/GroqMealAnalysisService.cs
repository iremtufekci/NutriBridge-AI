using Microsoft.Extensions.Options;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Options;

namespace Nightbrate.API.Services;

public sealed class GroqMealAnalysisService(HttpClient http, IOptions<GroqAiOptions> options) : IMealAnalysisService
{
    private readonly GroqAiOptions _opt = options.Value;

    public async Task<MealAnalysisResultDto> AnalyzeImageAsync(string imageFilePath, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(_opt.ApiKey))
            throw new AppException("Groq API anahtarı yapılandırılmamış.");

        if (!File.Exists(imageFilePath))
            throw new AppException("Analiz için görüntü dosyası bulunamadı.");

        var ext = Path.GetExtension(imageFilePath).ToLowerInvariant();
        var mime = ext switch
        {
            ".png" => "image/png",
            ".jpg" or ".jpeg" => "image/jpeg",
            _ => "image/jpeg"
        };
        var bytes = await File.ReadAllBytesAsync(imageFilePath, cancellationToken).ConfigureAwait(false);
        return await AnalyzeImageBytesAsync(bytes, mime, cancellationToken).ConfigureAwait(false);
    }

    public async Task<MealAnalysisResultDto> AnalyzeImageBytesAsync(byte[] imageBytes, string mimeType, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(_opt.ApiKey))
            throw new AppException("Groq API anahtarı yapılandırılmamış.");
        if (imageBytes is null || imageBytes.Length == 0)
            throw new AppException("Analiz için görüntü verisi boş.");

        var mime = string.IsNullOrWhiteSpace(mimeType) ? "image/jpeg" : mimeType.Trim();
        var model = string.IsNullOrWhiteSpace(_opt.VisionModel)
            ? "meta-llama/llama-4-scout-17b-16e-instruct"
            : _opt.VisionModel.Trim();

        const string prompt = """
            Bu fotograftaki yemekleri analiz et. Porsiyonlari gercekci varsay.
            Besin adlarini kisa Turkce yaz (ornegin: Izgara tavuk, Bulgur pilavi).
            Yalnizca su JSON formatinda yanit ver:
            {"foods":["..."],"estimatedCalories":0,"proteinGrams":0.0,"carbGrams":0.0,"fatGrams":0.0}
            """;

        var json = await GroqChatClient.CompleteVisionJsonAsync(http, _opt.ApiKey, model, prompt, imageBytes, mime, 0.35, cancellationToken)
            .ConfigureAwait(false);
        var parsed = GroqChatClient.DeserializeJson<MealJson>(json);

        var foods = parsed.Foods?.Where(s => !string.IsNullOrWhiteSpace(s)).Select(s => s.Trim()).Distinct().ToList()
                    ?? new List<string>();

        return new MealAnalysisResultDto
        {
            Foods = foods,
            EstimatedCalories = Math.Clamp(parsed.EstimatedCalories, 1, 8000),
            Protein = Math.Max(0, parsed.ProteinGrams),
            Carb = Math.Max(0, parsed.CarbGrams),
            Fat = Math.Max(0, parsed.FatGrams),
            AnalysisSource = "groq"
        };
    }

    private sealed class MealJson
    {
        public List<string>? Foods { get; set; }
        public int EstimatedCalories { get; set; }
        public double ProteinGrams { get; set; }
        public double CarbGrams { get; set; }
        public double FatGrams { get; set; }
    }
}
