package com.example.nightbrate // Paket

import com.google.gson.annotations.SerializedName // JSON alan adı eşlemesi

data class ClientProfileResponse( // Danışan profil yanıtı
    @SerializedName("firstName") val firstName: String? = null, // Ad
    @SerializedName("lastName") val lastName: String? = null, // Soyad
    @SerializedName("weight") val weight: Double = 0.0, // Kilo (kg)
    @SerializedName("height") val height: Double = 0.0, // Boy (cm)
    @SerializedName("targetCalories") val targetCalories: Int = 2000, // Günlük kalori hedefi
    @SerializedName("goalText") val goalText: String? = null, // Hedef açıklaması
    @SerializedName("themePreference") val themePreference: String? = "light", // Tema tercihi
    @SerializedName("dietitianName") val dietitianName: String? = null, // Bağlı diyetisyen adı
    @SerializedName("programStartDate") val programStartDate: String? = null // Program başlangıç tarihi
)

data class UpdateClientProfileRequest( // Profil güncelleme isteği
    @SerializedName("firstName") val firstName: String, // Ad
    @SerializedName("lastName") val lastName: String, // Soyad
    @SerializedName("weight") val weight: Double, // Kilo (kg)
    @SerializedName("height") val height: Double, // Boy (cm)
    @SerializedName("targetCalories") val targetCalories: Int // Günlük kalori hedefi
)

data class UpdateThemeRequest( // Tema güncelleme isteği
    @SerializedName("themePreference") val themePreference: String // Tema tercihi (light/dark)
)

data class ConnectToDietitianRequest( // Diyetisyene bağlanma isteği
    @SerializedName("connectionCode") val connectionCode: String // Bağlantı kodu
)

data class PreviewDietitianByCodeResult( // Kod ile diyetisyen önizleme yanıtı
    @SerializedName("firstName") val firstName: String? = null, // Ad
    @SerializedName("lastName") val lastName: String? = null, // Soyad
    @SerializedName("displayName") val displayName: String? = null // Görünen ad
)

data class ConnectToDietitianResult( // Diyetisyene bağlanma sonucu
    @SerializedName("message") val message: String? = null, // Bilgi mesajı
    @SerializedName("firstName") val firstName: String? = null, // Diyetisyen adı
    @SerializedName("lastName") val lastName: String? = null // Diyetisyen soyadı
)

data class DashboardStatsResponse( // Admin panel istatistik yanıtı
    @SerializedName("totalUsers") val totalUsers: Long = 0, // Toplam kullanıcı
    @SerializedName("activeUsers") val activeUsers: Long = 0, // Aktif kullanıcı
    @SerializedName("totalClients") val totalClients: Long = 0, // Toplam danışan
    @SerializedName("totalDietitians") val totalDietitians: Long = 0, // Toplam diyetisyen
    @SerializedName("activeDietitians") val activeDietitians: Long = 0, // Aktif diyetisyen
    @SerializedName("pendingDietitians") val pendingDietitians: Long = 0, // Onay bekleyen diyetisyen
    @SerializedName("roleDistribution") val roleDistribution: List<RoleCountItem> = emptyList(), // Rol dağılımı
    @SerializedName("monthlyRegistrations") val monthlyRegistrations: List<MonthlyRegistrationItem> = emptyList() // Aylık kayıtlar
)

data class RoleCountItem( // Rol sayım öğesi
    @SerializedName("role") val role: String? = null, // Rol adı
    @SerializedName("count") val count: Long = 0 // Kullanıcı sayısı
)

data class MonthlyRegistrationItem( // Aylık kayıt öğesi
    @SerializedName("year") val year: Int = 0, // Yıl
    @SerializedName("month") val month: Int = 0, // Ay
    @SerializedName("count") val count: Long = 0 // Kayıt sayısı
)

data class ActivityItemDto( // Son aktivite kaydı
    @SerializedName("id") val id: String? = null, // Kayıt kimliği
    @SerializedName("initial") val initial: String? = null, // Avatar baş harfi
    @SerializedName("actorDisplayName") val actorDisplayName: String? = null, // İşlemi yapan kişi
    @SerializedName("description") val description: String? = null, // Aktivite açıklaması
    @SerializedName("createdAt") val createdAt: String? = null // Oluşturulma zamanı
)

