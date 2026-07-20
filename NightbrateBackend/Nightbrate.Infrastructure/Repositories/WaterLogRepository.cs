using MongoDB.Bson; // BSON tipleri
using MongoDB.Driver; // MongoDB sürücü API'si
using Nightbrate.Application.Interfaces; // IWaterLogRepository arayüzü
using Nightbrate.Core.Entities; // WaterLog varlığı
using Nightbrate.Infrastructure.Data; // MongoDbContext

namespace Nightbrate.Infrastructure.Repositories; // Veri erişim katmanı depoları

public class WaterLogRepository(MongoDbContext context) : IWaterLogRepository // Su tüketimi kaydı erişimi
{
    public Task AddAsync(WaterLog waterLog) => context.WaterLogs.InsertOneAsync(waterLog); // Yeni su kaydı ekle

    public async Task<List<WaterLog>> GetByClientIdsInTimestampRangeAsync( // Tarih aralığında su kayıtları
        IReadOnlyCollection<string> clientIds,
        DateTime fromUtcInclusive,
        DateTime toUtcExclusive,
        CancellationToken cancellationToken = default)
    {
        if (clientIds.Count == 0) return new List<WaterLog>(); // Boş danışan listesi
        var f = Builders<WaterLog>.Filter.In(w => w.ClientId, clientIds) // Danışan filtresi
                & Builders<WaterLog>.Filter.Gte(w => w.Timestamp, fromUtcInclusive) // Başlangıç dahil
                & Builders<WaterLog>.Filter.Lt(w => w.Timestamp, toUtcExclusive); // Bitiş hariç
        return await context.WaterLogs.Find(f).ToListAsync(cancellationToken);
    }
}
