namespace Nightbrate.Core.Entities
{
    public class Client : BaseUser
    {
        public string FirstName { get; set; } = string.Empty;
        public string LastName { get; set; } = string.Empty;
        public double Weight { get; set; }
        public double Height { get; set; }
        public int TargetCalories { get; set; }
        public string? DietitianId { get; set; }
        public string ThemePreference { get; set; } = "light";
    }
}