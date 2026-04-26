using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;

namespace Nightbrate.API.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly IAuthService _authService;
        private readonly IUserProfileService _userProfileService;
        private readonly IUserRepository _userRepository;

        public AuthController(
            IAuthService authService,
            IUserProfileService userProfileService,
            IUserRepository userRepository)
        {
            _authService = authService;
            _userProfileService = userProfileService;
            _userRepository = userRepository;
        }

        [HttpPost("register-client")]
        public async Task<IActionResult> RegisterClient([FromBody] ClientRegisterDto request)
        {
            await _authService.RegisterClientAsync(request);
            return Ok(new { message = "Client kaydı başarılı." });
        }

        [HttpPost("register-dietitian")]
        public async Task<IActionResult> RegisterDietitian([FromBody] DietitianRegisterDto dto)
        {
            await _authService.RegisterDietitianAsync(dto);
            return Ok(new { message = "Diyetisyen kaydı alındı. Onay bekleniyor." });
        }

        [HttpPost("login")]
        public async Task<IActionResult> Login([FromBody] LoginDto request)
        {
            var result = await _authService.LoginAsync(request);
            return Ok(new { token = result.Token, role = result.Role });
        }

        [HttpGet("profile")]
        [Authorize]
        public async Task<IActionResult> GetCurrentProfile()
        {
            var userId = User.FindFirstValue("UserId") ?? User.FindFirstValue(ClaimTypes.NameIdentifier) ?? string.Empty;
            if (string.IsNullOrEmpty(userId)) return Unauthorized();
            return Ok(await _userProfileService.GetByUserIdAsync(userId));
        }

        [HttpPost("theme")]
        [Authorize]
        public async Task<IActionResult> UpdateTheme([FromBody] UpdateThemePreferenceDto dto)
        {
            var userId = User.FindFirstValue("UserId") ?? User.FindFirstValue(ClaimTypes.NameIdentifier) ?? string.Empty;
            if (string.IsNullOrEmpty(userId)) return Unauthorized();
            await _userRepository.UpdateThemePreferenceAllStoresAsync(userId, dto.ThemePreference);
            return Ok();
        }
    }
}