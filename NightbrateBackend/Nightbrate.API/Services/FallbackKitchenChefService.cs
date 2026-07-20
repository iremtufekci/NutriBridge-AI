using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Services;
using Nightbrate.Application.Utils;

namespace Nightbrate.API.Services;

public sealed class FallbackKitchenChefService(
    IKitchenChefService primary,
    MockKitchenChefService mock,
    ILogger<FallbackKitchenChefService> logger) : IKitchenChefService
{
    public async Task<KitchenChefResponseDto> GenerateRecipesAsync(KitchenChefRequestDto request, CancellationToken cancellationToken = default)
    {
        try
        {
            return await primary.GenerateRecipesAsync(request, cancellationToken).ConfigureAwait(false);
        }
        catch (Exception ex) when (AiNetworkExceptionHelper.IsNetworkOrDnsFailure(ex))
        {
            logger.LogWarning(ex, "Mutfak şefi AI erisilemedi; mock kullaniliyor.");
            var r = await mock.GenerateRecipesAsync(request, cancellationToken).ConfigureAwait(false);
            r.Source = "mock_network";
            return r;
        }
    }
}
