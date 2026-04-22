package com.example.nightbrate

import android.content.Context.MODE_PRIVATE
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = try {
            NightstrateApp.instance
                .getSharedPreferences("auth", MODE_PRIVATE)
                .getString("token", null)
        } catch (_: Exception) {
            null
        } ?: return chain.proceed(chain.request())

        val req = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(req)
    }
}
