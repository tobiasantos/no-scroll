package br.ufrn.noscroll

import br.ufrn.noscroll.adapters.persistence.DbConfig
import br.ufrn.noscroll.adapters.persistence.PostgresRepository
import br.ufrn.noscroll.adapters.persistence.createDataSource
import br.ufrn.noscroll.adapters.persistence.migrate
import br.ufrn.noscroll.adapters.web.routes
import br.ufrn.noscroll.domain.SetupRepository
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0") { module() }
        .start(wait = true)
}

fun Application.module(config: DbConfig = DbConfig.fromEnv()) {
    val dataSource = createDataSource(config)
    migrate(dataSource)
    monitor.subscribe(ApplicationStopped) { dataSource.close() }
    configure(PostgresRepository(Database.connect(dataSource)))
}

fun Application.configure(repository: SetupRepository) {
    install(Koin) {
        modules(module { single<SetupRepository> { repository } })
    }
    install(ContentNegotiation) { json() }
    routes()
}
