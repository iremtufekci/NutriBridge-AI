using System.Diagnostics;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.API.Monitoring;

public sealed class RequestMetricsMiddleware(RequestDelegate next, IRequestMetricsBuffer buffer)
{
    public async Task InvokeAsync(HttpContext context)
    {
        var path = context.Request.Path.Value ?? "/";
        if (ShouldSkip(path))
        {
            await next(context);
            return;
        }

        var sw = Stopwatch.StartNew();
        try
        {
            await next(context);
        }
        finally
        {
            sw.Stop();
            var key = NormalizePath(path);
            buffer.Record(new RequestMetricEvent(
                DateTime.UtcNow,
                key,
                context.Request.Method,
                context.Response.StatusCode,
                sw.ElapsedMilliseconds));
        }
    }

    private static bool ShouldSkip(string path)
    {
        if (string.IsNullOrEmpty(path)) return true;
        if (path.StartsWith("/swagger", StringComparison.OrdinalIgnoreCase)) return true;
        return false;
    }

    private static string NormalizePath(string path)
    {
        var parts = path.Split('/', StringSplitOptions.RemoveEmptyEntries);
        if (parts.Length == 0) return "/";
        var sb = new System.Text.StringBuilder();
        sb.Append('/');
        for (var i = 0; i < parts.Length; i++)
        {
            if (i >= 5) break;
            if (i > 0) sb.Append('/');
            var p = parts[i];
            if (LooksLikeId(p)) p = "{id}";
            sb.Append(p);
        }
        if (parts.Length > 5) sb.Append("/*");
        return sb.ToString();
    }

    private static bool LooksLikeId(string p)
    {
        if (p.Length is >= 20 and <= 28 && p.All(c => char.IsAsciiLetterOrDigit(c))) return true;
        if (p.Length is 36 && p.Count(c => c == '-') == 4) return true;
        if (p.All(char.IsDigit) && p.Length is >= 1 and <= 18) return true;
        return false;
    }
}
