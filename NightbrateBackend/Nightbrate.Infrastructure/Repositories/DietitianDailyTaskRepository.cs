using MongoDB.Bson; // ObjectId üretimi
using MongoDB.Driver; // MongoDB sürücü API'si
using Nightbrate.Application.Interfaces; // IDietitianDailyTaskRepository arayüzü
using Nightbrate.Core.Entities; // DietitianDailyTask varlığı
using Nightbrate.Infrastructure.Data; // MongoDbContext

namespace Nightbrate.Infrastructure.Repositories; // Veri erişim katmanı depoları

public class DietitianDailyTaskRepository(MongoDbContext context) : IDietitianDailyTaskRepository // Diyetisyen günlük görev erişimi
{
    public async Task<IReadOnlyList<DietitianDailyTask>> GetByDietitianAndTaskDateAsync( // Belirli günün görevleri
        string dietitianId,
        string taskDateYmd,
        CancellationToken cancellationToken = default)
    {
        var list = await context.DietitianDailyTasks
            .Find(x => x.DietitianId == dietitianId && x.TaskDate == taskDateYmd) // Diyetisyen + tarih filtresi
            .SortBy(x => x.SortPriority) // Önceliğe göre
            .ThenBy(x => x.Title) // Sonra başlığa göre
            .ToListAsync(cancellationToken);
        return list;
    }

    public Task<DietitianDailyTask?> GetByIdAsync(string id, CancellationToken cancellationToken = default) => // Id ile görev bul
        context.DietitianDailyTasks.Find(x => x.Id == id).FirstOrDefaultAsync(cancellationToken)!;

    public async Task InsertAsync(DietitianDailyTask task, CancellationToken cancellationToken = default) // Yeni görev ekle
    {
        if (string.IsNullOrEmpty(task.Id)) // Id yoksa
            task.Id = ObjectId.GenerateNewId().ToString(); // Yeni id üret
        await context.DietitianDailyTasks.InsertOneAsync(task, cancellationToken: cancellationToken);
    }

    public Task UpdateContentAsync( // Görev içeriğini güncelle
        string id,
        string title,
        string subtitle,
        int sortPriority,
        DateTime updatedAtUtc,
        CancellationToken cancellationToken = default)
    {
        var u = Builders<DietitianDailyTask>.Update // Güncellenecek alanlar
            .Set(x => x.Title, title)
            .Set(x => x.Subtitle, subtitle)
            .Set(x => x.SortPriority, sortPriority)
            .Set(x => x.UpdatedAtUtc, updatedAtUtc);
        return context.DietitianDailyTasks.UpdateOneAsync(x => x.Id == id, u, cancellationToken: cancellationToken);
    }

    public Task UpdateCompletionAsync( // Görev tamamlanma durumunu güncelle
        string id,
        bool isCompleted,
        DateTime? completedAtUtc,
        DateTime updatedAtUtc,
        CancellationToken cancellationToken = default)
    {
        var u = Builders<DietitianDailyTask>.Update // Tamamlanma alanları
            .Set(x => x.IsCompleted, isCompleted)
            .Set(x => x.CompletedAtUtc, completedAtUtc)
            .Set(x => x.UpdatedAtUtc, updatedAtUtc);
        return context.DietitianDailyTasks.UpdateOneAsync(x => x.Id == id, u, cancellationToken: cancellationToken);
    }

    public Task DeleteManyByIdsAsync(IReadOnlyCollection<string> ids, CancellationToken cancellationToken = default) // Toplu görev silme
    {
        if (ids.Count == 0) return Task.CompletedTask; // Silinecek id yok
        return context.DietitianDailyTasks.DeleteManyAsync(x => ids.Contains(x.Id!), cancellationToken);
    }
}
