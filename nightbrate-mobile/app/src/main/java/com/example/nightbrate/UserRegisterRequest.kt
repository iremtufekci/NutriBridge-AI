package com.example.nightbrate

/**
 * Kullanıcı kayıt bilgilerini tutan veri sınıfı.
 * role = 1 (Danışan), role = 2 (Diyetisyen) olarak düşünebilirsin.
 */
data class UserRegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val role: Int
)