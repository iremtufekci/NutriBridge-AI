package com.example.nightbrate // Paket tanımı

data class DietitianRegisterRequest( // Diyetisyen kayıt isteği gövdesi
    val firstName: String, // Ad
    val lastName: String, // Soyad
    val email: String, // E-posta
    val password: String, // Şifre
    val diplomaNo: String, // Diploma numarası
    val clinicName: String // Klinik/kurum adı
)