data class PendingDietitianItem( // Onay bekleyen diyetisyen satırı
    @SerializedName("id") val id: String? = null, // Diyetisyen kimliği
    @SerializedName("firstName") val firstName: String? = null, // Ad
    @SerializedName("lastName") val lastName: String? = null, // Soyad
    @SerializedName("email") val email: String? = null, // E-posta
    @SerializedName("diplomaNo") val diplomaNo: String? = null, // Diploma numarası
    @SerializedName("clinicName") val clinicName: String? = null, // Klinik adı
    @SerializedName("createdAt") val createdAt: String? = null, // Başvuru tarihi
    @SerializedName("isApproved") val isApproved: Boolean? = null // Onay durumu
)

/** GET api/Admin/dietitian/{id} — web AdminApprovals detay modali */
data class AdminDietitianDetailDto( // Admin diyetisyen detay yanıtı
    @SerializedName("id") val id: String? = null, // Diyetisyen kimliği
    @SerializedName("firstName") val firstName: String? = null, // Ad
    @SerializedName("lastName") val lastName: String? = null, // Soyad
    @SerializedName("diplomaNo") val diplomaNo: String? = null, // Diploma numarası
    @SerializedName("clinicName") val clinicName: String? = null, // Klinik adı
    @SerializedName("createdAt") val createdAt: String? = null, // Başvuru tarihi
    @SerializedName("isApproved") val isApproved: Boolean? = null, // Onay durumu
    @SerializedName("diplomaDocumentUrl") val diplomaDocumentUrl: String? = null // Diploma belgesi URL'si
)

data class CurrentUserProfileResponse( // Oturum açmış kullanıcı profili
    @SerializedName("email") val email: String? = null, // E-posta
    @SerializedName("role") val role: String? = null, // Kullanıcı rolü
    @SerializedName("firstName") val firstName: String? = null, // Ad
    @SerializedName("lastName") val lastName: String? = null, // Soyad
    @SerializedName("displayName") val displayName: String? = null, // Görünen ad
    @SerializedName("clinicName") val clinicName: String? = null, // Klinik adı
    @SerializedName("diplomaNo") val diplomaNo: String? = null, // Diploma numarası
    @SerializedName("themePreference") val themePreference: String? = "light", // Tema tercihi
    @SerializedName( // Bağlantı kodu (camelCase/PascalCase uyumu)
        value = "connectionCode", // JSON alan adı
        alternate = ["ConnectionCode"] // Alternatif PascalCase alan adı
    ) val connectionCode: String? = null // Diyetisyen bağlantı kodu
)

data class LastMealSummaryDto( // Son öğün özeti
    @SerializedName("photoUrl") val photoUrl: String? = null, // Yemek fotoğrafı URL'si
    @SerializedName("timestamp") val timestamp: String? = null // Kayıt zamanı
)

data class ClientWithLastMealItem( // Son öğünlü danışan kartı
    @SerializedName(value = "id", alternate = ["Id"]) val id: String? = null, // Danışan kimliği
    @SerializedName(value = "firstName", alternate = ["FirstName"]) val firstName: String? = null, // Ad
    @SerializedName(value = "lastName", alternate = ["LastName"]) val lastName: String? = null, // Soyad
    @SerializedName(value = "lastMeal", alternate = ["LastMeal"]) val lastMeal: LastMealSummaryDto? = null // Son öğün bilgisi
)

data class DietProgramViewResponse( // Diyetisyen program görünümü
    @SerializedName("clientId") val clientId: String? = null, // Danışan kimliği
    @SerializedName("programDate") val programDate: String? = null, // Program tarihi
    @SerializedName("breakfast") val breakfast: String? = null, // Kahvaltı içeriği
    @SerializedName("lunch") val lunch: String? = null, // Öğle yemeği içeriği
    @SerializedName("dinner") val dinner: String? = null, // Akşam yemeği içeriği
    @SerializedName("snack") val snack: String? = null, // Ara öğün içeriği
    @SerializedName("breakfastCalories") val breakfastCalories: Int? = 0, // Kahvaltı kalorisi
    @SerializedName("lunchCalories") val lunchCalories: Int? = 0, // Öğle kalorisi
    @SerializedName("dinnerCalories") val dinnerCalories: Int? = 0, // Akşam kalorisi
    @SerializedName("snackCalories") val snackCalories: Int? = 0, // Ara öğün kalorisi
    @SerializedName("totalCalories") val totalCalories: Int? = 0, // Toplam kalori
    @SerializedName("hasSavedProgram") val hasSavedProgram: Boolean? = null // Kayıtlı program var mı
)

