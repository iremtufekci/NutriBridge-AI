using MongoDB.Driver; // MongoDB sürücü API'si
using Nightbrate.Application.Interfaces; // IActivityLogRepository arayüzü
using Nightbrate.Core.Entities; // ActivityLog varlığı
using Nightbrate.Infrastructure.Data; // MongoDbContext

namespace Nightbrate.Infrastructure.Repositories; // Veri erişim katmanı depoları

public class ActivityLogRepository(MongoDbContext context) : IActivityLogRepository // Aktivite günlüğü erişimi
{
    public Task AddAsync(ActivityLog log) => context.ActivityLogs.InsertOneAsync(log); // Yeni aktivite kaydı ekle

    public async Task<List<ActivityLog>> GetRecentAsync(int take) // Son aktiviteleri getir
    {
        if (take < 1) take = 20; // Minimum 20
        if (take > 100) take = 100; // Maksimum 100
        return await context.ActivityLogs
            .Find(_ => true) // Tüm kayıtlar
            .SortByDescending(x => x.CreatedAt) // En yeni önce
            .Limit(take) // Kayıt sınırı
            .ToListAsync();
    }

    public async Task<List<ActivityLog>> GetByUserIdAsync(string userId, int take) // Kullanıcıya ait aktiviteler
    {
        if (string.IsNullOrWhiteSpace(userId)) return new List<ActivityLog>(); // Geçersiz id
        if (take < 1) take = 20; // Minimum 20
        if (take > 200) take = 200; // Maksimum 200
        return await context.ActivityLogs
            .Find(x => x.UserId == userId) // Kullanıcı filtresi
            .SortByDescending(x => x.CreatedAt) // En yeni önce
            .Limit(take)
            .ToListAsync();
    }

    public async Task<Dictionary<string, DateTime>> GetLastActivityByUserIdsAsync(IReadOnlyList<string> userIds) // Kullanıcı başına son aktivite zamanı
    {
        if (userIds.Count == 0) return new Dictionary<string, DateTime>(); // Boş liste
        var set = new HashSet<string>(userIds.Where(s => !string.IsNullOrWhiteSpace(s))!); // Geçerli id kümesi
        if (set.Count == 0) return new Dictionary<string, DateTime>();

        var filter = Builders<ActivityLog>.Filter.In(x => x.UserId, set); // Çoklu kullanıcı filtresi
        var logs = await context.ActivityLogs.Find(filter).ToListAsync(); // Tüm ilgili loglar
        var dict = new Dictionary<string, DateTime>(StringComparer.Ordinal); // Son aktivite sözlüğü
        foreach (var l in logs) // Her log için
        {
            if (string.IsNullOrEmpty(l.UserId)) continue; // UserId yok
            if (!set.Contains(l.UserId)) continue; // İstenen kümede değil
            if (!dict.TryGetValue(l.UserId, out var prev) || l.CreatedAt > prev) // Daha yeni mi
                dict[l.UserId] = l.CreatedAt; // Son aktiviteyi güncelle
        }
        return dict; // Kullanıcı id → son aktivite zamanı
    }
}
