using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IKitchenChefRecipeLogRepository
{
    Task AddAsync(KitchenChefRecipeLog log, CancellationToken cancellationToken = default);
    Task<IReadOnlyList<KitchenChefRecipeLog>> GetByClientIdAsync(string clientId, int take, CancellationToken cancellationToken = default);

    /// <summary>Danışanın paylaşım geçmişi; tarih (UTC) ve kaynak filtreli.</summary>
    Task<IReadOnlyList<KitchenChefRecipeLog>> GetByClientIdFilteredAsync(
        string clientId,
        DateTime? fromUtcInclusive,
        DateTime? toUtcExclusive,
        string? sourceFilter,
        int skip,
        int take,
        CancellationToken cancellationToken = default);
}
