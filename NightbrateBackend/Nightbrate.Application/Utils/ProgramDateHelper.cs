using System.Globalization;

namespace Nightbrate.Application.Utils;

public static class ProgramDateHelper
{
    public const string JsonFormat = "yyyy-MM-dd";

    public static string? TryNormalize(string? raw)
    {
        if (string.IsNullOrWhiteSpace(raw)) return null;
        var s = raw.Trim();

        if (DateOnly.TryParseExact(s, JsonFormat, CultureInfo.InvariantCulture, DateTimeStyles.None, out var dExact))
            return dExact.ToString(JsonFormat, CultureInfo.InvariantCulture);

        // Uç istemciler: ISO-8601 ("2026-04-25T00:00:00.000Z") veya benzeri — sadece tarih kısmı kullanılır
        if (s.Length >= 10 && s[4] == '-' && s[7] == '-')
        {
            var head = s[..10];
            if (DateOnly.TryParseExact(head, JsonFormat, CultureInfo.InvariantCulture, DateTimeStyles.None, out var dHead))
                return dHead.ToString(JsonFormat, CultureInfo.InvariantCulture);
        }

        if (DateTime.TryParse(
                s,
                CultureInfo.InvariantCulture,
                DateTimeStyles.RoundtripKind | DateTimeStyles.AssumeUniversal,
                out var dt))
            return DateOnly.FromDateTime(dt).ToString(JsonFormat, CultureInfo.InvariantCulture);

        return null;
    }

    public static bool IsBeforeTodayUtc(string programDate)
    {
        if (!DateOnly.TryParseExact(programDate, JsonFormat, CultureInfo.InvariantCulture, DateTimeStyles.None, out var d))
            return true;
        var today = DateOnly.FromDateTime(DateTime.UtcNow.Date);
        return d < today;
    }

    /// <summary>yyyy-MM-dd string → o günün UTC 00:00:00 anı.</summary>
    public static DateTime UtcStartOfYmdString(string ymd)
    {
        if (!DateOnly.TryParseExact(ymd, JsonFormat, CultureInfo.InvariantCulture, DateTimeStyles.None, out var d))
            return DateTime.SpecifyKind(DateTime.UtcNow.Date, DateTimeKind.Utc);
        return new DateTime(d.Year, d.Month, d.Day, 0, 0, 0, DateTimeKind.Utc);
    }

    /// <summary>Türkiye saatine göre bugünün yyyy-MM-dd değeri.</summary>
    public static string TurkeyTodayYmd()
    {
        var tr = TimeZoneInfo.ConvertTimeFromUtc(DateTime.UtcNow, ResolveTurkeyTimeZone());
        return tr.ToString(JsonFormat, CultureInfo.InvariantCulture);
    }

    /// <summary>Türkiye takvim günü başlangıcı (UTC).</summary>
    public static DateTime TurkeyTodayStartUtc()
    {
        var tz = ResolveTurkeyTimeZone();
        var tr = TimeZoneInfo.ConvertTimeFromUtc(DateTime.UtcNow, tz);
        var localMidnight = new DateTime(tr.Year, tr.Month, tr.Day, 0, 0, 0, DateTimeKind.Unspecified);
        return TimeZoneInfo.ConvertTimeToUtc(localMidnight, tz);
    }

    /// <summary>Türkiye takvim günü bitişi (ertesi gün 00:00 UTC, yarı açık aralık için).</summary>
    public static DateTime TurkeyTomorrowStartUtc() => TurkeyTodayStartUtc().AddDays(1);

    private static TimeZoneInfo ResolveTurkeyTimeZone()
    {
        try
        {
            return TimeZoneInfo.FindSystemTimeZoneById("Europe/Istanbul");
        }
        catch (TimeZoneNotFoundException)
        {
            return TimeZoneInfo.FindSystemTimeZoneById("Turkey Standard Time");
        }
    }
}
