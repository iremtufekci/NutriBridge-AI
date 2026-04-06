using Nightbrate.Infrastructure.Data;

var builder = WebApplication.CreateBuilder(args);

// --- 1. SERVİS AYARLARI (builder.Build'den önce) ---

// CORS Politikasını Tanımla
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAll", policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyMethod()
              .AllowAnyHeader();
    });
});

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Veritabanı Bağlantısı
builder.Services.AddSingleton<MongoDbContext>();

var app = builder.Build();

// --- 2. MIDDLEWARE YAPILANDIRMASI (Sıralama Çok Önemli!) ---

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

// 1. Önce CORS (Her şeyden önce gelmeli)
app.UseCors("AllowAll");

// 2. Varsa HTTPS yönlendirmesi (Geliştirme aşamasında bazen sorun çıkarabilir, kapalı tutabilirsin)
// app.UseHttpsRedirection(); 

// 3. Auth işlemleri (Controller'lardan önce gelmeli)
app.UseAuthorization();

// 4. En son rotalar
app.MapControllers();

app.Run();