package com.example.nightbrate // Uygulama paket adı

import okhttp3.OkHttpClient // HTTP istemci (Retrofit altında çalışır)
import okhttp3.logging.HttpLoggingInterceptor // Logcat'e istek/cevap yazdırır
import retrofit2.Retrofit // REST API arayüz fabrikası
import retrofit2.converter.gson.GsonConverterFactory // JSON ↔ Kotlin nesne dönüşümü
import java.util.concurrent.TimeUnit // Zaman aşımı birimleri

object RetrofitClient { // Tekil (singleton) API istemcisi — her yerden RetrofitClient.instance
    const val API_BASE_URL = "http://10.0.2.2:5231/" // Emülatörde bilgisayarın localhost'u (5231 portu)
    private const val BASE_URL = API_BASE_URL // Retrofit baseUrl sabiti

    private val logging = HttpLoggingInterceptor().apply { // İstek gövdesini Logcat'te görmek için
        level = HttpLoggingInterceptor.Level.BODY // URL + header + body tam log
    }

    private val client = OkHttpClient.Builder() // Özelleştirilmiş HTTP istemci
        .addInterceptor(AuthInterceptor()) // Her isteğe JWT token ekler
        .addInterceptor(logging) // Debug logları
        .connectTimeout(30, TimeUnit.SECONDS) // Bağlantı kurma süresi
        .readTimeout(120, TimeUnit.SECONDS) // Cevap okuma (Gemini uzun sürebilir)
        .writeTimeout(120, TimeUnit.SECONDS) // Gönderme süresi
        .callTimeout(200, TimeUnit.SECONDS) // Toplam çağrı süresi üst sınırı
        .build()

    val instance: ApiService by lazy { // İlk kullanımda oluşturulur, sonra aynı örnek
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL) // api/Auth/login → http://10.0.2.2:5231/api/Auth/login
            .client(client) // Yukarıdaki OkHttpClient
            .addConverterFactory(GsonConverterFactory.create()) // JSON parse
            .build()
        retrofit.create(ApiService::class.java) // ApiService arayüzünün canlı implementasyonu
    }
}
