using Nightbrate.Application.Interfaces; // IWeightLogRepository arayüzü
using Nightbrate.Core.Entities; // WeightLog varlığı
using Nightbrate.Infrastructure.Data; // MongoDbContext

namespace Nightbrate.Infrastructure.Repositories; // Veri erişim katmanı depoları

public class WeightLogRepository(MongoDbContext context) : IWeightLogRepository // Kilo kaydı erişimi
{
    public Task AddAsync(WeightLog weightLog) => context.WeightLogs.InsertOneAsync(weightLog); // Yeni kilo kaydı ekle
}
