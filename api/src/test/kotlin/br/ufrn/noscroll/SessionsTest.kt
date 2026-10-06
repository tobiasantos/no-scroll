package br.ufrn.noscroll

import br.ufrn.noscroll.adapters.persistence.DbConfig
import br.ufrn.noscroll.domain.NewSession
import br.ufrn.noscroll.domain.NewSetup
import br.ufrn.noscroll.domain.Session
import br.ufrn.noscroll.domain.Setup
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.testcontainers.postgresql.PostgreSQLContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SessionsTest {

    companion object {
        private val postgres = PostgreSQLContainer("postgres:17-alpine").apply { start() }
    }

    private fun ApplicationTestBuilder.appWithDb() = application {
        module(DbConfig(postgres.jdbcUrl, postgres.username, postgres.password))
    }

    private suspend fun HttpClient.createSetup(): Setup = post("/setups") {
        contentType(ContentType.Application.Json)
        setBody(NewSetup("com.instagram.android", "Instagram", 60))
    }.body()

    private val session = NewSession(Instant.parse("2026-09-28T10:00:00Z"), 25)

    @Test
    fun `registra uma sessao e busca pelo Location`() = testApplication {
        appWithDb()
        val client = createClient { install(ContentNegotiation) { json() } }
        val setup = client.createSetup()
        val response = client.post("/setups/${setup.id}/sessions") {
            contentType(ContentType.Application.Json)
            setBody(session)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val created = response.body<Session>()
        val location = response.headers[HttpHeaders.Location]!!
        assertEquals("/setups/${setup.id}/sessions/${created.id}", location)
        assertEquals(created, client.get(location).body<Session>())
        assertEquals(listOf(created), client.get("/setups/${setup.id}/sessions").body<List<Session>>())
    }

    @Test
    fun `sessao em setup inexistente devolve 404`() = testApplication {
        appWithDb()
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.post("/setups/9999/sessions") {
            contentType(ContentType.Application.Json)
            setBody(session)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `sessao de outro setup devolve 404`() = testApplication {
        appWithDb()
        val client = createClient { install(ContentNegotiation) { json() } }
        val setup = client.createSetup()
        val other = client.createSetup()
        val created = client.post("/setups/${setup.id}/sessions") {
            contentType(ContentType.Application.Json)
            setBody(session)
        }.body<Session>()
        assertEquals(HttpStatusCode.NotFound, client.get("/setups/${other.id}/sessions/${created.id}").status)
    }

    @Test
    fun `substitui e remove uma sessao`() = testApplication {
        appWithDb()
        val client = createClient { install(ContentNegotiation) { json() } }
        val setup = client.createSetup()
        val created = client.post("/setups/${setup.id}/sessions") {
            contentType(ContentType.Application.Json)
            setBody(session)
        }.body<Session>()
        val response = client.put("/setups/${setup.id}/sessions/${created.id}") {
            contentType(ContentType.Application.Json)
            setBody(NewSession(created.startedAt, 40))
        }
        assertEquals(40, response.body<Session>().durationMinutes)
        assertEquals(HttpStatusCode.NoContent, client.delete("/setups/${setup.id}/sessions/${created.id}").status)
        assertEquals(HttpStatusCode.NotFound, client.delete("/setups/${setup.id}/sessions/${created.id}").status)
    }

    @Test
    fun `remover o setup remove as sessoes`() = testApplication {
        appWithDb()
        val client = createClient { install(ContentNegotiation) { json() } }
        val setup = client.createSetup()
        val created = client.post("/setups/${setup.id}/sessions") {
            contentType(ContentType.Application.Json)
            setBody(session)
        }.body<Session>()
        client.delete("/setups/${setup.id}")
        assertEquals(HttpStatusCode.NotFound, client.get("/setups/${setup.id}/sessions/${created.id}").status)
    }

    @Test
    fun `filtra as sessoes pelo dia`() = testApplication {
        appWithDb()
        val client = createClient { install(ContentNegotiation) { json() } }
        val setup = client.createSetup()
        val today = client.post("/setups/${setup.id}/sessions") {
            contentType(ContentType.Application.Json)
            setBody(session)
        }.body<Session>()
        client.post("/setups/${setup.id}/sessions") {
            contentType(ContentType.Application.Json)
            setBody(NewSession(Instant.parse("2026-09-27T23:30:00Z"), 15))
        }
        val sessions = client.get("/setups/${setup.id}/sessions?date=2026-09-28").body<List<Session>>()
        assertEquals(listOf(today), sessions)
    }
}
