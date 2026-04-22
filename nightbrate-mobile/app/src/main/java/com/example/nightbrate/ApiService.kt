package com.example.nightbrate

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("api/Auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/Auth/register-dietitian")
    suspend fun registerDietitian(
        @Body request: DietitianRegisterRequest
    ): Response<Unit>

    @POST("api/Auth/register-client")
    suspend fun registerClient(
        @Body request: ClientRegisterRequest
    ): Response<Unit>

    @GET("api/Client/profile")
    suspend fun getClientProfile(): Response<ClientProfileResponse>

    @POST("api/Client/theme")
    suspend fun updateClientTheme(
        @Body request: UpdateThemeRequest
    ): Response<Unit>
}
