package com.example.nightbrate

/** /api/Admin/system-analytics yanıtı (Gson, ek alanlar yok sayılır). */
data class SystemAnalyticsResponse(
    val kpis: SystemKpiBlock,
    val endpointPerformance: List<EndpointPerformanceRow>,
    val databaseHourly: List<HourlyDbRow>?,
    val cacheHourly: List<HourlyCacheRow>?,
    val networkHourly: List<HourlyNetRow>?,
    val systemResources: SystemResourcesBlock?,
    val errorLogs: List<ErrorLogEntry>?,
    val securityEvents: List<SecurityEventEntry>?,
    val dataWindowHours: Int?,
    val generatedAtUtc: String?,
    val dataNote: String?
)

data class SystemKpiBlock(
    val apiRequestsPerHour: Int,
    val apiRequestsPerHourDeltaPercent: Double,
    val avgQueryTimeMs: Int,
    val avgQueryTimeDeltaPercent: Double,
    val securityScore: Double,
    val securityOpenIssues: Int,
    val cacheHitRatioPercent: Double,
    val cacheStatusLabel: String?
)

data class EndpointPerformanceRow(
    val endpoint: String,
    val calls: Int,
    val avgTimeMs: Int,
    val errors: Int,
    val successRatePercent: Double
)

data class HourlyDbRow(val hour: Int, val label: String, val reads: Int, val writes: Int, val slowQueries: Int)
data class HourlyCacheRow(val hour: Int, val label: String, val hits: Int, val misses: Int)
data class HourlyNetRow(val hour: Int, val label: String, val incomingMbps: Double, val outgoingMbps: Double)

data class SystemResourcesBlock(
    val cpuPercent: Double,
    val memoryPercent: Double,
    val memoryRefLabel: String?,
    val diskIoPercent: Double,
    val diskRefLabel: String?,
    val networkMbps: Double,
    val networkUp: Double,
    val networkDown: Double,
    val networkNote: String?
)

data class ErrorLogEntry(
    val statusCode: Int,
    val time: String,
    val endpoint: String,
    val message: String,
    val count: Int
)

data class SecurityEventEntry(
    val severity: String,
    val time: String,
    val name: String,
    val obfuscatedSource: String,
    val countLabel: String,
    val tone: String
)
