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
        public AuthController(IAuthService authService) => _authService = authService;

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
    }
}