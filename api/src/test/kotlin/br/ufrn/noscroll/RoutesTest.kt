package br.ufrn.noscroll

import br.ufrn.noscroll.adapters.persistence.InMemorySessionRepository
import br.ufrn.noscroll.adapters.persistence.InMemorySetupRepository
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutesTest {

    @Test
    fun `id que nao e numero devolve 400`() = testApplication {
        application { configure(InMemorySetupRepository(), InMemorySessionRepository()) }
        assertEquals(HttpStatusCode.BadRequest, client.get("/setups/abc").status)
    }

    @Test
    fun `id existente devolve 200`() = testApplication {
        application { configure(InMemorySetupRepository(), InMemorySessionRepository()) }
        assertEquals(HttpStatusCode.OK, client.get("/setups/1").status)
    }

    @Test
    fun `size acima do maximo devolve 400`() = testApplication {
        application { configure(InMemorySetupRepository(), InMemorySessionRepository()) }
        assertEquals(HttpStatusCode.BadRequest, client.get("/setups?size=1000").status)
    }

    @Test
    fun `page que nao e numero devolve 400`() = testApplication {
        application { configure(InMemorySetupRepository(), InMemorySessionRepository()) }
        assertEquals(HttpStatusCode.BadRequest, client.get("/setups?page=abc").status)
    }

    @Test
    fun `date fora do formato devolve 400`() = testApplication {
        application { configure(InMemorySetupRepository(), InMemorySessionRepository()) }
        assertEquals(HttpStatusCode.BadRequest, client.get("/setups/1/sessions?date=ontem").status)
    }

    @Test
    fun `setup com nome em branco devolve 422 em problem details`() = testApplication {
        application { configure(InMemorySetupRepository(), InMemorySessionRepository()) }
        val response = client.post("/setups") {
            contentType(ContentType.Application.Json)
            setBody("""{"appPackage":"com.x","appName":"  ","dailyLimitMinutes":30}""")
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertEquals(ContentType.Application.ProblemJson, response.contentType()?.withoutParameters())
        assertTrue("appName: não pode ficar em branco" in response.bodyAsText())
    }

    @Test
    fun `corpo sem campos obrigatorios devolve 400 em problem details`() = testApplication {
        application { configure(InMemorySetupRepository(), InMemorySessionRepository()) }
        val response = client.post("/setups") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(ContentType.Application.ProblemJson, response.contentType()?.withoutParameters())
    }

    @Test
    fun `sessao com duracao zero devolve 422`() = testApplication {
        application { configure(InMemorySetupRepository(), InMemorySessionRepository()) }
        val response = client.post("/setups/1/sessions") {
            contentType(ContentType.Application.Json)
            setBody("""{"startedAt":"2026-09-28T10:00:00Z","durationMinutes":0}""")
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
    }

    @Test
    fun `setup inexistente devolve 404 em problem details`() = testApplication {
        application { configure(InMemorySetupRepository(), InMemorySessionRepository()) }
        val response = client.get("/setups/9999")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(ContentType.Application.ProblemJson, response.contentType()?.withoutParameters())
    }
}
