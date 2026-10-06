package br.ufrn.noscroll.adapters.persistence

import br.ufrn.noscroll.domain.AlertMode
import br.ufrn.noscroll.domain.NewSetup
import br.ufrn.noscroll.domain.Page
import br.ufrn.noscroll.domain.PageRequest
import br.ufrn.noscroll.domain.Setup
import br.ufrn.noscroll.domain.SetupRepository

class InMemorySetupRepository : SetupRepository {
    private val setups = mutableListOf(
        Setup(1, "com.instagram.android", "Instagram", 60),
        Setup(2, "com.zhiliaoapp.musically", "TikTok", 30, AlertMode.PERIODIC, 15),
    )
    private var nextId = 3

    override suspend fun list(app: String?, pageRequest: PageRequest): Page<Setup> {
        val filtered = if (app == null) setups else setups.filter { it.appName.contains(app, ignoreCase = true) }
        val items = filtered.drop(pageRequest.offset.toInt()).take(pageRequest.size)
        return Page(items, pageRequest.page, pageRequest.size, filtered.size.toLong())
    }

    override suspend fun find(id: Int): Setup? = setups.find { it.id == id }
    override suspend fun add(new: NewSetup): Setup =
        Setup(nextId++, new.appPackage, new.appName, new.dailyLimitMinutes, new.alertMode, new.alertIntervalMinutes)
            .also { setups.add(it) }

    override suspend fun update(id: Int, new: NewSetup): Setup? {
        val index = setups.indexOfFirst { it.id == id }
        if (index == -1) return null
        val updated = Setup(id, new.appPackage, new.appName, new.dailyLimitMinutes, new.alertMode, new.alertIntervalMinutes)
        setups[index] = updated
        return updated
    }

    override suspend fun remove(id: Int): Boolean = setups.removeIf { it.id == id }
}
