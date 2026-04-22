using MongoDB.Driver;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class DietProgramRepository(MongoDbContext context) : IDietProgramRepository
{
    public Task<DietProgram?> GetByClientAndDayAsync(string clientId, string dayOfWeek) =>
        context.DietPrograms.Find(x => x.ClientId == clientId && x.DayOfWeek == dayOfWeek).FirstOrDefaultAsync()!;

    public Task UpsertAsync(DietProgram dietProgram) =>
        context.DietPrograms.ReplaceOneAsync(
            x => x.ClientId == dietProgram.ClientId
                 && x.DayOfWeek == dietProgram.DayOfWeek
                 && x.DietitianId == dietProgram.DietitianId,
            dietProgram,
            new ReplaceOptions { IsUpsert = true });
}
