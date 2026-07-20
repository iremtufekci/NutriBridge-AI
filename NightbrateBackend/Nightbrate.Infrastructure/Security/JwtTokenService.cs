using System.IdentityModel.Tokens.Jwt; // JWT token oluşturma ve işleme
using System.Security.Claims; // Token içi kullanıcı iddiaları (claims)
using System.Text; // Metin kodlama (UTF-8 anahtar)
using Microsoft.Extensions.Configuration; // appsettings yapılandırması
using Microsoft.IdentityModel.Tokens; // İmza ve doğrulama anahtarları
using Nightbrate.Application.Interfaces; // IJwtTokenService arayüzü
using Nightbrate.Core.Entities; // BaseUser varlığı

namespace Nightbrate.Infrastructure.Security; // Güvenlik altyapı katmanı

public class JwtTokenService(IConfiguration configuration) : IJwtTokenService // JWT token üretim servisi
{
    public string CreateToken(BaseUser user) // Verilen kullanıcı için JWT string döner
    {
        var key = configuration["Jwt:Key"] ?? throw new InvalidOperationException("Jwt:Key eksik."); // İmzalama gizli anahtarı
        var issuer = configuration["Jwt:Issuer"] ?? "NutriBridge.Api"; // Token yayıncısı (issuer)
        var audience = configuration["Jwt:Audience"] ?? "NutriBridge.Clients"; // Hedef kitle (audience)

        var role = user.Role.ToString(); // Rol adını string olarak al
        var claims = new List<Claim> // Token'a eklenecek kimlik bilgileri
        {
            new(JwtRegisteredClaimNames.Sub, user.Id ?? string.Empty), // Konu (subject) = kullanıcı id
            new("UserId", user.Id ?? string.Empty), // Özel UserId claim'i
            new(ClaimTypes.Role, role), // Standart rol claim'i
            // [Authorize(Roles = "Admin")] JWT tarafında çoğu kurulum "role" claim'ini bekler
            new("role", role), // JWT middleware uyumlu rol claim'i
            new(JwtRegisteredClaimNames.Email, user.Email) // E-posta claim'i
        };

        var credentials = new SigningCredentials( // Token imzalama bilgileri
            new SymmetricSecurityKey(Encoding.UTF8.GetBytes(key)), // Simetrik anahtar (UTF-8 baytları)
            SecurityAlgorithms.HmacSha512Signature); // HMAC-SHA512 imza algoritması

        var token = new JwtSecurityToken( // JWT nesnesini oluştur
            issuer: issuer, // Yayıncı
            audience: audience, // Hedef kitle
            claims: claims, // Kullanıcı iddiaları
            expires: DateTime.UtcNow.AddDays(7), // 7 gün geçerlilik süresi
            signingCredentials: credentials); // İmza bilgileri

        return new JwtSecurityTokenHandler().WriteToken(token); // Token'ı string olarak serileştir
    }
}
