namespace Nightbrate.Core.Entities
{
    public class Client
    {
        public int Id { get; set; }
        public string FirstName { get; set; }
        public string LastName { get; set; }
        public string Email { get; set; }
        public string PasswordHash { get; set; } // Şifreyi açık yazmıyoruz
        public int Height { get; set; }
        public int Weight { get; set; }
        public string Goal { get; set; }
        public string ActivityLevel { get; set; }
        public DateTime BirthDate { get; set; }
    }
}