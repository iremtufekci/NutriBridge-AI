using Microsoft.AspNetCore.Mvc;
using Nightbrate.Application.DTOs;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;
using MongoDB.Driver;
using System.Security.Cryptography;
using System.Text;

namespace Nightbrate.API.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly MongoDbContext _context;

        public AuthController(MongoDbContext context)
        {
            _context = context;
        }

        [HttpPost("register-client")]
        public async Task<IActionResult> RegisterClient([FromBody] ClientRegisterDto request)
        {
            if (request == null) return BadRequest(new { message = "Veri geçersiz." });

            CreatePasswordHash(request.Password, out byte[] passwordHash, out byte[] passwordSalt);

            var newClient = new Client
            {
                FirstName = request.FirstName,
                LastName = request.LastName,
                Email = request.Email.ToLower().Trim(),
                PasswordHash = passwordHash,
                PasswordSalt = passwordSalt,
                Height = request.Height,
                Weight = request.Weight,
                Goal = request.Goal,
                ActivityLevel = request.ActivityLevel,
                BirthDate = request.BirthDate,
                CreatedAt = DateTime.UtcNow
            };

            var newUser = new User
            {
                Username = $"{request.FirstName} {request.LastName}",
                Email = request.Email.ToLower().Trim(),
                PasswordHash = passwordHash,
                PasswordSalt = passwordSalt,
                Role = UserRole.Client,
                CreatedAt = DateTime.UtcNow
            };

            await _context.Clients.InsertOneAsync(newClient);
            await _context.Users.InsertOneAsync(newUser);

            return Ok(new { success = true, message = $"Hoş geldin {request.FirstName}! Kaydın başarıyla tamamlandı." });
        }

        [HttpPost("register-dietitian")]
        public async Task<IActionResult> RegisterDietitian([FromBody] DietitianRegisterDto dto)
        {
            if (dto == null) return BadRequest(new { message = "Veri geçersiz." });

            CreatePasswordHash(dto.Password, out byte[] passwordHash, out byte[] passwordSalt);

            var newDietitian = new Dietitian
            {
                FirstName = dto.FirstName,
                LastName = dto.LastName,
                Email = dto.Email.ToLower().Trim(),
                PasswordHash = passwordHash,
                PasswordSalt = passwordSalt,
                DiplomaNo = dto.DiplomaNo,
                ClinicName = dto.ClinicName,
                IsApproved = false,
                CreatedAt = DateTime.UtcNow
            };

            var newUser = new User
            {
                Username = $"{dto.FirstName} {dto.LastName}",
                Email = dto.Email.ToLower().Trim(),
                PasswordHash = passwordHash,
                PasswordSalt = passwordSalt,
                Role = UserRole.Dietitian,
                CreatedAt = DateTime.UtcNow
            };

            await _context.Dietitians.InsertOneAsync(newDietitian);
            await _context.Users.InsertOneAsync(newUser);

            return Ok(new { success = true, message = "Kayıt talebi başarıyla alındı. Admin onayı bekleniyor." });
        }

        [HttpPost("login")]
        public async Task<IActionResult> Login([FromBody] UserLoginDto request)
        {
            Console.WriteLine($"\n--- Giriş Denemesi: {request.Email} ---");

            if (request == null || string.IsNullOrEmpty(request.Email))
                return BadRequest(new { message = "Bilgiler eksik." });

            // E-postayı temizleyip arıyoruz
            var user = await _context.Users.Find(u => u.Email == request.Email.ToLower().Trim()).FirstOrDefaultAsync();

            if (user == null)
            {
                Console.WriteLine("HATA: Kullanıcı veritabanında bulunamadı.");
                return Unauthorized(new { message = "E-posta veya şifre hatalı." });
            }

            Console.WriteLine("BİLGİ: Kullanıcı bulundu. Şifre doğrulanıyor...");

            bool isPasswordValid = VerifyPasswordHash(request.Password, user.PasswordHash, user.PasswordSalt);
            Console.WriteLine($"SONUÇ: Şifre eşleşme durumu -> {isPasswordValid}");

            if (!isPasswordValid)
                return Unauthorized(new { message = "E-posta veya şifre hatalı." });

            string roleString = user.Role switch
            {
                UserRole.Admin => "admin",
                UserRole.Dietitian => "dietitian",
                UserRole.Client => "user", 
                _ => "user"
            };

            return Ok(new
            {
                token = "test_token_hazirlaniyor",
                role = roleString,
                userName = user.Username
            });
        }

        private void CreatePasswordHash(string password, out byte[] passwordHash, out byte[] passwordSalt)
        {
            using (var hmac = new HMACSHA512())
            {
                passwordSalt = hmac.Key;
                passwordHash = hmac.ComputeHash(Encoding.UTF8.GetBytes(password));
            }
        }

        private bool VerifyPasswordHash(string password, byte[] passwordHash, byte[] passwordSalt)
        {
            using (var hmac = new HMACSHA512(passwordSalt))
            {
                var computedHash = hmac.ComputeHash(Encoding.UTF8.GetBytes(password));
                return computedHash.SequenceEqual(passwordHash);
            }
        }
    }
}