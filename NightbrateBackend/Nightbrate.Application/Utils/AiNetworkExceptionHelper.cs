using System.Net.Http;
using System.Net.Sockets;

namespace Nightbrate.Application.Utils;

/// <summary>Gemini HTTP çağrılarında ağ/DNS hatalarını ayırt eder.</summary>
public static class AiNetworkExceptionHelper
{
    public static bool IsNetworkOrDnsFailure(Exception ex)
    {
        for (var current = ex; current is not null; current = current.InnerException)
        {
            if (current is HttpRequestException or SocketException)
                return true;

            var msg = current.Message;
            if (msg.Contains("ana bilgisayar", StringComparison.OrdinalIgnoreCase)
                || msg.Contains("No such host", StringComparison.OrdinalIgnoreCase)
                || msg.Contains("Name or service not known", StringComparison.OrdinalIgnoreCase)
                || msg.Contains("Network is unreachable", StringComparison.OrdinalIgnoreCase)
                || msg.Contains("actively refused", StringComparison.OrdinalIgnoreCase))
            {
                return true;
            }
        }

        return false;
    }
}
