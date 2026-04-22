using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class WaterLogRepository(MongoDbContext context) : IWaterLogRepository
{
    public Task AddAsync(WaterLog waterLog) => context.WaterLogs.InsertOneAsync(waterLog);
}
