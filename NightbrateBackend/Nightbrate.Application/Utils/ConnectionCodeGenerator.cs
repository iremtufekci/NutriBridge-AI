using System.Security.Cryptography;
using System.Text;

namespace Nightbrate.Application.Utils;

public static class ConnectionCodeGenerator
{
    private const string Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /// <summary>6 haneli, büyük harf + rakam (0–9, A–Z), çakışmaya karsi depo tarafinda tekrar denenir.</summary>
    public static string NewCode()
    {
        var b = new byte[6];
        RandomNumberGenerator.Fill(b);
        var sb = new StringBuilder(6);
        foreach (var x in b)
            sb.Append(Chars[x % Chars.Length]);
        return sb.ToString();
    }
}
