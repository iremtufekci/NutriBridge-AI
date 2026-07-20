using System.Security.Claims; // JWT claim okuma (UserId)
using Microsoft.AspNetCore.Authorization; // [Authorize] attribute
using Microsoft.AspNetCore.Mvc; // Controller tabanı
using Nightbrate.Application.DTOs; // İstek/cevap modelleri
using Nightbrate.Application.Interfaces; // Servis arayüzleri

namespace Nightbrate.API.Controllers
{
    [ApiController] // Otomatik model doğrulama + 400 cevapları
    [Route("api/[controller]")] // Temel yol: api/Auth
    public class AuthController : ControllerBase
    {
        private readonly IAuthService _authService; // Giriş ve kayıt iş mantığı
        private readonly IUserProfileService _userProfileService; // Profil okuma
        private readonly IUserRepository _userRepository; // Tema güncelleme için doğrudan repo

        public AuthController( // Constructor injection
            IAuthService authService,
            IUserProfileService userProfileService,
            IUserRepository userRepository)
        {
            _authService = authService;
            _userProfileService = userProfileService;
            _userRepository = userRepository;
        }

        [HttpPost("register-client")] // POST api/Auth/register-client
        public async Task<IActionResult> RegisterClient([FromBody] ClientRegisterDto request)
        {
            await _authService.RegisterClientAsync(request); // Şifre hash + MongoDB'ye yaz
            return Ok(new { message = "Client kaydı başarılı." });
        }

        private const int MaxDiplomaBytes = 10 * 1024 * 1024; // Diploma max 10 MB

        [HttpPost("register-dietitian")] // POST api/Auth/register-dietitian (multipart)
        [Consumes("multipart/form-data")] // Form + dosya kabul eder
        public async Task<IActionResult> RegisterDietitian(
            [FromForm] string firstName, // Form alanı: ad
            [FromForm] string lastName,
            [FromForm] string email,
            [FromForm] string password,
            [FromForm] string diplomaNo,
            [FromForm] string clinicName,
            IFormFile? diploma) // Diploma dosyası
        {
            if (diploma is null || diploma.Length == 0) // Dosya zorunlu
                return BadRequest(new { message = "Diploma veya sertifika belgesi zorunludur." });

            if (diploma.Length > MaxDiplomaBytes) // Boyut kontrolü
                return BadRequest(new { message = "Diploma dosyasi en fazla 10 MB olabilir." });

            var ct = (diploma.ContentType ?? string.Empty).Trim().ToLowerInvariant(); // MIME tipi
            var ext = Path.GetExtension(diploma.FileName ?? string.Empty).ToLowerInvariant(); // Uzantı
            var okByType = ct is "application/pdf" or "application/x-pdf" or "image/jpeg" or "image/jpg" or "image/png";
            var okByExt = ext is ".pdf" or ".jpg" or ".jpeg" or ".png";
            if (!okByType && !okByExt) // Tip veya uzantı uygun değilse
                return BadRequest(new { message = "Yalnizca PDF, JPG veya PNG yukleyebilirsiniz." });

            var dto = new DietitianRegisterDto // Servise gidecek DTO
            {
                FirstName = firstName?.Trim() ?? string.Empty,
                LastName = lastName?.Trim() ?? string.Empty,
                Email = email?.Trim() ?? string.Empty,
                Password = password ?? string.Empty,
                DiplomaNo = diplomaNo?.Trim() ?? string.Empty,
                ClinicName = clinicName?.Trim() ?? string.Empty
            };

            await using var stream = diploma.OpenReadStream(); // Dosya akışı
            await _authService.RegisterDietitianAsync(dto, stream, diploma.FileName ?? "diploma.pdf"); // Kayıt + onay bekliyor
            return Ok(new { message = "Diyetisyen kaydı alındı. Onay bekleniyor." });
        }

        [HttpPost("login")] // POST api/Auth/login
        public async Task<IActionResult> Login([FromBody] LoginDto request)
        {
            var result = await _authService.LoginAsync(request); // E-posta/şifre doğrula → JWT üret
            return Ok(new { token = result.Token, role = result.Role }); // Web/mobil bunu saklar
        }

        [HttpGet("profile")] // GET api/Auth/profile
        [Authorize] // JWT zorunlu
        public async Task<IActionResult> GetCurrentProfile()
        {
            var userId = User.FindFirstValue("UserId") ?? User.FindFirstValue(ClaimTypes.NameIdentifier) ?? string.Empty; // Token'dan id
            if (string.IsNullOrEmpty(userId)) return Unauthorized();
            return Ok(await _userProfileService.GetByUserIdAsync(userId)); // Görünen ad, rol vb.
        }

        [HttpPost("theme")] // POST api/Auth/theme — diyetisyen/admin tema
        [Authorize]
        public async Task<IActionResult> UpdateTheme([FromBody] UpdateThemePreferenceDto dto)
        {
            var userId = User.FindFirstValue("UserId") ?? User.FindFirstValue(ClaimTypes.NameIdentifier) ?? string.Empty;
            if (string.IsNullOrEmpty(userId)) return Unauthorized();
            await _userRepository.UpdateThemePreferenceAllStoresAsync(userId, dto.ThemePreference); // light/dark
            return Ok();
        }
    }
}
