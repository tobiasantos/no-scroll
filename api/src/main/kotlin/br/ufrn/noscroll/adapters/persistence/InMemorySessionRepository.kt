package br.ufrn.noscroll.adapters.persistence

import br.ufrn.noscroll.domain.NewSession
import br.ufrn.noscroll.domain.Session
import br.ufrn.noscroll.domain.SessionRepository
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.time.toJavaInstant

class InMemorySessionRepository : SessionRepository {
    private val sessions = mutableListOf<Session>()
    private var nextId = 1

    override suspend fun list(setupId: Int, date: LocalDate?): List<Session> = sessions.filter {
        it.setupId == setupId && (date == null || it.startedAt.toJavaInstant().atOffset(ZoneOffset.UTC).toLocalDate() == date)
    }

    override suspend fun find(setupId: Int, id: Int): Session? = sessions.find { it.setupId == setupId && it.id == id }
    override suspend fun add(setupId: Int, new: NewSession): Session =
        Session(nextId++, setupId, new.startedAt, new.durationMinutes).also { sessions.add(it) }

    override suspend fun update(setupId: Int, id: Int, new: NewSession): Session? {
        val index = sessions.indexOfFirst { it.setupId == setupId && it.id == id }
        if (index == -1) return null
        val updated = Session(id, setupId, new.startedAt, new.durationMinutes)
        sessions[index] = updated
        return updated
    }

    override suspend fun remove(setupId: Int, id: Int): Boolean =
        sessions.removeIf { it.setupId == setupId && it.id == id }
}
