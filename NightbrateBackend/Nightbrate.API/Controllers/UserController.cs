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
        // HATA BURADAYDI: _context.GetCollection yerine doğrudan koleksiyon özelliğini kullanıyoruz
        // Not: MongoDbContext içinde 'public IMongoCollection<User> Users => ...' tanımı olmalı
        var user = await _context.Users.Find(u => u.Email == request.Email).FirstOrDefaultAsync();
        
        if (user == null) return BadRequest("Kullanıcı bulunamadı!");

        if (!PasswordHasher.VerifyPasswordHash(request.Password, user.PasswordHash, user.PasswordSalt))
        {
            return BadRequest("Hatalı şifre!");
        }

        return Ok(new { 
            message = "Giriş başarılı!", 
            username = user.Username, 
            role = user.Role.ToString() 
        });
    }

    [HttpPost("register")]
    public async Task<IActionResult> Register(UserRegisterDto request)
    {
        // HATA BURADAYDI: _context.Users kullanarak doğrudan erişiyoruz
        var existingUser = await _context.Users.Find(u => u.Email == request.Email).FirstOrDefaultAsync();
        
        if (existingUser != null) return BadRequest("Bu email zaten kayıtlı!");

        PasswordHasher.CreatePasswordHash(request.Password, out byte[] passwordHash, out byte[] passwordSalt);

        var user = new User
        {
            Username = request.Username,
            Email = request.Email,
            PasswordHash = passwordHash,
            PasswordSalt = passwordSalt,
            Role = (UserRole)request.Role 
        };

        await _context.Users.InsertOneAsync(user);

        return Ok(new { message = "Kayıt başarılı!", userId = user.Id });
    }
}