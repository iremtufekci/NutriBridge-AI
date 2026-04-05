namespace Nightbrate.Application.DTOs
{
    public class ClientRegisterDto
    {
        public string FirstName { get; set; }
        public string LastName { get; set; }
        public string Email { get; set; }
        public string Password { get; set; }
        public int Height { get; set; }
        public int Weight { get; set; }
        public string Goal { get; set; }
        public string ActivityLevel { get; set; }
        public DateTime BirthDate { get; set; }
    }
}