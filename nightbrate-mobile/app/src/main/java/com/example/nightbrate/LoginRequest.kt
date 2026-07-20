package com.example.nightbrate // Paket tanımı

data class LoginRequest( // Giriş API isteği gövdesi
    val email: String, // Kullanıcı e-posta adresi
    val password: String // Kullanıcı şifresi
)
