using MongoDB.Driver; // MongoDB sürücü API'si
using Nightbrate.Application.Exceptions; // AppException
using Nightbrate.Application.Interfaces; // IClientRepository arayüzü
using Nightbrate.Core.Entities; // Client varlığı
using Nightbrate.Infrastructure.Data; // MongoDbContext

namespace Nightbrate.Infrastructure.Repositories; // Veri erişim katmanı depoları

public class ClientRepository(MongoDbContext context) : IClientRepository // Danışan koleksiyonu erişimi
{
    public Task AddAsync(Client client) => context.Clients.InsertOneAsync(client); // Yeni danışan ekle

    public Task<Client?> GetByIdAsync(string id) => // Id ile danışan bul
        context.Clients.Find(x => x.Id == id).FirstOrDefaultAsync()!;

    public async Task UpdateAsync(Client client) // Danışan profilini güncelle
    {
        if (string.IsNullOrWhiteSpace(client.Id)) throw new AppException("Gecerli danisan profili yok (Id)."); // Id zorunlu
        var r = await context.Clients.ReplaceOneAsync(x => x.Id == client.Id, client); // Belgeyi değiştir
        if (r.MatchedCount == 0) // Kayıt bulunamadı
            throw new AppException("Danisan profili guncellenemedi: veritabaninda 'Clients' kaydi bulunamadi. Lutfen destekle iletisin.");
    }

    public Task<List<Client>> GetByDietitianIdAsync(string dietitianId) => // Diyetisyene bağlı danışanlar
        context.Clients.Find(x => x.DietitianId == dietitianId).ToListAsync();

    public Task<List<Client>> GetByDietitianIdSortedAsync(string dietitianId, bool firstLastAscending) // Sıralı danışan listesi
    {
        var filter = Builders<Client>.Filter.Eq(x => x.DietitianId, dietitianId); // Diyetisyen filtresi
        var sort = firstLastAscending // Ad-soyada göre sıralama yönü
            ? Builders<Client>.Sort.Ascending(c => c.FirstName).Ascending(c => c.LastName)
            : Builders<Client>.Sort.Descending(c => c.FirstName).Descending(c => c.LastName);
        return context.Clients.Find(filter).Sort(sort).ToListAsync(); // Sorgu çalıştır
    }

    public Task<long> GetTotalAsync() => // Toplam danışan sayısı
        context.Clients.CountDocumentsAsync(Builders<Client>.Filter.Empty);

    public async Task<bool> TryAssignDietitianIfUnassignedAsync(string clientId, string dietitianId) // Diyetisyensiz danışana atama dene
    {
        var hasNoDietitian = Builders<Client>.Filter.Or( // Diyetisyeni olmayan danışan filtresi
            Builders<Client>.Filter.Eq(c => c.DietitianId, (string?)null),
            Builders<Client>.Filter.Eq(c => c.DietitianId, string.Empty)
        );
        var filter = Builders<Client>.Filter.Eq(c => c.Id, clientId) & hasNoDietitian; // Id + diyetisyensiz
        var update = Builders<Client>.Update.Set(c => c.DietitianId, dietitianId); // Diyetisyen ata
        var result = await context.Clients.UpdateOneAsync(filter, update); // Koşullu güncelleme
        return result.ModifiedCount == 1; // Başarılı mı (tam 1 kayıt güncellendi)
    }
}
