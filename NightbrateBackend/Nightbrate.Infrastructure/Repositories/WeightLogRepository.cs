using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class WeightLogRepository(MongoDbContext context) : IWeightLogRepository
{
    public Task AddAsync(WeightLog weightLog) => context.WeightLogs.InsertOneAsync(weightLog);
}
