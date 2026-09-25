package br.ufrn.noscroll

import br.ufrn.noscroll.adapters.persistence.DbConfig
import br.ufrn.noscroll.domain.NewSetup
import br.ufrn.noscroll.domain.Setup
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
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
    fun `cria e depois busca pelo id`() = testApplication {
        appWithDb()
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.post("/setups") {
            contentType(ContentType.Application.Json)
            setBody(NewSetup("com.google.android.youtube", "YouTube", 45))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val created = response.body<Setup>()
        assertEquals(created, client.get("/setups/${created.id}").body<Setup>())
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
