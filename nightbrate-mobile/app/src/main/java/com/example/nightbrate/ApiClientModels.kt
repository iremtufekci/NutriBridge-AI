package com.example.nightbrate

import com.google.gson.annotations.SerializedName

data class ClientProfileResponse(
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("weight") val weight: Double = 0.0,
    @SerializedName("height") val height: Double = 0.0,
    @SerializedName("targetCalories") val targetCalories: Int = 2000,
    @SerializedName("goalText") val goalText: String? = null,
    @SerializedName("themePreference") val themePreference: String? = "light",
    @SerializedName("dietitianName") val dietitianName: String? = null,
    @SerializedName("programStartDate") val programStartDate: String? = null
)

data class UpdateClientProfileRequest(
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("weight") val weight: Double,
    @SerializedName("height") val height: Double,
    @SerializedName("targetCalories") val targetCalories: Int
)

data class UpdateThemeRequest(
    @SerializedName("themePreference") val themePreference: String
)

data class ConnectToDietitianRequest(
    @SerializedName("connectionCode") val connectionCode: String
)

data class PreviewDietitianByCodeResult(
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("displayName") val displayName: String? = null
)

data class ConnectToDietitianResult(
    @SerializedName("message") val message: String? = null,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null
)

data class DashboardStatsResponse(
    @SerializedName("totalUsers") val totalUsers: Long = 0,
    @SerializedName("activeUsers") val activeUsers: Long = 0,
    @SerializedName("totalClients") val totalClients: Long = 0,
    @SerializedName("totalDietitians") val totalDietitians: Long = 0,
    @SerializedName("activeDietitians") val activeDietitians: Long = 0,
    @SerializedName("pendingDietitians") val pendingDietitians: Long = 0,
    @SerializedName("roleDistribution") val roleDistribution: List<RoleCountItem> = emptyList(),
    @SerializedName("monthlyRegistrations") val monthlyRegistrations: List<MonthlyRegistrationItem> = emptyList()
)

data class RoleCountItem(
    @SerializedName("role") val role: String? = null,
    @SerializedName("count") val count: Long = 0
)

data class MonthlyRegistrationItem(
    @SerializedName("year") val year: Int = 0,
    @SerializedName("month") val month: Int = 0,
    @SerializedName("count") val count: Long = 0
)

data class ActivityItemDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("initial") val initial: String? = null,
    @SerializedName("actorDisplayName") val actorDisplayName: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class PendingDietitianItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("diplomaNo") val diplomaNo: String? = null,
    @SerializedName("clinicName") val clinicName: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("isApproved") val isApproved: Boolean? = null
)

data class CurrentUserProfileResponse(
    @SerializedName("email") val email: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("clinicName") val clinicName: String? = null,
    @SerializedName("diplomaNo") val diplomaNo: String? = null,
    @SerializedName("themePreference") val themePreference: String? = "light",
    @SerializedName(
        value = "connectionCode",
        alternate = ["ConnectionCode"]
    ) val connectionCode: String? = null
)

data class ClientWithLastMealItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null
)

data class DietProgramViewResponse(
    @SerializedName("clientId") val clientId: String? = null,
    @SerializedName("programDate") val programDate: String? = null,
    @SerializedName("breakfast") val breakfast: String? = null,
    @SerializedName("lunch") val lunch: String? = null,
    @SerializedName("dinner") val dinner: String? = null,
    @SerializedName("snack") val snack: String? = null,
    @SerializedName("breakfastCalories") val breakfastCalories: Int? = 0,
    @SerializedName("lunchCalories") val lunchCalories: Int? = 0,
    @SerializedName("dinnerCalories") val dinnerCalories: Int? = 0,
    @SerializedName("snackCalories") val snackCalories: Int? = 0,
    @SerializedName("totalCalories") val totalCalories: Int? = 0,
    @SerializedName("hasSavedProgram") val hasSavedProgram: Boolean? = null
)

data class ClientDietProgramDayResponse(
    @SerializedName("programDate") val programDate: String? = null,
    @SerializedName("breakfast") val breakfast: String? = null,
    @SerializedName("lunch") val lunch: String? = null,
    @SerializedName("dinner") val dinner: String? = null,
    @SerializedName("snack") val snack: String? = null,
    @SerializedName("breakfastCalories") val breakfastCalories: Int? = 0,
    @SerializedName("lunchCalories") val lunchCalories: Int? = 0,
    @SerializedName("dinnerCalories") val dinnerCalories: Int? = 0,
    @SerializedName("snackCalories") val snackCalories: Int? = 0,
    @SerializedName("totalCalories") val totalCalories: Int? = 0,
    @SerializedName("hasProgram") val hasProgram: Boolean? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("dietitianName") val dietitianName: String? = null,
    @SerializedName("breakfastCompleted") val breakfastCompleted: Boolean? = false,
    @SerializedName("lunchCompleted") val lunchCompleted: Boolean? = false,
    @SerializedName("dinnerCompleted") val dinnerCompleted: Boolean? = false,
    @SerializedName("snackCompleted") val snackCompleted: Boolean? = false
)

data class SetMealCompletedRequest(
    @SerializedName("programDate") val programDate: String,
    @SerializedName("meal") val meal: String
)

data class SaveDietProgramRequest(
    @SerializedName("clientId") val clientId: String,
    @SerializedName("programDate") val programDate: String,
    @SerializedName("breakfast") val breakfast: String,
    @SerializedName("lunch") val lunch: String,
    @SerializedName("dinner") val dinner: String,
    @SerializedName("snack") val snack: String,
    @SerializedName("breakfastCalories") val breakfastCalories: Int,
    @SerializedName("lunchCalories") val lunchCalories: Int,
    @SerializedName("dinnerCalories") val dinnerCalories: Int,
    @SerializedName("snackCalories") val snackCalories: Int,
    @SerializedName("totalCalories") val totalCalories: Int
)

data class UserManagementStatsResponse(
    @SerializedName("totalUsers") val totalUsers: Int = 0,
    @SerializedName("admins") val admins: Int = 0,
    @SerializedName("dietitians") val dietitians: Int = 0,
    @SerializedName("clients") val clients: Int = 0,
    @SerializedName("active") val active: Int = 0,
    @SerializedName("pending") val pending: Int = 0
)

data class AdminUserRowItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("initial") val initial: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("roleKey") val roleKey: String? = null,
    @SerializedName("statusKey") val statusKey: String? = null,
    @SerializedName("statusLabel") val statusLabel: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("lastActivityAt") val lastActivityAt: String? = null,
    @SerializedName("isSuspended") val isSuspended: Boolean? = null
)

data class SetUserSuspensionRequest(
    @SerializedName("message") val message: String
)
