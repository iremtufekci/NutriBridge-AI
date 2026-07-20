package com.example.nightbrate // Paket

import okhttp3.MultipartBody // Dosya parçası (PDF, fotoğraf)
import okhttp3.RequestBody // Metin form alanı
import retrofit2.Response // HTTP cevap sarmalayıcı (isSuccessful, body)
import retrofit2.http.Body // JSON gövde
import retrofit2.http.GET // GET isteği
import retrofit2.http.Multipart // Çok parçalı form (dosya yükleme)
import retrofit2.http.Part // Form parçası
import retrofit2.http.POST // POST isteği
import retrofit2.http.PATCH // Kısmi güncelleme
import retrofit2.http.Path // URL'deki {id} parametresi
import retrofit2.http.Query // ?key=value sorgu parametresi

/** Backend REST uç noktalarının Kotlin arayüzü — RetrofitClient.instance üzerinden çağrılır */
interface ApiService {

    // --- Kimlik doğrulama ---
    @POST("api/Auth/login") // Giriş
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse> // E-posta+şifre → token+rol

    @Multipart // Diploma dosyası ile birlikte
    @POST("api/Auth/register-dietitian") // Diyetisyen kaydı
    suspend fun registerDietitian(
        @Part("firstName") firstName: RequestBody, // Ad
        @Part("lastName") lastName: RequestBody, // Soyad
        @Part("email") email: RequestBody, // E-posta
        @Part("password") password: RequestBody, // Şifre
        @Part("diplomaNo") diplomaNo: RequestBody, // Diploma no
        @Part("clinicName") clinicName: RequestBody, // Klinik adı
        @Part diploma: MultipartBody.Part // Diploma PDF/görsel dosyası
    ): Response<Unit>

    @POST("api/Auth/register-client") // Danışan kaydı
    suspend fun registerClient(@Body request: ClientRegisterRequest): Response<Unit>

    // --- Danışan: PDF ---
    @GET("api/Client/pdf-analyses") // Geçmiş PDF analizleri listesi
    suspend fun getClientPdfAnalyses(@Query("take") take: Int = 30): Response<List<ClientPdfAnalysisResponseDto>>

    @Multipart
    @POST("api/Client/pdf-analyses/upload") // Yeni PDF yükle + Gemini analiz
    suspend fun uploadClientPdf(@Part pdf: MultipartBody.Part): Response<ClientPdfAnalysisResponseDto>

    // --- Danışan: diyet programı ---
    @GET("api/Client/diet-programs") // Tüm geçmiş program günleri
    suspend fun getMyDietPrograms(): Response<List<ClientDietProgramDayResponse>>

    @GET("api/Client/diet-program") // Belirli bir günün programı (yyyy-MM-dd)
    suspend fun getMyDietProgramForDate(@Query("programDate") programDate: String): Response<ClientDietProgramDayResponse>

    @POST("api/Client/diet-program/meal-completed") // Öğünü tamamladım işareti
    suspend fun markMealCompleted(@Body body: SetMealCompletedRequest): Response<Unit>

    // --- Danışan: yemek fotoğrafı ---
    @Multipart
    @POST("api/Meal/upload-meal-photo") // Fotoğraf yükle → AI kalori analizi
    suspend fun uploadMealPhoto(@Part photo: MultipartBody.Part): Response<MealPhotoAnalysisResponse>

    // --- Danışan: profil ---
    @GET("api/Client/profile") // Profil bilgisi oku
    suspend fun getClientProfile(): Response<ClientProfileResponse>

    @POST("api/Client/profile") // Profil güncelle
    suspend fun updateClientProfile(@Body body: UpdateClientProfileRequest): Response<Unit>

    @POST("api/Client/theme") // Danışan tema tercihi
    suspend fun updateClientTheme(@Body request: UpdateThemeRequest): Response<Unit>

    @POST("api/Auth/theme") // Genel kullanıcı tema (diyetisyen/admin)
    suspend fun updateAuthTheme(@Body request: UpdateThemeRequest): Response<Unit>

    @POST("api/Client/preview-dietitian-by-code") // Diyetisyen kodunu doğrula (bağlanmadan önce)
    suspend fun previewDietitianByCode(@Body request: ConnectToDietitianRequest): Response<PreviewDietitianByCodeResult>

    @POST("api/Client/connect-to-dietitian") // Diyetisyene bağlan
    suspend fun connectToDietitian(@Body request: ConnectToDietitianRequest): Response<ConnectToDietitianResult>

    @GET("api/Auth/profile") // Giriş yapan kullanıcının genel profili
    suspend fun getCurrentUserProfile(): Response<CurrentUserProfileResponse>

    // --- Admin ---
    @GET("api/Admin/dashboard-stats") // Özet istatistik kartları
    suspend fun getAdminDashboardStats(): Response<DashboardStatsResponse>

    @GET("api/Admin/pending-dietitians") // Onay bekleyen diyetisyenler
    suspend fun getPendingDietitians(): Response<List<PendingDietitianItem>>

    @GET("api/Admin/dietitian/{dietitianId}") // Tek diyetisyen detayı
    suspend fun getAdminDietitianDetail(@Path("dietitianId") dietitianId: String): Response<AdminDietitianDetailDto>

    @GET("api/Admin/recent-activities") // Son sistem aktiviteleri
    suspend fun getRecentActivities(@Query("take") take: Int = 15): Response<List<ActivityItemDto>>

