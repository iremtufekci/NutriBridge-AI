package com.example.nightbrate

data class DietitianRegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val diplomaNo: String,
    val clinicName: String
)
