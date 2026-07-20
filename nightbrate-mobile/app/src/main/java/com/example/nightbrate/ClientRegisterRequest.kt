package com.example.nightbrate // Paket tanımı

data class ClientRegisterRequest( // Danışan kayıt isteği gövdesi
    val firstName: String, // Ad
    val lastName: String, // Soyad
    val email: String, // E-posta
    val password: String, // Şifre
    val weight: Double, // Kilo (kg)
    val height: Double, // Boy (cm)
    val targetCalories: Int, // Günlük hedef kalori
    val dietitianId: String? = null // Opsiyonel bağlı diyetisyen kimliği
)
