package com.example.nightbrate // Paket

import android.content.Context.MODE_PRIVATE // Uygulama içi gizli depolama modu
import okhttp3.Interceptor // OkHttp istek zinciri kancası
import okhttp3.Response // HTTP cevabı

class AuthInterceptor : Interceptor { // Giriş token'ını otomatik ekleyen sınıf
    override fun intercept(chain: Interceptor.Chain): Response { // Her HTTP isteğinde çalışır
        val token = try {
            NightstrateApp.instance // Application context
                .getSharedPreferences("auth", MODE_PRIVATE) // Girişte kaydedilen prefs
                .getString("token", null) // JWT string veya null
        } catch (_: Exception) {
            null // Uygulama henüz hazır değilse token yok say
        } ?: return chain.proceed(chain.request()) // Token yoksa isteği olduğu gibi gönder (login vb.)

        val req = chain.request().newBuilder() // Mevcut isteği kopyala
            .addHeader("Authorization", "Bearer $token") // JWT başlığı ekle
            .build()
        return chain.proceed(req) // Güncellenmiş isteği sunucuya ilet
    }
}
