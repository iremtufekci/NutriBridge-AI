namespace Nightbrate.Application.DTOs
{
    public class ClientRegisterDto
    {
        public string FirstName { get; set; } = string.Empty;
        public string LastName { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string Password { get; set; } = string.Empty;
        public double Weight { get; set; }
        public double Height { get; set; }
        public int TargetCalories { get; set; }
        public string? DietitianId { get; set; }
    }
}