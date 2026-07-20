package com.example.nightbrate

/** Groq gerçek AI sağlayıcısı mı. */
fun isRealAiSource(source: String?): Boolean = source?.lowercase() == "groq"

/** Groq'a ağ/DNS hatası nedeniyle ulaşılamadığında dönen yedek kaynak. */
fun isMockNetworkSource(source: String?): Boolean = source?.lowercase() == "mock_network"
