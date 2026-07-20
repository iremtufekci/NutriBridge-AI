using MongoDB.Bson; // ObjectId üretimi
using MongoDB.Driver; // MongoDB sürücü API'si
using Nightbrate.Application.Interfaces; // IMealLogRepository arayüzü
using Nightbrate.Core.Entities; // MealLog varlığı
using Nightbrate.Infrastructure.Data; // MongoDbContext

namespace Nightbrate.Infrastructure.Repositories; // Veri erişim katmanı depoları

public class MealLogRepository(MongoDbContext context) : IMealLogRepository // Öğün kaydı koleksiyonu erişimi
{
    public async Task AddAsync(MealLog mealLog) // Yeni öğün kaydı ekle
    {
        if (string.IsNullOrEmpty(mealLog.Id)) // Id yoksa
            mealLog.Id = ObjectId.GenerateNewId().ToString(); // Yeni ObjectId üret
        await context.MealLogs.InsertOneAsync(mealLog); // Koleksiyona ekle
    }

    public Task<MealLog?> GetLastByClientIdAsync(string clientId) => // Danışanın son öğün kaydı
        context.MealLogs.Find(x => x.ClientId == clientId)
            .SortByDescending(x => x.Timestamp) // En yeni önce
            .FirstOrDefaultAsync()!;

    public async Task<IReadOnlyList<MealLog>> GetByClientIdAsync(string clientId, int take, CancellationToken cancellationToken = default) // Danışanın öğün geçmişi
    {
        if (string.IsNullOrWhiteSpace(clientId)) return Array.Empty<MealLog>(); // Geçersiz id
        var limit = Math.Clamp(take, 1, 100); // Limit 1-100 arası
        return await context.MealLogs
            .Find(x => x.ClientId == clientId) // Danışan filtresi
            .SortByDescending(x => x.Timestamp) // Yeniden eskiye
            .Limit(limit) // Kayıt sınırı
            .ToListAsync(cancellationToken);
    }

    public async Task<IReadOnlyList<MealLog>> GetByClientIdsAsync( // Birden fazla danışanın öğün kayıtları
        IReadOnlyCollection<string> clientIds,
        int take,
        CancellationToken cancellationToken = default)
    {
        if (clientIds.Count == 0) return Array.Empty<MealLog>(); // Boş liste
        var limit = Math.Clamp(take, 1, 200); // Limit 1-200 arası
        var f = Builders<MealLog>.Filter.In(m => m.ClientId, clientIds); // Çoklu danışan filtresi
        return await context.MealLogs
            .Find(f)
            .SortByDescending(x => x.Timestamp) // Yeniden eskiye
            .Limit(limit)
            .ToListAsync(cancellationToken);
    }

    public async Task<List<MealLog>> GetByClientIdsInTimestampRangeAsync( // Tarih aralığında öğün kayıtları
        IReadOnlyCollection<string> clientIds,
        DateTime fromUtcInclusive,
        DateTime toUtcExclusive,
        CancellationToken cancellationToken = default)
    {
        if (clientIds.Count == 0) return new List<MealLog>(); // Boş liste
        var f = Builders<MealLog>.Filter.In(m => m.ClientId, clientIds) // Danışan filtresi
                & Builders<MealLog>.Filter.Gte(m => m.Timestamp, fromUtcInclusive) // Başlangıç dahil
                & Builders<MealLog>.Filter.Lt(m => m.Timestamp, toUtcExclusive); // Bitiş hariç
        return await context.MealLogs.Find(f).ToListAsync(cancellationToken);
    }
}