data class ClientDietProgramDayResponse( // Danışan günlük diyet programı
    @SerializedName("programDate") val programDate: String? = null, // Program tarihi
    @SerializedName("breakfast") val breakfast: String? = null, // Kahvaltı içeriği
    @SerializedName("lunch") val lunch: String? = null, // Öğle yemeği içeriği
    @SerializedName("dinner") val dinner: String? = null, // Akşam yemeği içeriği
    @SerializedName("snack") val snack: String? = null, // Ara öğün içeriği
    @SerializedName("breakfastCalories") val breakfastCalories: Int? = 0, // Kahvaltı kalorisi
    @SerializedName("lunchCalories") val lunchCalories: Int? = 0, // Öğle kalorisi
    @SerializedName("dinnerCalories") val dinnerCalories: Int? = 0, // Akşam kalorisi
    @SerializedName("snackCalories") val snackCalories: Int? = 0, // Ara öğün kalorisi
    @SerializedName("totalCalories") val totalCalories: Int? = 0, // Toplam kalori
    @SerializedName("hasProgram") val hasProgram: Boolean? = null, // Program tanımlı mı
    @SerializedName("updatedAt") val updatedAt: String? = null, // Son güncelleme zamanı
    @SerializedName("dietitianName") val dietitianName: String? = null, // Diyetisyen adı
    @SerializedName("breakfastCompleted") val breakfastCompleted: Boolean? = false, // Kahvaltı tamamlandı mı
    @SerializedName("lunchCompleted") val lunchCompleted: Boolean? = false, // Öğle tamamlandı mı
    @SerializedName("dinnerCompleted") val dinnerCompleted: Boolean? = false, // Akşam tamamlandı mı
    @SerializedName("snackCompleted") val snackCompleted: Boolean? = false // Ara öğün tamamlandı mı
)

data class SetMealCompletedRequest( // Öğün tamamlama isteği
    @SerializedName("programDate") val programDate: String, // Program tarihi
    @SerializedName("meal") val meal: String // Öğün anahtarı (breakfast/lunch/dinner/snack)
)

data class SaveDietProgramRequest( // Diyet programı kaydetme isteği
    @SerializedName("clientId") val clientId: String, // Danışan kimliği
    @SerializedName("programDate") val programDate: String, // Program tarihi
    @SerializedName("breakfast") val breakfast: String, // Kahvaltı içeriği
    @SerializedName("lunch") val lunch: String, // Öğle yemeği içeriği
    @SerializedName("dinner") val dinner: String, // Akşam yemeği içeriği
    @SerializedName("snack") val snack: String, // Ara öğün içeriği
    @SerializedName("breakfastCalories") val breakfastCalories: Int, // Kahvaltı kalorisi
    @SerializedName("lunchCalories") val lunchCalories: Int, // Öğle kalorisi
    @SerializedName("dinnerCalories") val dinnerCalories: Int, // Akşam kalorisi
    @SerializedName("snackCalories") val snackCalories: Int, // Ara öğün kalorisi
    @SerializedName("totalCalories") val totalCalories: Int // Toplam kalori
)

data class UserManagementStatsResponse( // Kullanıcı yönetimi istatistikleri
    @SerializedName("totalUsers") val totalUsers: Int = 0, // Toplam kullanıcı
    @SerializedName("admins") val admins: Int = 0, // Admin sayısı
    @SerializedName("dietitians") val dietitians: Int = 0, // Diyetisyen sayısı
    @SerializedName("clients") val clients: Int = 0, // Danışan sayısı
    @SerializedName("active") val active: Int = 0, // Aktif kullanıcı
    @SerializedName("pending") val pending: Int = 0 // Onay bekleyen kullanıcı
)

