using Microsoft.Extensions.Options;
using Nightbrate.Application;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Options;

namespace Nightbrate.API.Services;

public sealed class GroqKitchenChefService(HttpClient http, IOptions<GroqAiOptions> options) : IKitchenChefService
{
    private readonly GroqAiOptions _opt = options.Value;

    public async Task<KitchenChefResponseDto> GenerateRecipesAsync(KitchenChefRequestDto request, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(_opt.ApiKey))
            throw new AppException("Groq API anahtarı yapılandırılmamış.");
        if (string.IsNullOrWhiteSpace(request.Ingredients))
            throw new AppException("Malzemeler boş olamaz.");
        if (!KitchenChefPreferences.IsValid(request.Preference))
            throw new AppException("Geçerli bir tercih seçin.");
        if (request.TargetCalories is < 200 or > 5000)
            throw new AppException("Hedef kalori 200–5000 arasında olmalıdır.");

        var model = string.IsNullOrWhiteSpace(_opt.TextModel) ? "llama-3.3-70b-versatile" : _opt.TextModel.Trim();
        var preferenceLabel = KitchenChefPreferences.LabelOrDefault(request.Preference);
        var kcal = Math.Clamp(request.TargetCalories, 200, 5000);

        var prompt =
            "Evde su malzemeler var: " + request.Ingredients.Trim() + ". "
            + "Kullanici tercihi: " + preferenceLabel + ". "
            + "Hedef: yaklasik " + kcal + " kcal civarinda 2-5 farkli Turkce tarif uret. "
            + "Yalnizca JSON don: {\"recipes\":[{\"title\":\"...\",\"description\":\"...\","
            + "\"estimatedCalories\":0,\"prepTimeMinutes\":0,\"ingredients\":[\"...\"],\"steps\":[\"...\"]}]}";

        var json = await GroqChatClient.CompleteTextJsonAsync(http, _opt.ApiKey, model, prompt, 0.45, cancellationToken).ConfigureAwait(false);
        var root = GroqChatClient.DeserializeJson<RecipeRootJson>(json);
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
                Ingredients = r.Ingredients?.Where(x => !string.IsNullOrWhiteSpace(x)).Select(x => x.Trim()).ToList() ?? [],
                Steps = r.Steps?.Where(x => !string.IsNullOrWhiteSpace(x)).Select(x => x.Trim()).ToList() ?? []
            });
        }

        if (list.Count == 0)
            throw new AppException("Geçerli tarif üretilemedi.");

        return new KitchenChefResponseDto { Source = "groq", Recipes = list };
    }

    private sealed class RecipeRootJson
    {
        public List<RecipeJson>? Recipes { get; set; }
    }

    private sealed class RecipeJson
    {
        public string? Title { get; set; }
        public string? Description { get; set; }
        public int EstimatedCalories { get; set; }
        public int? PrepTimeMinutes { get; set; }
        public List<string>? Ingredients { get; set; }
        public List<string>? Steps { get; set; }
    }
}
