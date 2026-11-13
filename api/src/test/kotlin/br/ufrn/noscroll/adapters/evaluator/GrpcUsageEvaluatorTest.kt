package br.ufrn.noscroll.adapters.evaluator

import br.ufrn.noscroll.domain.Session
import br.ufrn.noscroll.domain.Setup
import br.ufrn.noscroll.evaluator.v1.EvaluateUsageRequest
import br.ufrn.noscroll.evaluator.v1.EvaluateUsageResponse
import br.ufrn.noscroll.evaluator.v1.EvaluatorServiceGrpcKt.EvaluatorServiceCoroutineImplBase
import br.ufrn.noscroll.evaluator.v1.evaluateUsageResponse
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.time.measureTime

class GrpcUsageEvaluatorTest {

    private val servers = mutableListOf<Server>()
    private val setup = Setup(1, "com.zhiliaoapp.musically", "TikTok", 30)
    private val sessions = listOf(
        Session(1, 1, Instant.parse("2026-11-13T10:00:00Z"), 25),
        Session(2, 1, Instant.parse("2026-11-13T15:00:00Z"), 10),
    )

    private fun evaluatorFor(name: String, service: EvaluatorServiceCoroutineImplBase): GrpcUsageEvaluator {
        servers += InProcessServerBuilder.forName(name).directExecutor().addService(service).build().start()
        return GrpcUsageEvaluator(InProcessChannelBuilder.forName(name).directExecutor().build(), deadline = 300.milliseconds)
    }

    @AfterTest
    fun stop() = servers.forEach { it.shutdownNow() }

    @Test
    fun `avaliacao respondida devolve true e envia o uso do dia`() = runBlocking {
        var received: EvaluateUsageRequest? = null
        val evaluator = evaluatorFor("fast", object : EvaluatorServiceCoroutineImplBase() {
            override suspend fun evaluateUsage(request: EvaluateUsageRequest): EvaluateUsageResponse {
                received = request
                return evaluateUsageResponse { usedMinutes = 35; exceeded = true }
            }
        })
        assertTrue(evaluator.evaluate(setup, sessions))
        assertEquals("TikTok", received?.appName)
        assertEquals(30, received?.limitMinutes)
        assertEquals(listOf(25, 10), received?.sessionMinutesList)
    }

    @Test
    fun `servico lento estoura o prazo e devolve false sem esperar`() = runBlocking {
        val evaluator = evaluatorFor("slow", object : EvaluatorServiceCoroutineImplBase() {
            override suspend fun evaluateUsage(request: EvaluateUsageRequest): EvaluateUsageResponse {
                delay(5_000)
                return evaluateUsageResponse { }
            }
        })
        var result = true
        val elapsed = measureTime { result = evaluator.evaluate(setup, sessions) }
        assertFalse(result)
        assertTrue(elapsed < 2_000.milliseconds, "esperou $elapsed: o prazo de 300 ms não foi aplicado")
    }

    @Test
    fun `servico fora do ar devolve false`() = runBlocking {
        val evaluator = GrpcUsageEvaluator(InProcessChannelBuilder.forName("nobody").build(), deadline = 300.milliseconds)
        assertFalse(evaluator.evaluate(setup, sessions))
    }
}
