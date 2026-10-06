package br.ufrn.noscroll

import br.ufrn.noscroll.adapters.persistence.InMemorySessionRepository
import br.ufrn.noscroll.adapters.persistence.InMemorySetupRepository
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
