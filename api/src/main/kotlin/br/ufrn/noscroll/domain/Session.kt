package br.ufrn.noscroll.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate
import kotlin.time.Instant

@Serializable
data class Session(
    val id: Int,
    val setupId: Int,
    val startedAt: Instant,
    val durationMinutes: Int,
)

@Serializable
data class NewSession(
    val startedAt: Instant,
    val durationMinutes: Int,
) {
    fun violations(): List<String> = buildList {
        if (durationMinutes !in 1..1440) add("durationMinutes: entre 1 e 1440")
    }
}

interface SessionRepository {
    suspend fun list(setupId: Int, date: LocalDate?): List<Session>
    suspend fun find(setupId: Int, id: Int): Session?
    suspend fun add(setupId: Int, new: NewSession): Session
    suspend fun update(setupId: Int, id: Int, new: NewSession): Session?
    suspend fun remove(setupId: Int, id: Int): Boolean
}
