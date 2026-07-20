using System.Text; // JWT imza anahtarı için byte dönüşümü
using System.Text.Json; // JSON camelCase ayarı
using System.Text.Json.Serialization; // Enum'ları string olarak serialize et
using Microsoft.AspNetCore.Authentication.JwtBearer; // Bearer token kimlik doğrulama
using Microsoft.AspNetCore.Http.Features; // Form/upload boyut limitleri
using Microsoft.IdentityModel.Tokens; // Token doğrulama parametreleri
using Nightbrate.API; // API katmanı uzantıları
using Nightbrate.API.Middleware; // Global hata yakalama
using Nightbrate.API.Monitoring; // İstek metrikleri middleware
using Nightbrate.API.Services; // Gemini AI servisleri
using Nightbrate.Application.Interfaces; // Servis arayüzleri (DI)
using Nightbrate.Application.Options; // appsettings bölümleri (Upload, Gemini vb.)
using Nightbrate.Application.Services; // İş mantığı servisleri
using Nightbrate.Infrastructure; // Cloudinary DI uzantısı
using Nightbrate.Infrastructure.Data; // MongoDB bağlantısı
using Nightbrate.Infrastructure.Monitoring; // Sistem kaynak izleme
using Nightbrate.Infrastructure.Repositories; // MongoDB repository'ler
using Nightbrate.Infrastructure.Security; // JWT üretimi
using Nightbrate.Infrastructure.Services; // Yerel dosya depolama

var builder = WebApplication.CreateBuilder(args); // ASP.NET Core uygulama fabrikası

builder.Services.Configure<FormOptions>(options =>
{
    options.MultipartBodyLengthLimit = 12 * 1024 * 1024; // Çok parçalı form max 12 MB (PDF/foto)
});
builder.WebHost.ConfigureKestrel(options =>
{
    options.Limits.MaxRequestBodySize = 12 * 1024 * 1024; // HTTP gövde üst sınırı
});

builder.Services.AddCors(options => // Web (5173) ve mobil istemciler için CORS
{
    options.AddPolicy("ClientApps", policy =>
    {
        policy.WithOrigins( // İzin verilen origin'ler
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://10.0.2.2:3000", // Android emülatör
                "http://localhost:5173", // Vite dev server
                "http://127.0.0.1:5173",
                "http://10.0.2.2:5173")
              .AllowAnyMethod() // GET/POST/PATCH hepsi
              .AllowAnyHeader(); // Authorization dahil
    });
});

var jwtKey = builder.Configuration["Jwt:Key"] ?? "NutriBridge-Dev-Key-AtLeast-64-Characters-Long-For-HS512-2026-Safe"; // Gizli imza anahtarı
var issuer = builder.Configuration["Jwt:Issuer"] ?? "NutriBridge.Api"; // Token yayıncısı
var audience = builder.Configuration["Jwt:Audience"] ?? "NutriBridge.Clients"; // Token hedef kitlesi

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme) // JWT şeması
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true, // Issuer kontrolü
            ValidateAudience = true, // Audience kontrolü
            ValidateIssuerSigningKey = true, // İmza doğrulama
            ValidIssuer = issuer,
            ValidAudience = audience,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey)), // HS512 anahtar
            ClockSkew = TimeSpan.Zero // Süre toleransı yok
        };
    });

builder.Services.AddControllers() // API Controller'ları kaydet
    .AddJsonOptions(o =>
    {
        o.JsonSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase; // firstName → JSON camelCase
        o.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter()); // Enum → "Client" string
    });
builder.Services.AddEndpointsApiExplorer(); // Swagger için
builder.Services.AddSwaggerGen(); // /swagger arayüzü

builder.Services.AddSingleton<MongoDbContext>(); // Tek MongoDB bağlantı örneği
builder.Services.AddScoped<IUserRepository, UserRepository>(); // Kullanıcı CRUD
builder.Services.AddScoped<IClientRepository, ClientRepository>(); // Danışan kayıtları
builder.Services.AddScoped<IDietitianRepository, DietitianRepository>(); // Diyetisyen kayıtları
builder.Services.AddScoped<IMealLogRepository, MealLogRepository>(); // Yemek analiz logları
builder.Services.AddScoped<IDietProgramRepository, DietProgramRepository>(); // Günlük diyet programları
builder.Services.AddScoped<IDietProgramHistoryRepository, DietProgramHistoryRepository>(); // Program geçmişi
builder.Services.AddScoped<IWaterLogRepository, WaterLogRepository>(); // Su takibi
builder.Services.AddScoped<IWeightLogRepository, WeightLogRepository>(); // Kilo takibi
builder.Services.AddScoped<IActivityLogRepository, ActivityLogRepository>(); // Sistem aktivite logu
builder.Services.AddScoped<IKitchenChefRecipeLogRepository, KitchenChefRecipeLogRepository>(); // AI tarif paylaşımları
builder.Services.AddScoped<ICriticalAlertAcknowledgmentRepository, CriticalAlertAcknowledgmentRepository>(); // Uyarı onayları
builder.Services.AddScoped<IDietitianDailyTaskRepository, DietitianDailyTaskRepository>(); // Günlük görevler
builder.Services.AddScoped<ICriticalAlertService, CriticalAlertService>(); // Kritik uyarı iş mantığı
builder.Services.AddScoped<IDietitianDailyTaskService, DietitianDailyTaskService>(); // Görev senkronizasyonu

