using MongoDB.Bson; // Ham BSON belge erişimi
using MongoDB.Driver; // MongoDB sürücü ve sorgu API'si
using Nightbrate.Application.DTOs; // MonthlyRegistrationDto vb.
using Nightbrate.Application.Interfaces; // IUserRepository arayüzü
using Nightbrate.Core.Entities; // BaseUser, Client, Dietitian varlıkları
using Nightbrate.Infrastructure.Data; // MongoDbContext

namespace Nightbrate.Infrastructure.Repositories; // Veri erişim katmanı depoları

public class UserRepository(MongoDbContext context) : IUserRepository // Kullanıcı koleksiyonu erişimi
{
    public Task<BaseUser?> GetByEmailAsync(string email) => // E-posta ile kullanıcı bul
        context.Users.Find(x => x.Email == email).FirstOrDefaultAsync()!;

    public async Task<BaseUser?> GetByIdAsync(string id) // Id ile kullanıcı bul (çoklu koleksiyon desteği)
    {
        if (string.IsNullOrWhiteSpace(id)) return null; // Geçersiz id

        var inUsers = await context.Users.Find(x => x.Id == id).FirstOrDefaultAsync()!; // Önce Users koleksiyonu
        if (inUsers is not null) return inUsers; // Bulunduysa döndür

        // Eski/yan koleksiyonlarda sadece duran belgeler icin
        var client = await context.Clients.Find(x => x.Id == id).FirstOrDefaultAsync()!; // Clients koleksiyonuna bak
        if (client is not null) return client; // Danışan bulundu
        return await context.Dietitians.Find(x => x.Id == id).FirstOrDefaultAsync()!; // Dietitians koleksiyonuna bak
    }

    public Task AddAsync(BaseUser user) => context.Users.InsertOneAsync(user); // Yeni kullanıcı ekle

    public Task<long> GetTotalUsersAsync() => // Toplam kullanıcı sayısı
        context.Users.CountDocumentsAsync(Builders<BaseUser>.Filter.Empty);

    public Task<long> CountByRoleAsync(UserRole role) => // Belirli roldeki kullanıcı sayısı
        context.Users.CountDocumentsAsync(x => x.Role == role);

    public async Task<IReadOnlyList<MonthlyRegistrationDto>> GetMonthlyUserRegistrationsAsync(int monthsBack) // Aylık kayıt istatistiği
    {
        if (monthsBack < 1) monthsBack = 6; // Minimum 6 ay
        if (monthsBack > 24) monthsBack = 24; // Maksimum 24 ay

        var now = DateTime.UtcNow; // Şu an (UTC)
        var firstThisMonth = new DateTime(now.Year, now.Month, 1, 0, 0, 0, DateTimeKind.Utc); // Bu ayın ilk günü
        var rangeStart = firstThisMonth.AddMonths(-(monthsBack - 1)); // Aralık başlangıcı

        var filter = Builders<BaseUser>.Filter.Gte(x => x.CreatedAt, rangeStart); // Tarih filtresi
        var all = await context.Users.Find(filter).ToListAsync(); // Kullanıcıları çek
        var counts = new Dictionary<(int y, int m), long>(); // Ay bazlı sayaç
        foreach (var u in all) // Her kullanıcı için
        {
            var t = u.CreatedAt.Kind == DateTimeKind.Unspecified // Tarih UTC mi kontrol et
                ? DateTime.SpecifyKind(u.CreatedAt, DateTimeKind.Utc)
                : u.CreatedAt.ToUniversalTime();
            var key = (t.Year, t.Month); // Yıl-ay anahtarı
            counts[key] = counts.GetValueOrDefault(key) + 1; // Sayacı artır
        }

        var list = new List<MonthlyRegistrationDto>(monthsBack); // Sonuç listesi
        for (var i = 0; i < monthsBack; i++) // Her ay için
        {
            var d = rangeStart.AddMonths(i); // Ay tarihi
            list.Add(new MonthlyRegistrationDto // DTO ekle
            {
                Year = d.Year, // Yıl
                Month = d.Month, // Ay
                Count = counts.GetValueOrDefault((d.Year, d.Month), 0L) // Kayıt sayısı
            });
        }
        return list; // Aylık istatistik listesi
    }

