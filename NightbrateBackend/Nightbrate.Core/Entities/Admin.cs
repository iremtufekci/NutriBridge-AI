namespace Nightbrate.Core.Entities;

/// <summary>Admin hesapları Users koleksiyonunda (polimorfik serileştirme) saklanır.</summary>
public class Admin : BaseUser
{
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
}
