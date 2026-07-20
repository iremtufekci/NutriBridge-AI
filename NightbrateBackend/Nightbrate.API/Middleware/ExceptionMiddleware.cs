using System.Net; // HTTP durum kodları
using System.Text.Json; // Hata cevabını JSON'a çevir
using Nightbrate.Application.Exceptions; // AppException = iş kuralı hatası
using Nightbrate.Application.Utils; // Ağ/DNS hata ayırt etme

namespace Nightbrate.API.Middleware;

public class ExceptionMiddleware(RequestDelegate next) // Pipeline'daki sonraki middleware
{
    public async Task InvokeAsync(HttpContext context) // Her HTTP isteğinde çalışır
    {
        try
        {
            await next(context); // Controller'a kadar ilerle
        }
        catch (Exception ex) // Yakalanmamış tüm hatalar
        {
            context.Response.ContentType = "application/json"; // JSON hata gövdesi
            context.Response.StatusCode = ex is AppException // İş kuralı hatası → 400
                ? (int)HttpStatusCode.BadRequest
                : (int)HttpStatusCode.InternalServerError; // Beklenmeyen → 500

            var message = ex is AppException
                ? ex.Message
                : AiNetworkExceptionHelper.IsNetworkOrDnsFailure(ex)
                    ? "Yapay zeka sunucusuna (Groq/Gemini) bağlanılamadı. İnternet veya DNS ayarlarınızı kontrol edin."
                    : ex.Message;

            var response = new
            {
                message,
                traceId = context.TraceIdentifier // Log izleme kimliği
            };
            await context.Response.WriteAsync(JsonSerializer.Serialize(response)); // Gövdeyi yaz
        }
    }
}
