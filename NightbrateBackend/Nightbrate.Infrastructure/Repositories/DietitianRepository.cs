using MongoDB.Bson; // BSON belge erişimi
using MongoDB.Driver; // MongoDB sürücü API'si
using Nightbrate.Application.Interfaces; // IDietitianRepository arayüzü
using Nightbrate.Core.Entities; // Dietitian varlığı
using Nightbrate.Infrastructure.Data; // MongoDbContext

namespace Nightbrate.Infrastructure.Repositories; // Veri erişim katmanı depoları

public class DietitianRepository(MongoDbContext context) : IDietitianRepository // Diyetisyen koleksiyonu erişimi
{
    public Task AddAsync(Dietitian dietitian) => context.Dietitians.InsertOneAsync(dietitian); // Yeni diyetisyen ekle

    public Task<List<Dietitian>> GetPendingAsync() => // Onay bekleyen diyetisyenler
        context.Dietitians.Find(x => !x.IsApproved).ToListAsync();

    public Task<Dietitian?> GetByIdAsync(string id) => // Id ile diyetisyen bul
        context.Dietitians.Find(x => x.Id == id).FirstOrDefaultAsync()!;

    public Task<Dietitian?> GetByEmailAsync(string email) // E-posta ile diyetisyen bul
    {
        if (string.IsNullOrWhiteSpace(email)) return Task.FromResult<Dietitian?>(null); // Boş e-posta
        var e = email.Trim().ToLowerInvariant(); // Normalize et
        return context.Dietitians.Find(x => x.Email == e).FirstOrDefaultAsync()!; // Sorgula
    }

    public Task UpdateAsync(Dietitian dietitian) => // Diyetisyen kaydını güncelle
        context.Dietitians.ReplaceOneAsync(x => x.Id == dietitian.Id, dietitian);

    public Task<long> GetTotalAsync() => // Toplam diyetisyen sayısı
        context.Dietitians.CountDocumentsAsync(Builders<Dietitian>.Filter.Empty);

    public Task<long> GetApprovedCountAsync() => // Onaylı diyetisyen sayısı
        context.Dietitians.CountDocumentsAsync(x => x.IsApproved);

    public async Task<bool> ConnectionCodeExistsAsync(string connectionCode) => // Bağlantı kodu kullanımda mı
        await context.Dietitians.CountDocumentsAsync(x => x.ConnectionCode == connectionCode) > 0;

    public Task<Dietitian?> GetApprovedByConnectionCodeAsync(string connectionCode) => // Onaylı diyetisyeni kod ile bul
        context.Dietitians
            .Find(x => x.IsApproved && x.ConnectionCode == connectionCode)
            .FirstOrDefaultAsync()!;

    public async Task<string?> GetConnectionCodeByDietitianIdRawAsync(string dietitianId) // Ham BSON'dan bağlantı kodu
    {
        if (string.IsNullOrWhiteSpace(dietitianId)) return null; // Geçersiz id
        if (!ObjectId.TryParse(dietitianId, out var oid)) return null; // ObjectId parse
        var doc = await context.DietitiansBson // Ham BSON koleksiyonu
            .Find(Builders<BsonDocument>.Filter.Eq("_id", oid))
            .FirstOrDefaultAsync();
        if (doc is null) return null; // Belge yok
        foreach (var key in new[] { "ConnectionCode", "connectionCode" }) // Olası alan adları
        {
            if (!doc.Contains(key) || doc[key] is not BsonValue v || v.IsBsonNull) continue;
            if (v.IsString) return v.AsString; // String değer
            return v.ToString() ?? null;
        }
        return null; // Kod bulunamadı
    }
}
