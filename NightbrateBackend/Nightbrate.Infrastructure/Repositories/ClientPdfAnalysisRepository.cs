using MongoDB.Bson;
using MongoDB.Driver;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class ClientPdfAnalysisRepository(MongoDbContext context) : IClientPdfAnalysisRepository
{
    public async Task InsertAsync(ClientPdfAnalysis doc, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrEmpty(doc.Id))
            doc.Id = ObjectId.GenerateNewId().ToString();
        await context.ClientPdfAnalyses.InsertOneAsync(doc, cancellationToken: cancellationToken);
    }

    public Task<ClientPdfAnalysis?> GetByIdAsync(string id, CancellationToken cancellationToken = default) =>
        context.ClientPdfAnalyses.Find(x => x.Id == id).FirstOrDefaultAsync(cancellationToken)!;

    public async Task<IReadOnlyList<ClientPdfAnalysis>> GetByClientIdAsync(string clientId, int take, CancellationToken cancellationToken = default)
    {
        var list = await context.ClientPdfAnalyses
            .Find(x => x.ClientId == clientId)
            .SortByDescending(x => x.CreatedAtUtc)
            .Limit(Math.Clamp(take, 1, 200))
            .ToListAsync(cancellationToken);
        return list;
    }
}
