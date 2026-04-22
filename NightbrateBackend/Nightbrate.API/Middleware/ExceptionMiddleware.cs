using System.Net;
using System.Text.Json;
using Nightbrate.Application.Exceptions;

namespace Nightbrate.API.Middleware;

public class ExceptionMiddleware(RequestDelegate next)
{
    public async Task InvokeAsync(HttpContext context)
    {
        try
        {
            await next(context);
        }
        catch (Exception ex)
        {
            context.Response.ContentType = "application/json";
            context.Response.StatusCode = ex is AppException
                ? (int)HttpStatusCode.BadRequest
                : (int)HttpStatusCode.InternalServerError;

            var response = new
            {
                message = ex.Message,
                traceId = context.TraceIdentifier
            };
            await context.Response.WriteAsync(JsonSerializer.Serialize(response));
        }
    }
}
