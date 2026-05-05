using CloudinaryDotNet;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Options;
using Nightbrate.Infrastructure.Services;

namespace Nightbrate.Infrastructure;

public static class CloudinaryStorageServiceCollectionExtensions
{
    /// <summary>Fotograflar image, PDF dosyalari raw olarak Cloudinary'e yuklenir.</summary>
    public static IServiceCollection AddNightbrateCloudinaryStorage(this IServiceCollection services)
    {
        services.AddSingleton(sp =>
        {
            var o = sp.GetRequiredService<IOptions<CloudinaryStorageOptions>>().Value;
            return new Cloudinary(new Account(o.CloudName.Trim(), o.ApiKey.Trim(), o.ApiSecret.Trim()));
        });

        services.AddScoped<IMealPhotoStorage, CloudinaryMealPhotoStorage>();
        services.AddScoped<IPdfDocumentStorage, CloudinaryPdfDocumentStorage>();
        return services;
    }
}