    public Task SetDietitianIsApprovedInUsersCollectionAsync(string dietitianId, bool isApproved) // Users'da onay durumu güncelle
    {
        var filter = Builders<BaseUser>.Filter.Eq(x => x.Id, dietitianId); // Diyetisyen filtresi
        var update = Builders<BaseUser>.Update.Set(nameof(Dietitian.IsApproved), isApproved); // IsApproved alanı
        return context.Users.UpdateOneAsync(filter, update); // Güncelle
    }

    public Task SetDietitianConnectionCodeInUsersCollectionAsync(string dietitianId, string connectionCode) // Bağlantı kodu güncelle
    {
        var filter = Builders<BaseUser>.Filter.Eq(x => x.Id, dietitianId); // Diyetisyen filtresi
        var update = Builders<BaseUser>.Update.Set(nameof(Dietitian.ConnectionCode), connectionCode); // ConnectionCode alanı
        return context.Users.UpdateOneAsync(filter, update); // Güncelle
    }

    public Task SetClientDietitianIdInUsersCollectionAsync(string clientId, string dietitianId) // Danışana diyetisyen ata
    {
        var filter = Builders<BaseUser>.Filter.And( // Danışan + Client rolü filtresi
            Builders<BaseUser>.Filter.Eq(x => x.Id, clientId),
            Builders<BaseUser>.Filter.Eq(x => x.Role, UserRole.Client)
        );
        var update = Builders<BaseUser>.Update.Set(nameof(Client.DietitianId), dietitianId); // DietitianId alanı
        return context.Users.UpdateOneAsync(filter, update); // Güncelle
    }

    public async Task<string?> GetConnectionCodeFromUsersBsonByUserIdAsync(string userId) // Ham BSON'dan bağlantı kodu oku
    {
        if (string.IsNullOrWhiteSpace(userId)) return null; // Geçersiz id
        if (!MongoDB.Bson.ObjectId.TryParse(userId, out var oid)) return null; // ObjectId parse
        var doc = await context.UsersBson // Ham BSON koleksiyonu
            .Find(Builders<BsonDocument>.Filter.Eq("_id", oid))
            .FirstOrDefaultAsync();
        if (doc is null) return null; // Belge yok
        foreach (var key in new[] { "ConnectionCode", "connectionCode" }) // Olası alan adları
        {
            if (!doc.Contains(key) || doc[key] is not BsonValue v || v.IsBsonNull) continue; // Alan yok veya null
            if (v.IsString) return v.AsString; // String değer
            return v.ToString() ?? null; // Diğer türleri string'e çevir
        }
        return null; // Kod bulunamadı
    }

    public async Task UpdateThemePreferenceAllStoresAsync(string userId, string themePreference) // Tüm koleksiyonlarda tema güncelle
    {
        if (string.IsNullOrWhiteSpace(userId)) return; // Geçersiz id

        var t = string.Equals(themePreference, "dark", StringComparison.OrdinalIgnoreCase) ? "dark" : "light"; // Geçerli tema değeri

        var f = Builders<BaseUser>.Filter.Eq(x => x.Id, userId); // Users filtresi
        var u = Builders<BaseUser>.Update.Set(x => x.ThemePreference, t); // Tema güncelleme
        await context.Users.UpdateOneAsync(f, u); // Users koleksiyonu

        _ = await context.Clients.UpdateOneAsync( // Clients koleksiyonu
            Builders<Client>.Filter.Eq(c => c.Id, userId),
            Builders<Client>.Update.Set(c => c.ThemePreference, t));

        _ = await context.Dietitians.UpdateOneAsync( // Dietitians koleksiyonu
            Builders<Dietitian>.Filter.Eq(d => d.Id, userId),
            Builders<Dietitian>.Update.Set(d => d.ThemePreference, t));
    }

