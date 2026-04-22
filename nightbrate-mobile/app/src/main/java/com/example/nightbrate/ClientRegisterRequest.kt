package com.example.nightbrate

data class ClientRegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val weight: Double,
    val height: Double,
    val targetCalories: Int,
    val dietitianId: String? = null
)
