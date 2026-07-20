namespace Nightbrate.API;

/// <summary>
/// <c>cloudinary://&lt;api_key&gt;:&lt;api_secret&gt;@&lt;cloud_name&gt;</c> (CLOUDINARY_URL) ayrıştırır.
/// </summary>
internal static class CloudinaryUrlParser
{
    public static bool TryParse(string url, out string cloudName, out string apiKey, out string apiSecret)
    {
        cloudName = string.Empty;
        apiKey = string.Empty;
        apiSecret = string.Empty;
        try
        {
            var uri = new Uri(url);
            if (!string.Equals(uri.Scheme, "cloudinary", StringComparison.OrdinalIgnoreCase))
                return false;

            cloudName = uri.Host;
            var userInfo = uri.UserInfo;
            if (string.IsNullOrEmpty(userInfo))
                return false;

            var colon = userInfo.IndexOf(':');
            if (colon < 0)
            {
                apiKey = Uri.UnescapeDataString(userInfo);
                return false;
            }

            apiKey = Uri.UnescapeDataString(userInfo[..colon]);
            apiSecret = Uri.UnescapeDataString(userInfo[(colon + 1)..]);
            return !string.IsNullOrWhiteSpace(cloudName) && !string.IsNullOrWhiteSpace(apiKey);
        }
        catch (UriFormatException)
        {
            return false;
        }
    }

    /// <summary>Placeholder veya eksik anahtarlar Cloudinary'e gitmemeli (yerel depolama kullanilir).</summary>
    public static bool HasUsableCredentials(string cloudName, string apiKey, string apiSecret)
    {
        static bool Placeholder(string? v) =>
            string.IsNullOrWhiteSpace(v) ||
            v.Contains("YOUR_", StringComparison.OrdinalIgnoreCase) ||
            v.Contains("CHANGE_ME", StringComparison.OrdinalIgnoreCase);

        return !Placeholder(cloudName) && !Placeholder(apiKey) && !Placeholder(apiSecret);
    }
}
