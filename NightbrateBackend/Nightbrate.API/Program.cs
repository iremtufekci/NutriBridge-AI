using Nightbrate.Infrastructure.Data;

var builder = WebApplication.CreateBuilder(args);

// --- 1. SERVİS AYARLARI (builder.Build'den önce olmalı!) ---
builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();
builder.Services.AddSingleton<MongoDbContext>();

// CORS servisini BURAYA (Yukarı) aldık:
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAll", policy =>
    {
        policy.AllowAnyOrigin()
               .AllowAnyMethod()
               .AllowAnyHeader();
    });
});

// --- 2. UYGULAMAYI İNŞA ET ---
var app = builder.Build(); 

// --- 3. İSTEK HATTINI YAPILANDIR (app.Build'den sonra olmalı!) ---
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

// CORS'u aktif et (MapControllers'dan önce olması iyidir)
app.UseCors("AllowAll");

app.MapControllers();

// WeatherForecast (İstersen silebilirsin, kalabilir de)
app.MapGet("/weatherforecast", () => { return "Nightbrate API is running!"; })
   .WithName("GetWeatherForecast")
   .WithOpenApi();

app.Run();