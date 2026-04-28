using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.Application.Services;

public sealed class MockKitchenChefService : IKitchenChefService
{
    public Task<KitchenChefResponseDto> GenerateRecipesAsync(KitchenChefRequestDto request, CancellationToken cancellationToken = default)
    {
        var k = Math.Clamp(request.TargetCalories, 200, 5000);
        var a = Math.Max(250, (int)(k * 0.4));
        var b = Math.Max(250, (int)(k * 0.5));
        return Task.FromResult(new KitchenChefResponseDto
        {
            Source = "mock",
            Recipes =
            [
                new KitchenChefRecipeDto
                {
                    Title = "Örnek 1: Tencere yemeği (sebze + protein)",
                    Description = "API’de Gemini anahtarı yok; birden fazla örnek tarif. Anahtar ekleyince AI üretir.",
                    EstimatedCalories = a,
                    PrepTimeMinutes = 30,
                    Ingredients = ["Seçtiğiniz malzemeler", "Sıvı", "Tuz, baharat"],
                    Steps = ["Malzemeleri hazırlayın.", "Tencerede pişirin.", "Kıvamını ayarlayıp servis edin."]
                },
                new KitchenChefRecipeDto
                {
                    Title = "Örnek 2: Fırın / tek porsiyon",
                    EstimatedCalories = b,
                    PrepTimeMinutes = 35,
                    Ingredients = ["Seçtiğiniz malzemeler", "Yağ", "Fırın kâsesi veya tepsi"],
                    Steps = ["Malzemeyi parçalayın.", "Baharatla harmanlayın.", "Pişirip kontrol edin."]
                }
            ]
        });
    }
}
