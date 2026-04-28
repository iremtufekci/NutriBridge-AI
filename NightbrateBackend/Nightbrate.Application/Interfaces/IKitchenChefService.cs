using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface IKitchenChefService
{
    Task<KitchenChefResponseDto> GenerateRecipesAsync(KitchenChefRequestDto request, CancellationToken cancellationToken = default);
}
