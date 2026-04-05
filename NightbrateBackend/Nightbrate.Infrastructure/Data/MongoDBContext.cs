using MongoDB.Driver;
using Microsoft.Extensions.Configuration;
using Nightbrate.Core.Entities; // 1. Bu satır en üstte olmalı

namespace Nightbrate.Infrastructure.Data
{
    public class MongoDbContext
    {
        private readonly IMongoDatabase _database;
        public IMongoCollection<User> Users => _database.GetCollection<User>("Users");

        public MongoDbContext(IConfiguration configuration)
        {
            var client = new MongoClient(configuration.GetConnectionString("MongoDb"));
            _database = client.GetDatabase("NightbrateDb");
        }

        public IMongoCollection<Client> Clients => _database.GetCollection<Client>("Clients");
        
        // 2. Bu satır Diyetisyenleri veritabanına bağlar
        public IMongoCollection<Dietitian> Dietitians => _database.GetCollection<Dietitian>("Dietitians");
    }
}