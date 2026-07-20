package com.example.nightbrate // Paket tanımı

/** /api/Admin/system-analytics yanıtı (Gson, ek alanlar yok sayılır). */
data class SystemAnalyticsResponse( // Sistem analitik ana yanıt
    val kpis: SystemKpiBlock, // Özet KPI bloğu
    val endpointPerformance: List<EndpointPerformanceRow>, // Endpoint performans satırları
    val databaseHourly: List<HourlyDbRow>?, // Saatlik veritabanı metrikleri
    val cacheHourly: List<HourlyCacheRow>?, // Saatlik önbellek metrikleri
    val networkHourly: List<HourlyNetRow>?, // Saatlik ağ metrikleri
    val systemResources: SystemResourcesBlock?, // CPU/bellek/disk kaynakları
    val errorLogs: List<ErrorLogEntry>?, // Hata günlükleri
    val securityEvents: List<SecurityEventEntry>?, // Güvenlik olayları
    val dataWindowHours: Int?, // Veri penceresi (saat)
    val generatedAtUtc: String?, // Üretim zamanı UTC
    val dataNote: String? // Veri notu/açıklama
)

data class SystemKpiBlock( // Üst düzey KPI değerleri
    val apiRequestsPerHour: Int, // Saatlik API istek sayısı
    val apiRequestsPerHourDeltaPercent: Double, // İstek değişim yüzdesi
    val avgQueryTimeMs: Int, // Ortalama sorgu süresi (ms)
    val avgQueryTimeDeltaPercent: Double, // Sorgu süresi değişim yüzdesi
    val securityScore: Double, // Güvenlik skoru
    val securityOpenIssues: Int, // Açık güvenlik sorunu sayısı
    val cacheHitRatioPercent: Double, // Önbellek isabet oranı
    val cacheStatusLabel: String? // Önbellek durum etiketi
)

data class EndpointPerformanceRow( // Tek endpoint performans satırı
    val endpoint: String, // API yolu
    val calls: Int, // Çağrı sayısı
    val avgTimeMs: Int, // Ortalama yanıt süresi
    val errors: Int, // Hata sayısı
    val successRatePercent: Double // Başarı oranı yüzdesi
)

data class HourlyDbRow(val hour: Int, val label: String, val reads: Int, val writes: Int, val slowQueries: Int) // Saatlik DB satırı
data class HourlyCacheRow(val hour: Int, val label: String, val hits: Int, val misses: Int) // Saatlik önbellek satırı
data class HourlyNetRow(val hour: Int, val label: String, val incomingMbps: Double, val outgoingMbps: Double) // Saatlik ağ satırı

data class SystemResourcesBlock( // Sistem kaynak kullanımı
    val cpuPercent: Double, // CPU kullanım yüzdesi
    val memoryPercent: Double, // Bellek kullanım yüzdesi
    val memoryRefLabel: String?, // Bellek referans etiketi
    val diskIoPercent: Double, // Disk I/O yüzdesi
    val diskRefLabel: String?, // Disk referans etiketi
    val networkMbps: Double, // Toplam ağ Mbps
    val networkUp: Double, // Yükleme Mbps
    val networkDown: Double, // İndirme Mbps
    val networkNote: String? // Ağ notu
)

data class ErrorLogEntry( // Hata günlük kaydı
    val statusCode: Int, // HTTP durum kodu
    val time: String, // Olay zamanı
    val endpoint: String, // Hatalı endpoint
    val message: String, // Hata mesajı
    val count: Int // Tekrar sayısı
)

data class SecurityEventEntry( // Güvenlik olay kaydı
    val severity: String, // Önem derecesi
    val time: String, // Olay zamanı
    val name: String, // Olay adı
    val obfuscatedSource: String, // Maskelenmiş kaynak
    val countLabel: String, // Sayım etiketi
    val tone: String // Görsel ton (renk/stil)
)
