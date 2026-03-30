package com.example.nightbrate

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/user/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}