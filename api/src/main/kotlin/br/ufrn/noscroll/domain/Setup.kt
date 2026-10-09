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
) {
    fun violations(): List<String> = buildList {
        if (appPackage.isBlank()) add("appPackage: não pode ficar em branco")
        if (appPackage.length > 200) add("appPackage: no máximo 200 caracteres")
        if (appName.isBlank()) add("appName: não pode ficar em branco")
        if (appName.length > 100) add("appName: no máximo 100 caracteres")
        if (dailyLimitMinutes !in 1..1440) add("dailyLimitMinutes: entre 1 e 1440")
        if (alertMode == AlertMode.PERIODIC && alertIntervalMinutes == null) {
            add("alertIntervalMinutes: obrigatório quando alertMode é PERIODIC")
        }
        if (alertIntervalMinutes != null && alertIntervalMinutes < 1) add("alertIntervalMinutes: no mínimo 1")
    }
}

interface SetupRepository {
    suspend fun list(app: String?, pageRequest: PageRequest): Page<Setup>
    suspend fun find(id: Int): Setup?
    suspend fun add(new: NewSetup): Setup
    suspend fun update(id: Int, new: NewSetup): Setup?
    suspend fun remove(id: Int): Boolean
}
