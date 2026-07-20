using MongoDB.Bson; // ObjectId üretimi
using MongoDB.Driver; // MongoDB sürücü API'si
using Nightbrate.Application.Interfaces; // IClientPdfAnalysisRepository arayüzü
using Nightbrate.Core.Entities; // ClientPdfAnalysis varlığı
using Nightbrate.Infrastructure.Data; // MongoDbContext

namespace Nightbrate.Infrastructure.Repositories; // Veri erişim katmanı depoları

public class ClientPdfAnalysisRepository(MongoDbContext context) : IClientPdfAnalysisRepository // PDF analiz kaydı erişimi
{
    public async Task InsertAsync(ClientPdfAnalysis doc, CancellationToken cancellationToken = default) // Yeni analiz kaydı ekle
    {
        if (string.IsNullOrEmpty(doc.Id)) // Id yoksa
            doc.Id = ObjectId.GenerateNewId().ToString(); // Yeni id üret
        await context.ClientPdfAnalyses.InsertOneAsync(doc, cancellationToken: cancellationToken);
    }

    public Task<ClientPdfAnalysis?> GetByIdAsync(string id, CancellationToken cancellationToken = default) => // Id ile analiz bul
        context.ClientPdfAnalyses.Find(x => x.Id == id).FirstOrDefaultAsync(cancellationToken)!;

    public async Task<IReadOnlyList<ClientPdfAnalysis>> GetByClientIdAsync(string clientId, int take, CancellationToken cancellationToken = default) // Danışanın analiz geçmişi
    {
        var list = await context.ClientPdfAnalyses
            .Find(x => x.ClientId == clientId) // Danışan filtresi
            .SortByDescending(x => x.CreatedAtUtc) // En yeni önce
            .Limit(Math.Clamp(take, 1, 200)) // Limit 1-200
            .ToListAsync(cancellationToken);
        return list;
    }
}
