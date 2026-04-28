using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Http.Features;
using Microsoft.IdentityModel.Tokens;
using Nightbrate.API.Middleware;
using Nightbrate.API.Monitoring;
using Nightbrate.API.Services;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Options;
using Nightbrate.Application.Services;
using Nightbrate.Infrastructure.Data;
using Nightbrate.Infrastructure.Monitoring;
using Nightbrate.Infrastructure.Repositories;
using Nightbrate.Infrastructure.Security;
using Nightbrate.Infrastructure.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.Configure<FormOptions>(options =>
{
    options.MultipartBodyLengthLimit = 5 * 1024 * 1024;
});
builder.WebHost.ConfigureKestrel(options =>
{
    options.Limits.MaxRequestBodySize = 5 * 1024 * 1024;
});

builder.Services.AddCors(options =>
{
    options.AddPolicy("ClientApps", policy =>
    {
        policy.WithOrigins(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://10.0.2.2:3000",
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://10.0.2.2:5173")
              .AllowAnyMethod()
              .AllowAnyHeader();
    });
});

var jwtKey = builder.Configuration["Jwt:Key"] ?? "NutriBridge-Dev-Key-AtLeast-64-Characters-Long-For-HS512-2026-Safe";
var issuer = builder.Configuration["Jwt:Issuer"] ?? "NutriBridge.Api";
var audience = builder.Configuration["Jwt:Audience"] ?? "NutriBridge.Clients";

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidateAudience = true,
            ValidateIssuerSigningKey = true,
            ValidIssuer = issuer,
            ValidAudience = audience,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey)),
            ClockSkew = TimeSpan.Zero
        };
    });

builder.Services.AddControllers()
    .AddJsonOptions(o =>
    {
        o.JsonSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
        o.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter());
    });
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

builder.Services.AddSingleton<MongoDbContext>();
builder.Services.AddScoped<IUserRepository, UserRepository>();
builder.Services.AddScoped<IClientRepository, ClientRepository>();
builder.Services.AddScoped<IDietitianRepository, DietitianRepository>();
builder.Services.AddScoped<IMealLogRepository, MealLogRepository>();
builder.Services.AddScoped<IDietProgramRepository, DietProgramRepository>();
builder.Services.AddScoped<IDietProgramHistoryRepository, DietProgramHistoryRepository>();
builder.Services.AddScoped<IWaterLogRepository, WaterLogRepository>();
builder.Services.AddScoped<IWeightLogRepository, WeightLogRepository>();
builder.Services.AddScoped<IActivityLogRepository, ActivityLogRepository>();
builder.Services.AddScoped<IKitchenChefRecipeLogRepository, KitchenChefRecipeLogRepository>();
builder.Services.AddScoped<ICriticalAlertAcknowledgmentRepository, CriticalAlertAcknowledgmentRepository>();
builder.Services.AddScoped<IDietitianDailyTaskRepository, DietitianDailyTaskRepository>();
builder.Services.AddScoped<ICriticalAlertService, CriticalAlertService>();
builder.Services.AddScoped<IDietitianDailyTaskService, DietitianDailyTaskService>();

builder.Services.AddScoped<IAuthService, AuthService>();
builder.Services.AddScoped<IActivityLogService, ActivityLogService>();
builder.Services.AddScoped<IUserProfileService, UserProfileService>();
builder.Services.AddScoped<IAdminService, AdminService>();
builder.Services.AddSingleton<IRequestMetricsBuffer, RequestMetricsBuffer>();
builder.Services.AddSingleton<ISystemResourceProvider, SystemResourceProvider>();
builder.Services.AddSingleton<ISystemAnalyticsService, SystemAnalyticsService>();
builder.Services.AddScoped<IDietitianService, DietitianService>();
builder.Services.AddScoped<IClientService, ClientService>();
builder.Services.AddScoped<IJwtTokenService, JwtTokenService>();

builder.Services.Configure<MealUploadOptions>(o =>
{
    o.MealsDirectory = Path.Combine(builder.Environment.ContentRootPath, "wwwroot", "uploads", "meals");
    o.PublicRelativePath = "/uploads/meals";
});
builder.Services.AddScoped<IMealPhotoStorage, LocalMealPhotoStorage>();

builder.Services.Configure<GeminiMealAnalysisOptions>(builder.Configuration.GetSection("Gemini"));
builder.Services.PostConfigure<GeminiMealAnalysisOptions>(o =>
{
    o.ApiKey = (o.ApiKey ?? string.Empty).Trim();
    o.Model = string.IsNullOrWhiteSpace(o.Model) ? "gemini-2.5-flash" : o.Model.Trim();
});

var geminiKey = builder.Configuration["Gemini:ApiKey"]?.Trim();
if (!string.IsNullOrWhiteSpace(geminiKey))
{
    builder.Services.AddHttpClient<IMealAnalysisService, GeminiMealAnalysisService>();
    builder.Services.AddHttpClient<IKitchenChefService, GeminiKitchenChefService>();
}
else
{
    builder.Services.AddSingleton<IMealAnalysisService, MockMealAnalysisService>();
    builder.Services.AddSingleton<IKitchenChefService, MockKitchenChefService>();
}

builder.Services.AddScoped<IMealPhotoAnalysisService, MealPhotoAnalysisService>();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseMiddleware<ExceptionMiddleware>();
app.UseCors("ClientApps");
app.UseStaticFiles();
app.UseAuthentication();
app.UseAuthorization();
app.UseMiddleware<RequestMetricsMiddleware>();
app.MapControllers();

app.Run();