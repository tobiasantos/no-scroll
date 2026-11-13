package br.ufrn.noscroll.adapters.evaluator

import br.ufrn.noscroll.domain.Session
import br.ufrn.noscroll.domain.Setup
import br.ufrn.noscroll.domain.UsageEvaluator
import br.ufrn.noscroll.evaluator.v1.EvaluatorServiceGrpcKt.EvaluatorServiceCoroutineStub
import br.ufrn.noscroll.evaluator.v1.evaluateUsageRequest
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusException
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class GrpcUsageEvaluator(
    private val channel: ManagedChannel,
    private val deadline: Duration = 1.seconds,
) : UsageEvaluator {

    private val stub = EvaluatorServiceCoroutineStub(channel)
    private val log = LoggerFactory.getLogger(GrpcUsageEvaluator::class.java)

    override suspend fun evaluate(setup: Setup, todaySessions: List<Session>): Boolean = try {
        val response = stub
            .withDeadlineAfter(deadline.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .evaluateUsage(evaluateUsageRequest {
                appName = setup.appName
                limitMinutes = setup.dailyLimitMinutes
                sessionMinutes += todaySessions.map { it.durationMinutes }
            })
        log.info(
            "setup {}: used {} of {}, exceeded {}, alerts {}",
            setup.id, response.usedMinutes, setup.dailyLimitMinutes, response.exceeded,
            response.deliveriesList.map { "${it.channel}=${it.delivered}" },
        )
        true
    } catch (e: StatusException) {
        log.warn("setup {} not evaluated: {}", setup.id, e.status.code)
        false
    }

    fun close() {
        channel.shutdown().awaitTermination(1, TimeUnit.SECONDS)
    }

    companion object {
        fun to(address: String) = GrpcUsageEvaluator(ManagedChannelBuilder.forTarget(address).usePlaintext().build())
    }
}
