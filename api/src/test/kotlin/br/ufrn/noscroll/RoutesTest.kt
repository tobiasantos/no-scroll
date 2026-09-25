package br.ufrn.noscroll

import br.ufrn.noscroll.adapters.persistence.InMemoryRepository
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutesTest {

    @Test
    fun `id que nao e numero devolve 400`() = testApplication {
        application { configure(InMemoryRepository()) }
        assertEquals(HttpStatusCode.BadRequest, client.get("/setups/abc").status)
    }

    @Test
    fun `id existente devolve 200`() = testApplication {
        application { configure(InMemoryRepository()) }
        assertEquals(HttpStatusCode.OK, client.get("/setups/1").status)
    }
}