data class AdminUserRowItem( // Admin kullanıcı listesi satırı
    @SerializedName("id") val id: String? = null, // Kullanıcı kimliği
    @SerializedName("displayName") val displayName: String? = null, // Görünen ad
    @SerializedName("initial") val initial: String? = null, // Avatar baş harfi
    @SerializedName("email") val email: String? = null, // E-posta
    @SerializedName("phone") val phone: String? = null, // Telefon
    @SerializedName("role") val role: String? = null, // Rol etiketi
    @SerializedName("roleKey") val roleKey: String? = null, // Rol anahtarı
    @SerializedName("statusKey") val statusKey: String? = null, // Durum anahtarı
    @SerializedName("statusLabel") val statusLabel: String? = null, // Durum etiketi
    @SerializedName("createdAt") val createdAt: String? = null, // Kayıt tarihi
    @SerializedName("lastActivityAt") val lastActivityAt: String? = null, // Son aktivite zamanı
    @SerializedName("isSuspended") val isSuspended: Boolean? = null // Askıya alınmış mı
)

data class SetUserSuspensionRequest( // Kullanıcı askıya alma isteği
    @SerializedName("message") val message: String // Askıya alma gerekçesi
)

data class MealPhotoAnalysisResponse( // Yemek fotoğrafı analiz yanıtı
    @SerializedName("mealLogId") val mealLogId: String? = null, // Öğün kaydı kimliği
    @SerializedName("photoUrl") val photoUrl: String? = null, // Fotoğraf URL'si
    @SerializedName("estimatedCalories") val estimatedCalories: Int = 0, // Tahmini kalori
    @SerializedName("detectedFoods") val detectedFoods: List<String> = emptyList(), // Algılanan yiyecekler
    @SerializedName("timestampUtc") val timestampUtc: String? = null, // Analiz zamanı (UTC)
    @SerializedName("analysisSource") val analysisSource: String? = null // Analiz kaynağı (AI/mock)
)

data class KitchenChefGenerateRequest( // Mutfak şefi tarif üretme isteği
    @SerializedName("ingredients") val ingredients: String, // Malzeme listesi
    @SerializedName("preference") val preference: String, // Tercih (vegan, düşük karb vb.)
    @SerializedName("targetCalories") val targetCalories: Int // Hedef kalori
)

data class KitchenChefGenerateResponse( // Mutfak şefi tarif üretme yanıtı
    @SerializedName("recipes") val recipes: List<KitchenChefRecipeItem> = emptyList(), // Üretilen tarifler
    @SerializedName("source") val source: String? = null // Kaynak (gemini/mock)
)

data class KitchenChefRecipeItem( // Tek tarif öğesi
    @SerializedName("title") val title: String = "", // Tarif başlığı
    @SerializedName("description") val description: String? = null, // Kısa açıklama
    @SerializedName("estimatedCalories") val estimatedCalories: Int = 0, // Tahmini kalori
    @SerializedName("prepTimeMinutes") val prepTimeMinutes: Int? = null, // Hazırlık süresi (dk)
    @SerializedName("ingredients") val ingredients: List<String> = emptyList(), // Malzemeler
    @SerializedName("steps") val steps: List<String> = emptyList() // Yapılış adımları
)

data class KitchenChefDailyShareStatusDto( // Günlük tarif paylaşım durumu
    @SerializedName("canShareToday") val canShareToday: Boolean = true, // Bugün paylaşılabilir mi
    @SerializedName("sharedToday") val sharedToday: Boolean = false // Bugün paylaşıldı mı
)

data class KitchenChefSaveRequest( // Tarif kaydetme isteği
    @SerializedName("ingredients") val ingredients: String, // Kullanılan malzemeler
    @SerializedName("preference") val preference: String, // Tercih
    @SerializedName("targetCalories") val targetCalories: Int, // Hedef kalori
    @SerializedName("source") val source: String = "mock", // Kaynak
    @SerializedName("selectedRecipes") val selectedRecipes: List<KitchenChefRecipeItem> // Seçilen tarifler
)