    @GET("api/Admin/system-analytics") // Grafik/rapor verileri
    suspend fun getSystemAnalytics(): Response<SystemAnalyticsResponse>

    @GET("api/Admin/user-management/stats") // Kullanıcı sayıları özeti
    suspend fun getUserManagementStats(): Response<UserManagementStatsResponse>

    @GET("api/Admin/user-management/users") // Filtrelenebilir kullanıcı listesi
    suspend fun getUserManagementUsers(
        @Query("q") q: String? = null, // Arama metni
        @Query("role") role: String? = null, // Rol filtresi
        @Query("status") status: String? = null // Aktif/askıda
    ): Response<List<AdminUserRowItem>>

    @GET("api/Admin/user-management/{userId}/activity-logs") // Kullanıcının işlem geçmişi
    suspend fun getUserActivityLogs(@Path("userId") userId: String, @Query("take") take: Int = 30): Response<List<ActivityItemDto>>

    @POST("api/Admin/user-management/{userId}/suspend") // Hesabı askıya al
    suspend fun suspendUser(@Path("userId") userId: String, @Body body: SetUserSuspensionRequest): Response<Unit>

    @POST("api/Admin/user-management/{userId}/unsuspend") // Askıyı kaldır
    suspend fun unsuspendUser(@Path("userId") userId: String): Response<Unit>

    @POST("api/Admin/approve-dietitian/{dietitianId}") // Diyetisyeni onayla
    suspend fun approveDietitian(@Path("dietitianId") dietitianId: String): Response<Unit>

    // --- Diyetisyen ---
    @GET("api/Dietitian/critical-alerts") // Kritik uyarı listesi
    suspend fun getDietitianCriticalAlerts(): Response<List<DietitianCriticalAlertDto>>

    @GET("api/Dietitian/daily-tasks/today") // Bugünkü görevler
    suspend fun getTodayDailyTasks(): Response<DietitianTodayTasksBundleDto>

    @PATCH("api/Dietitian/daily-tasks/{taskId}/complete") // Görevi tamamla/geri al
    suspend fun setDailyTaskComplete(@Path("taskId") taskId: String, @Body body: SetDietitianTaskCompleteBody): Response<Unit>

    @GET("api/Dietitian/clients-with-last-meal") // Danışan + son öğün bilgisi (dashboard)
    suspend fun getClientsWithLastMeal(): Response<List<ClientWithLastMealItem>>

    @GET("api/Dietitian/diet-program-dates") // Danışanın program yazılmış günleri
    suspend fun getDietProgramDates(@Query("clientId") clientId: String): Response<List<String>>

    @GET("api/Dietitian/diet-program") // Belirli gün programını oku
    suspend fun getDietProgram(@Query("clientId") clientId: String, @Query("programDate") programDate: String): Response<DietProgramViewResponse>

    @POST("api/Dietitian/diet-program") // Program kaydet
    suspend fun saveDietProgram(@Body body: SaveDietProgramRequest): Response<Unit>

    // --- AI mutfak şefi ---
    @POST("api/KitchenChef/generate") // Malzemelerden tarif üret (Gemini)
    suspend fun generateKitchenRecipes(@Body body: KitchenChefGenerateRequest): Response<KitchenChefGenerateResponse>

    @POST("api/KitchenChef/save") // Seçilen tarifi diyetisyene paylaş
    suspend fun saveKitchenRecipes(@Body body: KitchenChefSaveRequest): Response<Unit>

    @GET("api/KitchenChef/share-status") // Bugün paylaşım hakkı var mı (günde 1)
    suspend fun getKitchenShareStatus(): Response<KitchenChefDailyShareStatusDto>

    @GET("api/KitchenChef/my-shares") // Danışanın paylaşım geçmişi
    suspend fun getMyKitchenShares(
        @Query("from") from: String? = null, // Tarih aralığı başı
        @Query("to") to: String? = null, // Tarih aralığı sonu
        @Query("source") source: String? = null, // Kaynak filtresi
        @Query("skip") skip: Int = 0, // Sayfalama
        @Query("take") take: Int = 50
    ): Response<List<KitchenChefShareLogItem>>

    @GET("api/Dietitian/my-clients") // Danışan listesi (sıralama/sekme)
    suspend fun getMyClients(@Query("sort") sort: String = "nameAsc", @Query("tab") tab: String = "all"): Response<DietitianMyClientsResponseDto>

    @GET("api/Dietitian/client-overview") // Danışan detay özeti
    suspend fun getClientOverview(@Query("clientId") clientId: String): Response<DietitianClientOverviewDto>

    @GET("api/Dietitian/client-kitchen-recipe-logs") // Danışanın AI tarif logları (diyetisyen görünümü)
    suspend fun getClientKitchenRecipeLogs(@Query("clientId") clientId: String, @Query("take") take: Int = 30): Response<List<KitchenChefShareLogItem>>

    @POST("api/Dietitian/acknowledge-critical-alert") // Uyarıyı okundu işaretle
    suspend fun acknowledgeCriticalAlert(@Body body: AckCriticalAlertRequest): Response<Unit>

    @GET("api/Dietitian/client-brief") // Kısa danışan kartı bilgisi
    suspend fun getDietitianClientBrief(@Query("clientId") clientId: String): Response<DietitianClientBriefDto>
}
