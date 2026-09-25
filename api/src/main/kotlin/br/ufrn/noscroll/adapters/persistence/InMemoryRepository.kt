package br.ufrn.noscroll.adapters.persistence

import br.ufrn.noscroll.domain.AlertMode
import br.ufrn.noscroll.domain.NewSetup
import br.ufrn.noscroll.domain.Setup
import br.ufrn.noscroll.domain.SetupRepository

class InMemoryRepository : SetupRepository {
    private val setups = mutableListOf(
        Setup(1, "com.instagram.android", "Instagram", 60),
        Setup(2, "com.zhiliaoapp.musically", "TikTok", 30, AlertMode.PERIODIC, 15),
    )
    private var nextId = 3

    override suspend fun list(): List<Setup> = setups.toList()
    override suspend fun find(id: Int): Setup? = setups.find { it.id == id }
    override suspend fun add(new: NewSetup): Setup =
        Setup(nextId++, new.appPackage, new.appName, new.dailyLimitMinutes, new.alertMode, new.alertIntervalMinutes)
            .also { setups.add(it) }
}
