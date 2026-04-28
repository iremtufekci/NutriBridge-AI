using MongoDB.Bson;
using MongoDB.Driver;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class CriticalAlertAcknowledgmentRepository(MongoDbContext context) : ICriticalAlertAcknowledgmentRepository
{
    public async Task<IReadOnlyList<CriticalAlertAcknowledgment>> GetByDietitianIdAsync(string dietitianId, CancellationToken cancellationToken = default)
    {
        return await context.CriticalAlertAcknowledgments
            .Find(x => x.DietitianId == dietitianId)
            .ToListAsync(cancellationToken);
    }

    public async Task AddAsync(CriticalAlertAcknowledgment doc, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrEmpty(doc.Id))
            doc.Id = ObjectId.GenerateNewId().ToString();
        await context.CriticalAlertAcknowledgments.InsertOneAsync(doc, cancellationToken: cancellationToken);
    }
}
