package com.example.nightbrate

import com.google.gson.annotations.SerializedName

data class ClientProfileResponse(
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("weight") val weight: Double = 0.0,
    @SerializedName("height") val height: Double = 0.0,
    @SerializedName("targetCalories") val targetCalories: Int? = null,
    @SerializedName("goalText") val goalText: String? = null,
    @SerializedName("themePreference") val themePreference: String? = "light",
    @SerializedName("dietitianName") val dietitianName: String? = null,
    @SerializedName("programStartDate") val programStartDate: String? = null
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
    @SerializedName(
        value = "connectionCode",
        alternate = ["ConnectionCode"]
    ) val connectionCode: String? = null
)
