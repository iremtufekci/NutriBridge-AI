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

        public MongoDbContext(IConfiguration configuration)
        {
            var client = new MongoClient(configuration.GetConnectionString("MongoDb"));
            _database = client.GetDatabase(configuration["MongoDbSettings:DatabaseName"] ?? "NutriBridgeDb");
        }
    }
}