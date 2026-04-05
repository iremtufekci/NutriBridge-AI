using Microsoft.AspNetCore.Mvc;
using Nightbrate.Application.DTOs;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;
using MongoDB.Driver;

namespace Nightbrate.API.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly MongoDbContext _context;

        // Veritabanı bağlantısını buraya çektik
        public AuthController(MongoDbContext context)
        {
            _context = context;
        }

        // 1. DANIŞAN KAYDI
        [HttpPost("register-client")]
        public async Task<IActionResult> RegisterClient([FromBody] ClientRegisterDto request)
        {
            if (request == null) return BadRequest(new { message = "Veri geçersiz." });

            var newClient = new Client
            {
                FirstName = request.FirstName,
                LastName = request.LastName,
                Email = request.Email,
                PasswordHash = request.Password, // Şimdilik düz metin, ileride hash'leyeceğiz
                Height = request.Height,
                Weight = request.Weight,
                Goal = request.Goal,
                ActivityLevel = request.ActivityLevel,
                BirthDate = request.BirthDate
            };

            // MongoDB'ye kaydet
            await _context.Clients.InsertOneAsync(newClient);

            return Ok(new { success = true, message = $"Hoş geldin {request.FirstName}! Kaydın başarıyla tamamlandı." });
        }
        [HttpPost("register-dietitian")]
public async Task<IActionResult> RegisterDietitian([FromBody] DietitianRegisterDto dto)
{
    // 1. Konsola yazdır (Zaten bunu görüyorsun, çalışıyor!)
    Console.WriteLine($"İstek Geldi: {dto.FirstName} {dto.LastName}");

    // 2. MongoDB'ye Kayıt Kısmı (BURAYI EKLE):
    var newDietitian = new Dietitian
    {
        FirstName = dto.FirstName,
        LastName = dto.LastName,
        Email = dto.Email,
        PasswordHash = dto.Password, // Şifreleme ileride eklenecek
        DiplomaNo = dto.DiplomaNo,
        ClinicName = dto.ClinicName,
        IsApproved = false, // Admin onayı bekliyor
        CreatedAt = DateTime.Now
    };

    // Veritabanına asıl yazma komutu:
    await _context.Dietitians.InsertOneAsync(newDietitian); 

    return Ok(new { success = true, message = "Kayıt talebi başarıyla alındı. Admin onayı bekleniyor." });
}
        
    }
}