    public Task<List<BaseUser>> GetAllUsersForAdminAsync() => // Admin paneli için tüm kullanıcılar
        context.Users.Find(_ => true).ToListAsync()!;

    public async Task SetUserSuspensionAllStoresAsync(string userId, bool isSuspended, string? message, DateTime? suspendedAt) // Askıya alma durumunu tüm koleksiyonlarda güncelle
    {
        if (string.IsNullOrWhiteSpace(userId)) return; // Geçersiz id

        var fU = Builders<BaseUser>.Filter.Eq(x => x.Id, userId); // Users filtresi
        var uU = Builders<BaseUser>.Update // Askıya alma alanları
            .Set(x => x.IsSuspended, isSuspended)
            .Set(x => x.SuspensionMessage, isSuspended ? message : null)
            .Set(x => x.SuspendedAt, suspendedAt);
        await context.Users.UpdateOneAsync(fU, uU); // Users güncelle

        _ = await context.Clients.UpdateOneAsync( // Clients güncelle
            Builders<Client>.Filter.Eq(c => c.Id, userId),
            Builders<Client>.Update
                .Set(c => c.IsSuspended, isSuspended)
                .Set(c => c.SuspensionMessage, isSuspended ? message : null)
                .Set(c => c.SuspendedAt, suspendedAt));

        _ = await context.Dietitians.UpdateOneAsync( // Dietitians güncelle
            Builders<Dietitian>.Filter.Eq(d => d.Id, userId),
            Builders<Dietitian>.Update
                .Set(d => d.IsSuspended, isSuspended)
                .Set(d => d.SuspensionMessage, isSuspended ? message : null)
                .Set(d => d.SuspendedAt, suspendedAt));
    }

    public Task UpdateClientProfileInUsersCollectionAsync( // Users koleksiyonunda danışan profili güncelle
        string clientId,
        string firstName,
        string lastName,
        double weight,
        double height,
        int targetCalories)
    {
        if (string.IsNullOrWhiteSpace(clientId)) return Task.CompletedTask; // Geçersiz id
        var f = Builders<BaseUser>.Filter.And( // Danışan + Client rolü
            Builders<BaseUser>.Filter.Eq(x => x.Id, clientId),
            Builders<BaseUser>.Filter.Eq(x => x.Role, UserRole.Client));
        var u = Builders<BaseUser>.Update // Profil alanları
            .Set(nameof(Client.FirstName), firstName)
            .Set(nameof(Client.LastName), lastName)
            .Set(nameof(Client.Weight), weight)
            .Set(nameof(Client.Height), height)
            .Set(nameof(Client.TargetCalories), targetCalories);
        return context.Users.UpdateOneAsync(f, u); // Güncelle
    }

    public async Task<(string? FirstName, string? LastName)> GetAdminNameFromUsersBsonAsync(string userId) // Admin ad-soyadını BSON'dan oku
    {
        if (string.IsNullOrWhiteSpace(userId) || !MongoDB.Bson.ObjectId.TryParse(userId, out var oid)) // Id doğrula
            return (null, null);
        var doc = await context.UsersBson // Ham BSON sorgusu
            .Find(Builders<BsonDocument>.Filter.Eq("_id", oid))
            .FirstOrDefaultAsync();
        if (doc is null) return (null, null); // Belge yok

        static string? GetString(BsonDocument d, params string[] keys) // Belgeden string alan oku
        {
            foreach (var key in keys) // Olası alan adlarını dene
            {
                if (!d.Contains(key)) continue; // Alan yok
                var v = d[key];
                if (v is null || v.IsBsonNull) continue; // Null değer
                if (v.IsString) return v.AsString; // String döndür
            }
            return null; // Bulunamadı
        }

        return ( // Ad ve soyadı döndür
            GetString(doc, "firstName", "FirstName", "givenName", "ad"),
            GetString(doc, "lastName", "LastName", "familyName", "surname", "soyad"));
    }
}
