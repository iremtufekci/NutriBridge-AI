using MongoDB.Driver;
using Microsoft.Extensions.Configuration;

namespace Nightbrate.Infrastructure.Data;

public class MongoDbContext
{
    private readonly IMongoDatabase _database;

    public MongoDbContext(IConfiguration configuration)
    {
        // appsettings.json dosyasından az önce yazdığın linki okur
        var connectionString = configuration.GetSection("ConnectionStrings:MongoDb").Value;
        var client = new MongoClient(connectionString);
        
        // Veritabanı adını buradan alıyoruz
        _database = client.GetDatabase("NightbrateDb");
    }

    // Koleksiyonlara (Tablolara) erişmek için bu metodu kullanacağız
    public IMongoCollection<T> GetCollection<T>(string name) => _database.GetCollection<T>(name);
}