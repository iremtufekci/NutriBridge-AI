using Microsoft.AspNetCore.Mvc;
using Nightbrate.Infrastructure.Data;
using Nightbrate.Core.Entities;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Utils;
using MongoDB.Driver;

namespace Nightbrate.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class UserController : ControllerBase
{
    private readonly MongoDbContext _context;

    public UserController(MongoDbContext context)
    {
        _context = context;
    }
    [HttpPost("login")]
public async Task<IActionResult> Login(UserLoginDto request)
{
    var usersCollection = _context.GetCollection<User>("Users");
    
    // 1. Kullanıcıyı email ile bul
    var user = await usersCollection.Find(u => u.Email == request.Email).FirstOrDefaultAsync();
    if (user == null) return BadRequest("Kullanıcı bulunamadı!");

    // 2. Şifreyi doğrula
    if (!PasswordHasher.VerifyPasswordHash(request.Password, user.PasswordHash, user.PasswordSalt))
    {
        return BadRequest("Hatalı şifre!");
    }

    // 3. Başarılı giriş (İleride buraya JWT Token ekleyeceğiz)
    return Ok(new { 
        message = "Giriş başarılı!", 
        username = user.Username, 
        role = user.Role.ToString() 
    });
}

    [HttpPost("register")]
    public async Task<IActionResult> Register(UserRegisterDto request)
    {
        var usersCollection = _context.GetCollection<User>("Users");

        // 1. Email zaten kayıtlı mı kontrolü
        var existingUser = await usersCollection.Find(u => u.Email == request.Email).FirstOrDefaultAsync();
        if (existingUser != null) return BadRequest("Bu email zaten kayıtlı!");

        // 2. Şifreyi Hash'leme
        PasswordHasher.CreatePasswordHash(request.Password, out byte[] passwordHash, out byte[] passwordSalt);

        // 3. Yeni kullanıcı nesnesini oluşturma
        var user = new User
        {
            Username = request.Username,
            Email = request.Email,
            PasswordHash = passwordHash,
            PasswordSalt = passwordSalt,
            Role = (UserRole)request.Role // DTO'dan gelen rakamı Role enum'ına çevirir
        };

        // 4. Veritabanına kaydetme
        await usersCollection.InsertOneAsync(user);

        return Ok(new { message = "Kayıt başarılı!", userId = user.Id });
    }
}