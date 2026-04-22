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
