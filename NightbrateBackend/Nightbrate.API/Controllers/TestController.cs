using Microsoft.AspNetCore.Mvc;
using Nightbrate.Infrastructure.Data;
using MongoDB.Driver;
using MongoDB.Bson;

namespace Nightbrate.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class TestController : ControllerBase
{
    private readonly MongoDbContext _context;

    public TestController(MongoDbContext context)
    {
        _context = context;
    }

    [HttpGet("ping")]
    public IActionResult PingMongoDB()
    {
        try
        {
            // MongoDB'ye basit bir komut gönderiyoruz
            var result = _context.GetCollection<BsonDocument>("Test").Database.RunCommand<BsonDocument>(new BsonDocument("ping", 1));
            return Ok(new { message = "MongoDB Bağlantısı Başarılı!", detail = result.ToString() });
        }
        catch (Exception ex)
        {
            return BadRequest(new { message = "Bağlantı Hatası!", error = ex.Message });
        }
    }
}