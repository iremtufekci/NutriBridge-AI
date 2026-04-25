using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using Nightbrate.API.Middleware;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Services;
using Nightbrate.Infrastructure.Data;
using Nightbrate.Infrastructure.Repositories;
using Nightbrate.Infrastructure.Security;

var builder = WebApplication.CreateBuilder(args);

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
builder.Services.AddScoped<IWaterLogRepository, WaterLogRepository>();
builder.Services.AddScoped<IWeightLogRepository, WeightLogRepository>();
builder.Services.AddScoped<IActivityLogRepository, ActivityLogRepository>();

builder.Services.AddScoped<IAuthService, AuthService>();
builder.Services.AddScoped<IActivityLogService, ActivityLogService>();
builder.Services.AddScoped<IUserProfileService, UserProfileService>();
builder.Services.AddScoped<IAdminService, AdminService>();
builder.Services.AddScoped<IDietitianService, DietitianService>();
builder.Services.AddScoped<IClientService, ClientService>();
builder.Services.AddScoped<IJwtTokenService, JwtTokenService>();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseMiddleware<ExceptionMiddleware>();
app.UseCors("ClientApps");
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();

app.Run();