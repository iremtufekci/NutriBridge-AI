namespace Nightbrate.Core.Entities // Varlık sınıfları ad alanı
{
    public class Client : BaseUser // Danışan kullanıcı varlığı
    {
        public string FirstName { get; set; } = string.Empty; // Danışan adı
        public string LastName { get; set; } = string.Empty; // Danışan soyadı
        public double Weight { get; set; } // Güncel kilo (kg)
        public double Height { get; set; } // Boy (cm)
        public int TargetCalories { get; set; } // Günlük hedef kalori
        public string? DietitianId { get; set; } // Bağlı diyetisyen kimliği (varsa)
    }
}
