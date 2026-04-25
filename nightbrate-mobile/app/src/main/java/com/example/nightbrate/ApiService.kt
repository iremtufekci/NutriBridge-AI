package com.example.nightbrate

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

    @POST("api/Client/preview-dietitian-by-code")
    suspend fun previewDietitianByCode(
        @Body request: ConnectToDietitianRequest
    ): Response<PreviewDietitianByCodeResult>

    @POST("api/Client/connect-to-dietitian")
    suspend fun connectToDietitian(
        @Body request: ConnectToDietitianRequest
    ): Response<ConnectToDietitianResult>

    @GET("api/Auth/profile")
    suspend fun getCurrentUserProfile(): Response<CurrentUserProfileResponse>

    @GET("api/Admin/dashboard-stats")
    suspend fun getAdminDashboardStats(): Response<DashboardStatsResponse>

    @GET("api/Admin/pending-dietitians")
    suspend fun getPendingDietitians(): Response<List<PendingDietitianItem>>

    @GET("api/Admin/recent-activities")
    suspend fun getRecentActivities(
        @Query("take") take: Int = 15
    ): Response<List<ActivityItemDto>>

    @POST("api/Admin/approve-dietitian/{dietitianId}")
    suspend fun approveDietitian(
        @Path("dietitianId") dietitianId: String
    ): Response<Unit>
}
