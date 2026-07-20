package com.example.nightbrate // Paket tanımı

/**
 * Kullanıcı kayıt bilgilerini tutan veri sınıfı.
 * role = 1 (Danışan), role = 2 (Diyetisyen) olarak düşünebilirsin.
 */
data class UserRegisterRequest( // Genel kullanıcı kayıt isteği
    val username: String, // Kullanıcı adı
    val email: String, // E-posta adresi
    val password: String, // Şifre
    val role: Int // Rol kodu (1=danışan, 2=diyetisyen)
)
