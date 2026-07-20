using System.Linq; // LINQ sorguları
using MongoDB.Bson; // ObjectId üretimi
using MongoDB.Driver; // MongoDB sürücü API'si
using Nightbrate.Application.Interfaces; // IDietProgramRepository arayüzü
using Nightbrate.Core.Entities; // DietProgram varlığı
using Nightbrate.Infrastructure.Data; // MongoDbContext

namespace Nightbrate.Infrastructure.Repositories; // Veri erişim katmanı depoları

public class DietProgramRepository(MongoDbContext context) : IDietProgramRepository // Diyet programı erişimi
{
    public Task<DietProgram?> GetByDietitianClientAndProgramDateAsync(string dietitianId, string clientId, string programDate) => // Belirli gün programı
        context.DietPrograms
            .Find(x => x.DietitianId == dietitianId && x.ClientId == clientId && x.ProgramDate == programDate)
            .FirstOrDefaultAsync()!;

    public async Task<List<string>> GetProgramDatesByDietitianAndClientAsync(string dietitianId, string clientId) // Danışanın program günleri
    {
        var filter = // Diyetisyen + danışan filtresi
            Builders<DietProgram>.Filter.Eq(x => x.DietitianId, dietitianId)
            & Builders<DietProgram>.Filter.Eq(x => x.ClientId, clientId);
        var dates = await context.DietPrograms
            .Find(filter)
            .Project(x => x.ProgramDate) // Sadece tarih alanını al
            .ToListAsync();
        return dates.Where(s => !string.IsNullOrWhiteSpace(s)).Distinct().OrderBy(s => s).ToList(); // Benzersiz sıralı tarihler
    }

    public Task<List<DietProgram>> GetAllByClientIdAsync(string clientId) => // Danışanın tüm programları
        context.DietPrograms.Find(x => x.ClientId == clientId).ToListAsync();

    public async Task<DietProgram?> GetCurrentByClientIdAndProgramDateAsync(string clientId, string programDate) // Güncel program (en son güncellenen)
    {
        return await context.DietPrograms
            .Find(x => x.ClientId == clientId && x.ProgramDate == programDate)
            .SortByDescending(x => x.UpdatedAt) // En son güncelleme
            .FirstOrDefaultAsync();
    }

    public async Task UpsertAsync(DietProgram dietProgram) // Program ekle veya güncelle
    {
        var filter = // Benzersiz anahtar: danışan + tarih + diyetisyen
            Builders<DietProgram>.Filter.Eq(x => x.ClientId, dietProgram.ClientId)
            & Builders<DietProgram>.Filter.Eq(x => x.ProgramDate, dietProgram.ProgramDate)
            & Builders<DietProgram>.Filter.Eq(x => x.DietitianId, dietProgram.DietitianId);

        // Güncellemede yedek belgedeki _id, Replace edilen gövdeyle aynı olmalı; aksi halde Code 66 (immutable _id).
        var inDb = await context.DietPrograms.Find(filter).FirstOrDefaultAsync(); // Mevcut kayıt var mı
        if (inDb is not null)
        {
            dietProgram.Id = inDb.Id; // Mevcut id'yi koru
        }
        else if (string.IsNullOrEmpty(dietProgram.Id))
        {
            // Sadece gerçekten yeni ekleme: Id yok → E11000 _id: null cakismasini onler.
            dietProgram.Id = ObjectId.GenerateNewId().ToString(); // Yeni id üret
        }

        await context.DietPrograms.ReplaceOneAsync(filter, dietProgram, new ReplaceOptions { IsUpsert = true }); // Upsert
    }

    public async Task<List<DietProgram>> GetByDietitianClientsAndProgramDatesAsync( // Toplu program sorgusu
        string dietitianId,
        IReadOnlyCollection<string> clientIds,
        IReadOnlyCollection<string> programDates,
        CancellationToken cancellationToken = default)
    {
        if (clientIds.Count == 0 || programDates.Count == 0) return new List<DietProgram>(); // Boş parametre
        var f = // Çoklu filtre
            Builders<DietProgram>.Filter.Eq(x => x.DietitianId, dietitianId)
            & Builders<DietProgram>.Filter.In(x => x.ClientId, clientIds)
            & Builders<DietProgram>.Filter.In(x => x.ProgramDate, programDates);
        return await context.DietPrograms.Find(f).ToListAsync(cancellationToken);
    }
}