data class KitchenChefShareLogItem( // Paylaşılan tarif günlük kaydı
    @SerializedName("id") val id: String? = null, // Kayıt kimliği
    @SerializedName("createdAtUtc") val createdAtUtc: String = "", // Oluşturulma zamanı (UTC)
    @SerializedName("ingredients") val ingredients: String = "", // Malzemeler
    @SerializedName("preference") val preference: String = "", // Tercih
    @SerializedName("targetCalories") val targetCalories: Int = 0, // Hedef kalori
    @SerializedName("source") val source: String? = null, // Kaynak
    @SerializedName("selectedRecipes") val selectedRecipes: List<KitchenChefRecipeItem> = emptyList() // Seçilen tarifler
)

data class ClientPdfAnalysisResponseDto( // Danışan PDF analiz yanıtı
    @SerializedName("id") val id: String? = null, // Analiz kimliği
    @SerializedName("pdfUrl") val pdfUrl: String? = null, // PDF dosya URL'si
    @SerializedName("originalFileName") val originalFileName: String? = null, // Orijinal dosya adı
    @SerializedName("documentType") val documentType: String? = null, // Belge türü
    @SerializedName("summary") val summary: String? = null, // Özet
    @SerializedName("keyFindings") val keyFindings: List<String> = emptyList(), // Önemli bulgular
    @SerializedName("cautions") val cautions: List<String> = emptyList(), // Uyarılar
    @SerializedName("suggestedForDietitian") val suggestedForDietitian: List<String> = emptyList(), // Diyetisyene öneriler
    @SerializedName("analysisSource") val analysisSource: String? = null, // Analiz kaynağı
    @SerializedName("createdAtUtc") val createdAtUtc: String? = null // Analiz zamanı (UTC)
)

data class DietitianDailyTaskItemDto( // Diyetisyen günlük görev öğesi
    @SerializedName("id") val id: String? = null, // Görev kimliği
    @SerializedName("taskKey") val taskKey: String? = null, // Görev anahtarı
    @SerializedName("title") val title: String? = null, // Görev başlığı
    @SerializedName("subtitle") val subtitle: String? = null, // Alt başlık
    @SerializedName("category") val category: String? = null, // Kategori
    @SerializedName("clientId") val clientId: String? = null, // İlgili danışan kimliği
    @SerializedName("isCompleted") val isCompleted: Boolean = false, // Tamamlandı mı
    @SerializedName("dueLabel") val dueLabel: String? = null // Son tarih etiketi
)

data class DietitianTodayTasksBundleDto( // Bugünkü görevler paketi
    @SerializedName("taskDate") val taskDate: String? = null, // Görev tarihi
    @SerializedName("pendingCount") val pendingCount: Int = 0, // Bekleyen görev sayısı
    @SerializedName("completedCount") val completedCount: Int = 0, // Tamamlanan görev sayısı
    @SerializedName("totalCount") val totalCount: Int = 0, // Toplam görev sayısı
    @SerializedName("tasks") val tasks: List<DietitianDailyTaskItemDto> = emptyList() // Görev listesi
)

data class SetDietitianTaskCompleteBody( // Görev tamamlama isteği gövdesi
    @SerializedName("isCompleted") val isCompleted: Boolean // Tamamlandı bayrağı
)

data class DietitianCriticalAlertDto( // Kritik uyarı kaydı
    @SerializedName("id") val id: String = "", // Uyarı kimliği
    @SerializedName("clientId") val clientId: String = "", // Danışan kimliği
    @SerializedName("clientName") val clientName: String = "", // Danışan adı
    @SerializedName("alertType") val alertType: String = "", // Uyarı türü
    @SerializedName("severity") val severity: String = "", // Önem derecesi
    @SerializedName("message") val message: String = "", // Uyarı mesajı
    @SerializedName("date") val date: String = "", // Görüntüleme tarihi
    @SerializedName("referenceDate") val referenceDate: String = "" // Referans tarihi (onay için)
)

