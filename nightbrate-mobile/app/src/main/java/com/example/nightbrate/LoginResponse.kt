package com.example.nightbrate // Paket tanımı

data class LoginResponse( // Giriş API yanıt gövdesi
    val token: String, // JWT erişim belirteci
    val role: String // Kullanıcı rolü (client/dietitian/admin)
)
