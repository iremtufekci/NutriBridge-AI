package com.example.nightbrate

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    // 1. MEVCUT: Kullanıcı Girişi (Admin, Diyetisyen veya Danışan için)
    @POST("api/User/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    // 2. YENİ: Diyetisyen Kayıt Başvurusu
    @POST("api/Auth/register-dietitian")
    suspend fun registerDietitian(
        @Body request: DietitianRegisterRequest
    ): Response<Unit>

    // 3. YENİ: Danışan Kaydı
    @POST("api/Auth/register-client")
    suspend fun registerClient(
        @Body request: UserRegisterRequest
    ): Response<Unit>
}
