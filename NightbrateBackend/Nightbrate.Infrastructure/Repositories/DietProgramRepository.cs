using System.Linq;
using MongoDB.Bson;
using MongoDB.Driver;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class DietProgramRepository(MongoDbContext context) : IDietProgramRepository
{
    public Task<DietProgram?> GetByDietitianClientAndProgramDateAsync(string dietitianId, string clientId, string programDate) =>
        context.DietPrograms
            .Find(x => x.DietitianId == dietitianId && x.ClientId == clientId && x.ProgramDate == programDate)
            .FirstOrDefaultAsync()!;

    public async Task<List<string>> GetProgramDatesByDietitianAndClientAsync(string dietitianId, string clientId)
    {
        var filter =
            Builders<DietProgram>.Filter.Eq(x => x.DietitianId, dietitianId)
            & Builders<DietProgram>.Filter.Eq(x => x.ClientId, clientId);
        var dates = await context.DietPrograms
            .Find(filter)
            .Project(x => x.ProgramDate)
            .ToListAsync();
        return dates.Where(s => !string.IsNullOrWhiteSpace(s)).Distinct().OrderBy(s => s).ToList();
    }

    public Task<List<DietProgram>> GetAllByClientIdAsync(string clientId) =>
        context.DietPrograms.Find(x => x.ClientId == clientId).ToListAsync();

    public async Task<DietProgram?> GetCurrentByClientIdAndProgramDateAsync(string clientId, string programDate)
    {
        return await context.DietPrograms
            .Find(x => x.ClientId == clientId && x.ProgramDate == programDate)
            .SortByDescending(x => x.UpdatedAt)
            .FirstOrDefaultAsync();
    }

    public async Task UpsertAsync(DietProgram dietProgram)
    {
        var filter =
            Builders<DietProgram>.Filter.Eq(x => x.ClientId, dietProgram.ClientId)
            & Builders<DietProgram>.Filter.Eq(x => x.ProgramDate, dietProgram.ProgramDate)
            & Builders<DietProgram>.Filter.Eq(x => x.DietitianId, dietProgram.DietitianId);

        // Güncellemede yedek belgedeki _id, Replace edilen gövdeyle aynı olmalı; aksi halde Code 66 (immutable _id).
        var inDb = await context.DietPrograms.Find(filter).FirstOrDefaultAsync();
        if (inDb is not null)
        {
            dietProgram.Id = inDb.Id;
        }
        else if (string.IsNullOrEmpty(dietProgram.Id))
        {
            // Sadece gerçekten yeni ekleme: Id yok → E11000 _id: null cakismasini onler.
            dietProgram.Id = ObjectId.GenerateNewId().ToString();
        }

        await context.DietPrograms.ReplaceOneAsync(filter, dietProgram, new ReplaceOptions { IsUpsert = true });
    }
}
