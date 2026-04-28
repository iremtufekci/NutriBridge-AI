using MongoDB.Bson;
using MongoDB.Driver;
using Microsoft.Extensions.Configuration;
using Nightbrate.Core.Entities;

namespace Nightbrate.Infrastructure.Data
{
    public class MongoDbContext
    {
        private readonly IMongoDatabase _database;
        public IMongoCollection<BaseUser> Users => _database.GetCollection<BaseUser>("Users");
        public IMongoCollection<Client> Clients => _database.GetCollection<Client>("Clients");
        public IMongoCollection<Dietitian> Dietitians => _database.GetCollection<Dietitian>("Dietitians");
        public IMongoCollection<MealLog> MealLogs => _database.GetCollection<MealLog>("MealLogs");
        public IMongoCollection<WaterLog> WaterLogs => _database.GetCollection<WaterLog>("WaterLogs");
        public IMongoCollection<WeightLog> WeightLogs => _database.GetCollection<WeightLog>("WeightLogs");
        public IMongoCollection<DietProgram> DietPrograms => _database.GetCollection<DietProgram>("DietPrograms");
        public IMongoCollection<DietProgramHistory> DietProgramHistories => _database.GetCollection<DietProgramHistory>("DietProgramHistories");
        public IMongoCollection<ActivityLog> ActivityLogs => _database.GetCollection<ActivityLog>("ActivityLogs");
        public IMongoCollection<KitchenChefRecipeLog> KitchenChefRecipeLogs => _database.GetCollection<KitchenChefRecipeLog>("KitchenChefRecipeLogs");
        public IMongoCollection<CriticalAlertAcknowledgment> CriticalAlertAcknowledgments => _database.GetCollection<CriticalAlertAcknowledgment>("CriticalAlertAcknowledgments");
        public IMongoCollection<DietitianDailyTask> DietitianDailyTasks => _database.GetCollection<DietitianDailyTask>("DietitianDailyTasks");

        /// <summary>BSON anahtar adlari (Pascal / camel) farkli olabilen alanlar icin ham okuma.</summary>
        public IMongoCollection<BsonDocument> DietitiansBson => _database.GetCollection<BsonDocument>("Dietitians");

        public IMongoCollection<BsonDocument> UsersBson => _database.GetCollection<BsonDocument>("Users");

        public MongoDbContext(IConfiguration configuration)
        {
            var client = new MongoClient(configuration.GetConnectionString("MongoDb"));
            _database = client.GetDatabase(configuration["MongoDbSettings:DatabaseName"] ?? "NutriBridgeDb");
        }
    }
}