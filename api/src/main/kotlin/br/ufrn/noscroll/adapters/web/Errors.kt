package br.ufrn.noscroll.adapters.web

import br.ufrn.noscroll.domain.InvalidInput
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Problem(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String? = null,
    val violations: List<String>? = null,
)

private val json = Json { explicitNulls = false }

suspend fun ApplicationCall.respondProblem(
    status: HttpStatusCode,
    type: String,
    title: String,
    detail: String? = null,
    violations: List<String>? = null,
) {
    val problem = Problem("/problems/$type", title, status.value, detail, violations)
    respondText(json.encodeToString(problem), ContentType.Application.ProblemJson, status)
}

fun Application.handleErrors() {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respondProblem(
                HttpStatusCode.BadRequest, "malformed-request", "Requisição malformada",
                cause.cause?.message ?: cause.message,
            )
        }
        exception<NotFoundException> { call, cause ->
            call.respondProblem(HttpStatusCode.NotFound, "not-found", "Recurso inexistente", cause.message)
        }
        exception<InvalidInput> { call, cause ->
            call.respondProblem(
                HttpStatusCode.UnprocessableEntity, "invalid-input", "Entrada inválida",
                "A entrada viola ${cause.violations.size} regra(s).", cause.violations,
            )
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("erro não tratado", cause)
            call.respondProblem(HttpStatusCode.InternalServerError, "internal", "Erro interno")
        }
    }
}
