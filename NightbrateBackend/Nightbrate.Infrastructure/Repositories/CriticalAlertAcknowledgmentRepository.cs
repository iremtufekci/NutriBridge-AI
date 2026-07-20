using MongoDB.Bson; // ObjectId üretimi
using MongoDB.Driver; // MongoDB sürücü API'si
using Nightbrate.Application.Interfaces; // ICriticalAlertAcknowledgmentRepository arayüzü
using Nightbrate.Core.Entities; // CriticalAlertAcknowledgment varlığı
using Nightbrate.Infrastructure.Data; // MongoDbContext

namespace Nightbrate.Infrastructure.Repositories; // Veri erişim katmanı depoları

public class CriticalAlertAcknowledgmentRepository(MongoDbContext context) : ICriticalAlertAcknowledgmentRepository // Kritik uyarı onay kaydı erişimi
{
    public async Task<IReadOnlyList<CriticalAlertAcknowledgment>> GetByDietitianIdAsync(string dietitianId, CancellationToken cancellationToken = default) // Diyetisyenin onayları
    {
        return await context.CriticalAlertAcknowledgments
            .Find(x => x.DietitianId == dietitianId) // Diyetisyen filtresi
            .ToListAsync(cancellationToken);
    }

    public async Task AddAsync(CriticalAlertAcknowledgment doc, CancellationToken cancellationToken = default) // Yeni onay kaydı ekle
    {
        if (string.IsNullOrEmpty(doc.Id)) // Id yoksa
            doc.Id = ObjectId.GenerateNewId().ToString(); // Yeni id üret
        await context.CriticalAlertAcknowledgments.InsertOneAsync(doc, cancellationToken: cancellationToken);
    }
}
