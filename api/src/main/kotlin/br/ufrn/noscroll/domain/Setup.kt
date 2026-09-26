package br.ufrn.noscroll.domain

import kotlinx.serialization.Serializable

enum class AlertMode { ONCE, PERIODIC }

@Serializable
data class Setup(
    val id: Int,
    val appPackage: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val alertMode: AlertMode = AlertMode.ONCE,
    val alertIntervalMinutes: Int? = null,
)

@Serializable
data class NewSetup(
    val appPackage: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val alertMode: AlertMode = AlertMode.ONCE,
    val alertIntervalMinutes: Int? = null,
)

interface SetupRepository {
    suspend fun list(): List<Setup>
    suspend fun find(id: Int): Setup?
    suspend fun add(new: NewSetup): Setup
    suspend fun update(id: Int, new: NewSetup): Setup?
    suspend fun remove(id: Int): Boolean
}