builder.Services.AddScoped<IAuthService, AuthService>(); // Giriş/kayıt
builder.Services.AddScoped<IActivityLogService, ActivityLogService>(); // Aktivite yazma/okuma
builder.Services.AddScoped<IUserProfileService, UserProfileService>(); // Profil bilgisi
builder.Services.AddScoped<IAdminService, AdminService>(); // Admin panel işlemleri
builder.Services.AddSingleton<IRequestMetricsBuffer, RequestMetricsBuffer>(); // API metrik tamponu
builder.Services.AddSingleton<ISystemResourceProvider, SystemResourceProvider>(); // CPU/RAM vb.
builder.Services.AddSingleton<ISystemAnalyticsService, SystemAnalyticsService>(); // Admin analitik
builder.Services.AddScoped<IDietitianService, DietitianService>(); // Diyetisyen paneli
builder.Services.AddScoped<IClientService, ClientService>(); // Danışan paneli
builder.Services.AddScoped<IJwtTokenService, JwtTokenService>(); // Token üretimi

builder.Services.Configure<CloudinaryStorageOptions>(builder.Configuration.GetSection("Cloudinary")); // Bulut depolama ayarları
builder.Services.PostConfigure<CloudinaryStorageOptions>(o => // CLOUDINARY_URL ortam değişkeninden doldur
{
    var url = builder.Configuration["CLOUDINARY_URL"] ?? Environment.GetEnvironmentVariable("CLOUDINARY_URL");
    if (string.IsNullOrWhiteSpace(url))
        return;
    if (!CloudinaryUrlParser.TryParse(url.Trim(), out var cn, out var ak, out var sec))
        return;
    if (string.IsNullOrWhiteSpace(o.CloudName))
        o.CloudName = cn;
    if (string.IsNullOrWhiteSpace(o.ApiKey))
        o.ApiKey = ak;
    if (string.IsNullOrWhiteSpace(o.ApiSecret))
        o.ApiSecret = sec;
});

builder.Services.Configure<PdfUploadOptions>(o => // PDF yerel klasör ayarları
{
    o.PdfsDirectory = Path.Combine(builder.Environment.ContentRootPath, "wwwroot", "uploads", "pdfs");
    o.PublicRelativePath = "/uploads/pdfs"; // Tarayıcıdan erişim yolu
    o.MaxPdfBytes = 9 * 1024 * 1024;
});

builder.Services.Configure<DiplomaUploadOptions>(o => // Diyetisyen diploma dosyası
{
    o.DiplomasDirectory = Path.Combine(builder.Environment.ContentRootPath, "wwwroot", "uploads", "diplomas");
    o.PublicRelativePath = "/uploads/diplomas";
    o.MaxBytes = 10 * 1024 * 1024;
});
builder.Services.AddScoped<IDiplomaDocumentStorage, LocalDiplomaDocumentStorage>(); // Diploma her zaman yerel disk

var cloudinaryOpts = new CloudinaryStorageOptions(); // Cloudinary kullanılacak mı kontrolü
builder.Configuration.GetSection("Cloudinary").Bind(cloudinaryOpts);
var cloudinaryUrl = builder.Configuration["CLOUDINARY_URL"] ?? Environment.GetEnvironmentVariable("CLOUDINARY_URL");
if (!string.IsNullOrWhiteSpace(cloudinaryUrl) &&
    CloudinaryUrlParser.TryParse(cloudinaryUrl.Trim(), out var cn, out var ak, out var sec))
{
    if (string.IsNullOrWhiteSpace(cloudinaryOpts.CloudName))
        cloudinaryOpts.CloudName = cn;
    if (string.IsNullOrWhiteSpace(cloudinaryOpts.ApiKey))
        cloudinaryOpts.ApiKey = ak;
    if (string.IsNullOrWhiteSpace(cloudinaryOpts.ApiSecret))
        cloudinaryOpts.ApiSecret = sec;
}

var useCloudinary = CloudinaryUrlParser.HasUsableCredentials( // API key dolu mu?
    cloudinaryOpts.CloudName, cloudinaryOpts.ApiKey, cloudinaryOpts.ApiSecret);

