package com.example.nightbrate

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PATCH
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

    @GET("api/Client/diet-programs")
    suspend fun getMyDietPrograms(): Response<List<ClientDietProgramDayResponse>>

    /** Tek gün programı (web ClientHome takvim seçimi). */
    @GET("api/Client/diet-program")
    suspend fun getMyDietProgramForDate(
        @Query("programDate") programDate: String
    ): Response<ClientDietProgramDayResponse>

    @POST("api/Client/diet-program/meal-completed")
    suspend fun markMealCompleted(
        @Body body: SetMealCompletedRequest
    ): Response<Unit>

    @Multipart
    @POST("api/Meal/upload-meal-photo")
    suspend fun uploadMealPhoto(
        @Part photo: MultipartBody.Part
    ): Response<MealPhotoAnalysisResponse>

    @GET("api/Client/profile")
    suspend fun getClientProfile(): Response<ClientProfileResponse>

    @POST("api/Client/profile")
    suspend fun updateClientProfile(
        @Body body: UpdateClientProfileRequest
    ): Response<Unit>

    @POST("api/Client/theme")
    suspend fun updateClientTheme(
        @Body request: UpdateThemeRequest
    ): Response<Unit>

    @POST("api/Auth/theme")
    suspend fun updateAuthTheme(
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

    @GET("api/Admin/dietitian/{dietitianId}")
    suspend fun getAdminDietitianDetail(
        @Path("dietitianId") dietitianId: String
    ): Response<AdminDietitianDetailDto>

    @GET("api/Admin/recent-activities")
    suspend fun getRecentActivities(
        @Query("take") take: Int = 15
    ): Response<List<ActivityItemDto>>

    @GET("api/Admin/system-analytics")
    suspend fun getSystemAnalytics(): Response<SystemAnalyticsResponse>

    @GET("api/Admin/user-management/stats")
    suspend fun getUserManagementStats(): Response<UserManagementStatsResponse>

    @GET("api/Admin/user-management/users")
    suspend fun getUserManagementUsers(
        @Query("q") q: String? = null,
        @Query("role") role: String? = null,
        @Query("status") status: String? = null
    ): Response<List<AdminUserRowItem>>

    @GET("api/Admin/user-management/{userId}/activity-logs")
    suspend fun getUserActivityLogs(
        @Path("userId") userId: String,
        @Query("take") take: Int = 30
    ): Response<List<ActivityItemDto>>

    @POST("api/Admin/user-management/{userId}/suspend")
    suspend fun suspendUser(
        @Path("userId") userId: String,
        @Body body: SetUserSuspensionRequest
    ): Response<Unit>

    @POST("api/Admin/user-management/{userId}/unsuspend")
    suspend fun unsuspendUser(
        @Path("userId") userId: String
    ): Response<Unit>

    @POST("api/Admin/approve-dietitian/{dietitianId}")
    suspend fun approveDietitian(
        @Path("dietitianId") dietitianId: String
    ): Response<Unit>

    @GET("api/Dietitian/critical-alerts")
    suspend fun getDietitianCriticalAlerts(): Response<List<DietitianCriticalAlertStub>>

    @GET("api/Dietitian/daily-tasks/today")
    suspend fun getTodayDailyTasks(): Response<DietitianTodayTasksBundleDto>

    @PATCH("api/Dietitian/daily-tasks/{taskId}/complete")
    suspend fun setDailyTaskComplete(
        @Path("taskId") taskId: String,
        @Body body: SetDietitianTaskCompleteBody
    ): Response<Unit>

    @GET("api/Dietitian/clients-with-last-meal")
    suspend fun getClientsWithLastMeal(): Response<List<ClientWithLastMealItem>>

    @GET("api/Dietitian/diet-program-dates")
    suspend fun getDietProgramDates(
        @Query("clientId") clientId: String
    ): Response<List<String>>

    @GET("api/Dietitian/diet-program")
    suspend fun getDietProgram(
        @Query("clientId") clientId: String,
        @Query("programDate") programDate: String
    ): Response<DietProgramViewResponse>

    @POST("api/KitchenChef/generate")
    suspend fun generateKitchenRecipes(
        @Body body: KitchenChefGenerateRequest
    ): Response<KitchenChefGenerateResponse>

    @GET("api/KitchenChef/my-shares")
    suspend fun getMyKitchenShares(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("source") source: String? = null,
        @Query("skip") skip: Int = 0,
        @Query("take") take: Int = 50
    ): Response<List<KitchenChefShareLogItem>>