data class AckCriticalAlertRequest( // Kritik uyarı onaylama isteği
    @SerializedName("clientId") val clientId: String, // Danışan kimliği
    @SerializedName("alertType") val alertType: String, // Uyarı türü
    @SerializedName("referenceDate") val referenceDate: String // Referans tarihi
)

data class DietitianTabCountsDto( // Danışan sekmesi sayaçları
    @SerializedName("all") val all: Int = 0, // Tüm danışanlar
    @SerializedName("active") val active: Int = 0, // Aktif danışanlar
    @SerializedName("critical") val critical: Int = 0, // Kritik danışanlar
    @SerializedName("passive") val passive: Int = 0 // Pasif danışanlar
)

data class DietitianMyClientsResponseDto( // Diyetisyen danışan listesi yanıtı
    @SerializedName("tabCounts") val tabCounts: DietitianTabCountsDto = DietitianTabCountsDto(), // Sekme sayaçları
    @SerializedName("clients") val clients: List<DietitianClientCardDto> = emptyList() // Danışan kartları
)

data class DietitianClientCardDto( // Danışan liste kartı
    @SerializedName("id") val id: String? = null, // Danışan kimliği
    @SerializedName("firstName") val firstName: String? = null, // Ad
    @SerializedName("lastName") val lastName: String? = null, // Soyad
    @SerializedName("displayName") val displayName: String? = null, // Görünen ad
    @SerializedName("startedAtUtc") val startedAtUtc: String? = null, // Program başlangıcı (UTC)
    @SerializedName("lastActivityUtc") val lastActivityUtc: String? = null, // Son aktivite (UTC)
    @SerializedName("compliancePercent") val compliancePercent: Int = 0, // Uyum yüzdesi
    @SerializedName("segment") val segment: String? = null, // Segment (active/critical/passive)
    @SerializedName("isCritical") val isCritical: Boolean = false // Kritik durumda mı
)

data class DietitianClientBriefDto( // Danışan kısa profil özeti
    @SerializedName("clientId") val clientId: String? = null, // Danışan kimliği
    @SerializedName("firstName") val firstName: String? = null, // Ad
    @SerializedName("lastName") val lastName: String? = null, // Soyad
    @SerializedName("email") val email: String? = null, // E-posta
    @SerializedName("targetCalories") val targetCalories: Int = 0, // Kalori hedefi
    @SerializedName("weight") val weight: Double = 0.0, // Kilo (kg)
    @SerializedName("height") val height: Double = 0.0, // Boy (cm)
    @SerializedName("phone") val phone: String? = null // Telefon
)

data class DietitianProgramMealOverviewDto( // Program öğün özeti
    @SerializedName("mealKey") val mealKey: String? = null, // Öğün anahtarı
    @SerializedName("label") val label: String? = null, // Öğün etiketi
    @SerializedName("description") val description: String? = null, // Öğün açıklaması
    @SerializedName("calories") val calories: Int = 0, // Kalori
    @SerializedName("completed") val completed: Boolean = false // Tamamlandı mı
)

data class DietitianProgramDayOverviewDto( // Günlük program özeti
    @SerializedName("programDate") val programDate: String? = null, // Program tarihi
    @SerializedName("weekdayLabel") val weekdayLabel: String? = null, // Haftanın günü etiketi
    @SerializedName("meals") val meals: List<DietitianProgramMealOverviewDto> = emptyList() // Öğün listesi
)

data class DietitianClientOverviewDto( // Danışan detay genel bakış
    @SerializedName("client") val client: DietitianClientBriefDto? = null, // Danışan profili
    @SerializedName("compliancePercent") val compliancePercent: Int = 0, // Uyum yüzdesi
    @SerializedName("complianceReferenceDate") val complianceReferenceDate: String? = null, // Uyum referans tarihi
    @SerializedName("weeklyProgramDays") val weeklyProgramDays: List<DietitianProgramDayOverviewDto> = emptyList(), // Haftalık program
    @SerializedName("kitchenRecipeLogs") val kitchenRecipeLogs: List<KitchenChefShareLogItem> = emptyList(), // Mutfak şefi kayıtları
    @SerializedName("pdfAnalyses") val pdfAnalyses: List<ClientPdfAnalysisResponseDto> = emptyList() // PDF analizleri
)
