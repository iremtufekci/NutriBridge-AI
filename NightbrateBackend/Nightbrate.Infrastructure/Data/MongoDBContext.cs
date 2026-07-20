using MongoDB.Bson; // Ham BSON doküman tipi
using MongoDB.Driver; // MongoDB sürücüsü
using Microsoft.Extensions.Configuration; // appsettings.json okuma
using Nightbrate.Core.Entities; // Entity sınıfları

namespace Nightbrate.Infrastructure.Data
{
    /// <summary>MongoDB veritabanına erişim — tüm koleksiyonlar buradan</summary>
    public class MongoDbContext
    {
        private readonly IMongoDatabase _database; // Seçili veritabanı (NutriBridgeDb)
        public IMongoCollection<BaseUser> Users => _database.GetCollection<BaseUser>("Users"); // Tüm kullanıcılar (taban)
        public IMongoCollection<Client> Clients => _database.GetCollection<Client>("Clients"); // Danışan profilleri
        public IMongoCollection<Dietitian> Dietitians => _database.GetCollection<Dietitian>("Dietitians"); // Diyetisyen profilleri
        public IMongoCollection<MealLog> MealLogs => _database.GetCollection<MealLog>("MealLogs"); // Yemek foto analiz kayıtları
        public IMongoCollection<WaterLog> WaterLogs => _database.GetCollection<WaterLog>("WaterLogs"); // Su içme logları
        public IMongoCollection<WeightLog> WeightLogs => _database.GetCollection<WeightLog>("WeightLogs"); // Kilo logları
        public IMongoCollection<DietProgram> DietPrograms => _database.GetCollection<DietProgram>("DietPrograms"); // Günlük programlar
        public IMongoCollection<DietProgramHistory> DietProgramHistories => _database.GetCollection<DietProgramHistory>("DietProgramHistories"); // Arşiv
        public IMongoCollection<ActivityLog> ActivityLogs => _database.GetCollection<ActivityLog>("ActivityLogs"); // Sistem aktiviteleri
        public IMongoCollection<KitchenChefRecipeLog> KitchenChefRecipeLogs => _database.GetCollection<KitchenChefRecipeLog>("KitchenChefRecipeLogs"); // AI tarif paylaşımları
        public IMongoCollection<CriticalAlertAcknowledgment> CriticalAlertAcknowledgments => _database.GetCollection<CriticalAlertAcknowledgment>("CriticalAlertAcknowledgments"); // Okundu işaretleri
        public IMongoCollection<DietitianDailyTask> DietitianDailyTasks => _database.GetCollection<DietitianDailyTask>("DietitianDailyTasks"); // Günlük görevler
        public IMongoCollection<ClientPdfAnalysis> ClientPdfAnalyses => _database.GetCollection<ClientPdfAnalysis>("ClientPdfAnalyses"); // PDF analiz sonuçları

        /// <summary>Alan adı uyumsuzlukları için ham BSON okuma (Dietitians)</summary>
        public IMongoCollection<BsonDocument> DietitiansBson => _database.GetCollection<BsonDocument>("Dietitians");

        public IMongoCollection<BsonDocument> UsersBson => _database.GetCollection<BsonDocument>("Users"); // Ham kullanıcı okuma

        public MongoDbContext(IConfiguration configuration) // DI ile appsettings enjekte edilir
        {
            var client = new MongoClient(configuration.GetConnectionString("MongoDb")); // Atlas bağlantı dizesi
            _database = client.GetDatabase(configuration["MongoDbSettings:DatabaseName"] ?? "NutriBridgeDb"); // DB adı
        }
    }
}
