using MongoDB.Bson; // ObjectId üretimi
using MongoDB.Driver; // MongoDB sürücü API'si
using Nightbrate.Application.Interfaces; // IKitchenChefRecipeLogRepository arayüzü
using Nightbrate.Core.Entities; // KitchenChefRecipeLog varlığı
using Nightbrate.Infrastructure.Data; // MongoDbContext

namespace Nightbrate.Infrastructure.Repositories; // Veri erişim katmanı depoları

public class KitchenChefRecipeLogRepository(MongoDbContext context) : IKitchenChefRecipeLogRepository // AI mutfak tarif kaydı erişimi
{
    public async Task AddAsync(KitchenChefRecipeLog log, CancellationToken cancellationToken = default) // Yeni tarif kaydı ekle
    {
        if (string.IsNullOrEmpty(log.Id)) // Id yoksa
            log.Id = ObjectId.GenerateNewId().ToString(); // Yeni id üret
        await context.KitchenChefRecipeLogs.InsertOneAsync(log, cancellationToken: cancellationToken);
    }

    public async Task<IReadOnlyList<KitchenChefRecipeLog>> GetByClientIdAsync(string clientId, int take, CancellationToken cancellationToken = default) // Danışanın tarif geçmişi
    {
        if (take < 1) take = 50; // Minimum 50
        if (take > 200) take = 200; // Maksimum 200
        return await context.KitchenChefRecipeLogs
            .Find(x => x.ClientId == clientId) // Danışan filtresi
            .SortByDescending(x => x.CreatedAtUtc) // En yeni önce
            .Limit(take)
            .ToListAsync(cancellationToken);
    }

    public async Task<IReadOnlyList<KitchenChefRecipeLog>> GetByClientIdFilteredAsync( // Filtreli tarif geçmişi
        string clientId,
        DateTime? fromUtcInclusive,
        DateTime? toUtcExclusive,
        string? sourceFilter,
        int skip,
        int take,
        CancellationToken cancellationToken = default)
    {
        if (take < 1) take = 50; // Limit alt sınırı
        if (take > 500) take = 500; // Limit üst sınırı
        if (skip < 0) skip = 0; // Sayfalama offset

        var builder = Builders<KitchenChefRecipeLog>.Filter; // Filtre oluşturucu
        var f = builder.Eq(x => x.ClientId, clientId); // Danışan filtresi
        if (fromUtcInclusive.HasValue) // Başlangıç tarihi varsa
            f &= builder.Gte(x => x.CreatedAtUtc, fromUtcInclusive.Value);
        if (toUtcExclusive.HasValue) // Bitiş tarihi varsa
            f &= builder.Lt(x => x.CreatedAtUtc, toUtcExclusive.Value);
        if (!string.IsNullOrWhiteSpace(sourceFilter) && // Kaynak filtresi
            !string.Equals(sourceFilter, "all", StringComparison.OrdinalIgnoreCase))
        {
            var s = sourceFilter.Trim().ToLowerInvariant(); // Normalize kaynak
            if (s is "groq" or "gemini" or "mock" or "mock_network") // Geçerli kaynak değerleri
                f &= builder.Eq(x => x.Source, s);
        }

        return await context.KitchenChefRecipeLogs
            .Find(f)
            .SortByDescending(x => x.CreatedAtUtc) // En yeni önce
            .Skip(skip) // Sayfalama
            .Limit(take)
            .ToListAsync(cancellationToken);
    }

    public Task<long> CountByClientIdInUtcRangeAsync( // Tarih aralığında tarif sayısı
        string clientId,
        DateTime fromUtcInclusive,
        DateTime toUtcExclusive,
        CancellationToken cancellationToken = default)
    {
        var builder = Builders<KitchenChefRecipeLog>.Filter;
        var f = builder.Eq(x => x.ClientId, clientId) // Danışan filtresi
                & builder.Gte(x => x.CreatedAtUtc, fromUtcInclusive) // Başlangıç dahil
                & builder.Lt(x => x.CreatedAtUtc, toUtcExclusive); // Bitiş hariç
        return context.KitchenChefRecipeLogs.CountDocumentsAsync(f, cancellationToken: cancellationToken);
    }
}
