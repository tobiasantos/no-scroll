package br.ufrn.noscroll

import br.ufrn.noscroll.adapters.persistence.DbConfig
import br.ufrn.noscroll.domain.AlertMode
import br.ufrn.noscroll.domain.NewSetup
import br.ufrn.noscroll.domain.Setup
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
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
import kotlin.test.assertTrue

class SetupsTest {

    companion object {
        private val postgres = PostgreSQLContainer("postgres:17-alpine").apply { start() }
    }

    private fun ApplicationTestBuilder.appWithDb() = application {
        module(DbConfig(postgres.jdbcUrl, postgres.username, postgres.password))
    }

    @Test
    fun `lista os setups criados pela migracao`() = testApplication {
        appWithDb()
        val client = createClient { install(ContentNegotiation) { json() } }
        val setups = client.get("/setups").body<List<Setup>>()
        assertTrue(setups.any { it.appName == "Instagram" })
    }

    @Test
    fun `cria e depois busca pelo Location`() = testApplication {
        appWithDb()
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.post("/setups") {
            contentType(ContentType.Application.Json)
            setBody(NewSetup("com.google.android.youtube", "YouTube", 45))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val created = response.body<Setup>()
        val location = response.headers[HttpHeaders.Location]!!
        assertEquals("/setups/${created.id}", location)
        assertEquals(created, client.get(location).body<Setup>())
    }

    @Test
    fun `substitui um setup com PUT`() = testApplication {
        appWithDb()
        val client = createClient { install(ContentNegotiation) { json() } }
        val created = client.post("/setups") {
            contentType(ContentType.Application.Json)
            setBody(NewSetup("com.reddit.frontpage", "Reddit", 30))
        }.body<Setup>()
        val changes = NewSetup("com.reddit.frontpage", "Reddit", 10, AlertMode.PERIODIC, 5)
        val response = client.put("/setups/${created.id}") {
            contentType(ContentType.Application.Json)
            setBody(changes)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(10, client.get("/setups/${created.id}").body<Setup>().dailyLimitMinutes)
    }

    @Test
    fun `PUT em id inexistente devolve 404`() = testApplication {
        appWithDb()
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.put("/setups/9999") {
            contentType(ContentType.Application.Json)
            setBody(NewSetup("com.reddit.frontpage", "Reddit", 30))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `remove com 204 e repetir devolve 404`() = testApplication {
        appWithDb()
        val client = createClient { install(ContentNegotiation) { json() } }
        val created = client.post("/setups") {
            contentType(ContentType.Application.Json)
            setBody(NewSetup("com.pinterest", "Pinterest", 20))
        }.body<Setup>()
        assertEquals(HttpStatusCode.NoContent, client.delete("/setups/${created.id}").status)
        assertEquals(HttpStatusCode.NotFound, client.delete("/setups/${created.id}").status)
    }

    @Test
    fun `id inexistente devolve 404`() = testApplication {
        appWithDb()
        assertEquals(HttpStatusCode.NotFound, client.get("/setups/9999").status)
    }

    @Test
    fun `openapi descreve as rotas`() = testApplication {
        appWithDb()
        val spec = client.get("/docs/documentation.yaml").bodyAsText()
        assertTrue("/setups/{id}:" in spec)
    }
}
