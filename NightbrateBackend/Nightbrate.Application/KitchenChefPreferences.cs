namespace Nightbrate.Application;

/// <summary>AI Mutfak Sefi: tek secimlik tercih kodlari (web/mobil/API ortak).</summary>
public static class KitchenChefPreferences
{
    public static readonly string[] Codes =
    [
        "practical",
        "low_calorie",
        "vegetarian",
        "high_protein",
        "vegan",
        "gluten_free"
    ];

    public static bool IsValid(string? code)
    {
        if (string.IsNullOrWhiteSpace(code)) return false;
        var t = code.Trim();
        foreach (var c in Codes)
        {
            if (string.Equals(c, t, StringComparison.OrdinalIgnoreCase))
                return true;
        }
        return false;
    }

    public static string LabelOrDefault(string? code) => (code?.Trim().ToLowerInvariant()) switch
    {
        "practical" => "Pratik, cabuk hazirlanan",
        "low_calorie" => "Dusuk kalorili",
        "vegetarian" => "Vejetaryen",
        "high_protein" => "Yuksek protein",
        "vegan" => "Vegan",
        "gluten_free" => "Glutensiz",
        _ => "Genel dengeli"
    };
}
