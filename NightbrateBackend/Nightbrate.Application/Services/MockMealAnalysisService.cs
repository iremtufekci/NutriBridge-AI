using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.Application.Services;

/// <summary>Simulasyon: gecikme + rastgele besin listesi ve makul kalori. Ileride gercek API burada degistirilecek.</summary>
public class MockMealAnalysisService : IMealAnalysisService
{
    private static readonly string[] Pool =
    [
        "Izgara Tavuk",
        "Karışık Salata",
        "Bulgur Pilavı",
        "Mercimek Çorbası",
        "Fırın Sebze",
        "Yoğurt",
        "Tam Buğday Ekmeği",
        "Haşlanmış Yumurta",
        "Avokado",
        "Ton Balığı",
        "Kinoa Salatası",
        "Zeytinyağlı Enginar"
    ];

    public async Task<MealAnalysisResultDto> AnalyzeImageAsync(string imageFilePath, CancellationToken cancellationToken = default)
    {
        _ = imageFilePath;
        await Task.Delay(Random.Shared.Next(600, 1200), cancellationToken).ConfigureAwait(false);

        var count = Random.Shared.Next(2, 5);
        var foods = Pool.OrderBy(_ => Random.Shared.Next()).Take(count).ToList();
        var kcal = Random.Shared.Next(280, 720);

        var p = Math.Round(kcal * 0.003 + Random.Shared.NextDouble() * 8, 1);
        var c = Math.Round(kcal * 0.012 + Random.Shared.NextDouble() * 15, 1);
        var f = Math.Round(kcal * 0.004 + Random.Shared.NextDouble() * 10, 1);

        return new MealAnalysisResultDto
        {
            Foods = foods,
            EstimatedCalories = kcal,
            Protein = p,
            Carb = c,
            Fat = f
        };
    }
}
