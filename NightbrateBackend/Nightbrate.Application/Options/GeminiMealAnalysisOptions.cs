namespace Nightbrate.Application.Options;

/// <summary>Google AI (Gemini) ile ogun fotografi analizi. ApiKey bos ise MockMealAnalysisService kullanilir.</summary>
public class GeminiMealAnalysisOptions
{
    public string ApiKey { get; set; } = string.Empty;

    /// <summary>Guncel kararli cok modlu model: gemini-2.5-flash; dusuk maliyet: gemini-2.5-flash-lite</summary>
    public string Model { get; set; } = "gemini-2.5-flash";
}