if (useCloudinary)
{
    builder.Services.AddNightbrateCloudinaryStorage(); // Foto+PDF Cloudinary'ye
}
else
{
    builder.Services.Configure<MealUploadOptions>(o => // Yerel yemek fotoğrafı klasörü
    {
        o.MealsDirectory = Path.Combine(builder.Environment.ContentRootPath, "wwwroot", "uploads", "meals");
        o.PublicRelativePath = "/uploads/meals";
    });
    builder.Services.AddScoped<IMealPhotoStorage, LocalMealPhotoStorage>(); // Diskte sakla
    builder.Services.AddScoped<IPdfDocumentStorage, LocalPdfDocumentStorage>(); // PDF diskte
}
builder.Services.AddScoped<IClientPdfAnalysisRepository, ClientPdfAnalysisRepository>(); // PDF analiz Mongo kaydı
builder.Services.AddScoped<IClientPdfAnalysisService, ClientPdfAnalysisService>(); // PDF yükle+analiz akışı

builder.Services.Configure<GroqAiOptions>(builder.Configuration.GetSection("Groq"));
builder.Services.PostConfigure<GroqAiOptions>(o =>
{
    o.ApiKey = (o.ApiKey ?? string.Empty).Trim();
    var envGroq = Environment.GetEnvironmentVariable("GROQ_API_KEY");
    if (!string.IsNullOrWhiteSpace(envGroq))
        o.ApiKey = envGroq.Trim();
});

builder.Services.AddSingleton<MockMealAnalysisService>();
builder.Services.AddSingleton<MockKitchenChefService>();
builder.Services.AddSingleton<MockPdfAnalysisService>();

var groqKey = builder.Configuration["Groq:ApiKey"]?.Trim();
if (string.IsNullOrWhiteSpace(groqKey))
    groqKey = Environment.GetEnvironmentVariable("GROQ_API_KEY")?.Trim();

if (!string.IsNullOrWhiteSpace(groqKey))
{
    builder.Services.PostConfigure<GroqAiOptions>(o => o.ApiKey = groqKey);
    static void ConfigureGroqHttp(HttpClient client)
    {
        client.BaseAddress = new Uri("https://api.groq.com/openai/v1/");
        client.Timeout = TimeSpan.FromSeconds(120);
    }

    builder.Services.AddHttpClient<GroqMealAnalysisService>(ConfigureGroqHttp);
    builder.Services.AddHttpClient<GroqKitchenChefService>(ConfigureGroqHttp);
    builder.Services.AddHttpClient<GroqPdfAnalysisService>(ConfigureGroqHttp);
    builder.Services.AddSingleton<IMealAnalysisService>(sp => new FallbackMealAnalysisService(
        sp.GetRequiredService<GroqMealAnalysisService>(),
        sp.GetRequiredService<MockMealAnalysisService>(),
        sp.GetRequiredService<ILogger<FallbackMealAnalysisService>>()));
    builder.Services.AddSingleton<IKitchenChefService>(sp => new FallbackKitchenChefService(
        sp.GetRequiredService<GroqKitchenChefService>(),
        sp.GetRequiredService<MockKitchenChefService>(),
        sp.GetRequiredService<ILogger<FallbackKitchenChefService>>()));
    builder.Services.AddSingleton<IPdfAnalysisAiService>(sp => new FallbackPdfAnalysisService(
        sp.GetRequiredService<GroqPdfAnalysisService>(),
        sp.GetRequiredService<MockPdfAnalysisService>(),
        sp.GetRequiredService<ILogger<FallbackPdfAnalysisService>>()));
    Console.WriteLine("[AI] Groq aktif — yemek, mutfak şefi ve PDF analizi Groq üzerinden.");
}
else
{
    Console.WriteLine("[AI] Groq anahtarı yok — yalnızca yerel simülasyon (mock) kullanılacak.");
    builder.Services.AddSingleton<IMealAnalysisService>(sp => sp.GetRequiredService<MockMealAnalysisService>());
    builder.Services.AddSingleton<IKitchenChefService>(sp => sp.GetRequiredService<MockKitchenChefService>());
    builder.Services.AddSingleton<IPdfAnalysisAiService>(sp => sp.GetRequiredService<MockPdfAnalysisService>());
}

builder.Services.AddScoped<IMealPhotoAnalysisService, MealPhotoAnalysisService>(); // Foto kaydet + AI + MealLog

var app = builder.Build(); // Middleware pipeline oluştur

if (app.Environment.IsDevelopment()) // Sadece geliştirmede Swagger açık
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseMiddleware<ExceptionMiddleware>(); // Tüm hataları JSON { message } olarak döndür
app.UseCors("ClientApps"); // CORS politikası
app.UseStaticFiles(); // wwwroot/uploads statik dosya sunumu
app.UseAuthentication(); // JWT doğrula
app.UseAuthorization(); // [Authorize] rollerini uygula
app.UseMiddleware<RequestMetricsMiddleware>(); // İstek süresi/metrik topla
app.MapControllers(); // api/Controller/action route'ları

app.Run(); // Kestrel sunucusunu başlat (5231)
