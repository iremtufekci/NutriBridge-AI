namespace Nightbrate.Application.Options;

/// <summary>Groq Cloud API (gsk_ anahtar). Gemini yerine veya ondan önce kullanılabilir.</summary>
public class GroqAiOptions
{
    public string ApiKey { get; set; } = string.Empty;

    /// <summary>Yemek fotoğrafı analizi (çok modlu).</summary>
    public string VisionModel { get; set; } = "meta-llama/llama-4-scout-17b-16e-instruct";

    /// <summary>Tarif üretimi ve PDF metin analizi.</summary>
    public string TextModel { get; set; } = "llama-3.3-70b-versatile";
}
