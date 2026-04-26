🚀 NutriBridge (Nightbrate) - Akıllı Diyet ve Takip Sistemi
NutriBridge, diyetisyenler ve danışanlar arasındaki etkileşimi dijitalleştiren, öğün takibi, sistem analitiği ve yapay zeka destekli süreç yönetimini birleştiren kapsamlı bir sağlık platformudur.

📌 Proje Özellikleri
Admin Paneli: Diyetisyen onayı, sistem analitiği ve kullanıcı yönetimi.

Diyetisyen Paneli: Danışan atama (Takip Kodu ile), kişiselleştirilmiş diyet programı hazırlama ve öğün tamamlama takibi.

Mobil Uygulama (Android): Danışanlar için öğün kaydı, su/kilo takibi ve diyetisyen etkileşimi.

Güvenlik: JWT tabanlı kimlik doğrulama ve HMACSHA512 şifreleme.

Veritabanı: Esnek ve hızlı veri akışı için MongoDB.

🛠️ Kullanılan Teknolojiler
Backend: .NET 8 Web API, Entity Framework Core (MongoDB Provider).

Web (Frontend): React.js, Tailwind CSS, Recharts (Analitik Grafikler).

Mobil: Kotlin, Android SDK, Retrofit (API İletişimi).

Veritabanı: MongoDB.

🏗️ Mimari Yapı
Proje, katmanlı bir mimari (N-Tier Architecture) üzerine kurulmuştur:

Presentation Layer: React Web & Android App.

Business Logic Layer: .NET 8 Services & Auth Management.

Data Access Layer: MongoDB Repository Pattern.

📦 Kurulum ve Çalıştırma (Adım Adım)
Projeyi yerel bilgisayarınızda çalıştırmak için aşağıdaki adımları izleyin:

1. Ön Gereksinimler
Bilgisayarınızda şunların kurulu olduğundan emin olun:

.NET 8 SDK

Node.js (v18 veya üzeri)

MongoDB Community Server

Android Studio

2. Backend Kurulumu
Bash
cd NightbrateBackend
# appsettings.json dosyasındaki ConnectionString'i kendi MongoDB adresinize göre güncelleyin.
dotnet restore
dotnet run
API varsayılan olarak http://localhost:5000 adresinde çalışacaktır.

3. Web (React) Kurulumu
Bash
cd NightbrateWeb
npm install
npm start
Panel http://localhost:3000 adresinde açılacaktır.

4. Mobil (Android) Kurulumu
Android Studio'yu açın.

NightbrateAndroid klasörünü projeye dahil edin.

RetrofitClient içindeki BASE_URL adresini kendi yerel IP'niz veya emülatör köprüsü (http://10.0.2.2:5000) ile güncelleyin.

Projeyi bir emülatör veya gerçek cihazda çalıştırın.

📈 Veri Akışı ve Kullanım
Admin Girişi: Varsayılan admin bilgileriyle giriş yapın, bekleyen diyetisyenleri onaylayın.

Diyetisyen Kaydı: Onaylanan diyetisyen paneline girer, kendisine özel üretilen Bağlantı Kodu'nu alır.

Danışan Bağlantısı: Danışan mobil uygulamadan bu kodu girerek diyetisyenine bağlanır.

Takip: Danışan öğünlerini tamamladıkça diyetisyen panelinde grafikler ve durumlar güncellenir.
