package br.ufrn.noscroll.adapters.web

import br.ufrn.noscroll.domain.InvalidInput
import br.ufrn.noscroll.domain.NewSession
import br.ufrn.noscroll.domain.NewSetup
import br.ufrn.noscroll.domain.Page
import br.ufrn.noscroll.domain.PageRequest
import br.ufrn.noscroll.domain.Session
import br.ufrn.noscroll.domain.SessionRepository
import br.ufrn.noscroll.domain.Setup
import br.ufrn.noscroll.domain.SetupRepository
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.OpenApiInfo
import io.ktor.openapi.jsonSchema
import io.ktor.server.application.Application
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.format.DateTimeParseException

@OptIn(ExperimentalKtorApi::class)
fun Application.routes() {
    val setups by inject<SetupRepository>()
    val sessions by inject<SessionRepository>()

    routing {
        route("/setups") {
            get {
                val page = call.request.queryParameters["page"]
                    ?.let { it.toIntOrNull() ?: throw BadRequestException("page deve ser um número inteiro") } ?: 0
                val size = call.request.queryParameters["size"]
                    ?.let { it.toIntOrNull() ?: throw BadRequestException("size deve ser um número inteiro") }
                    ?: PageRequest.DEFAULT_SIZE
                if (page < 0) throw BadRequestException("page deve ser >= 0")
                if (size !in 1..PageRequest.MAX_SIZE) throw BadRequestException("size deve estar entre 1 e ${PageRequest.MAX_SIZE}")
                val app = call.request.queryParameters["app"]?.takeIf { it.isNotBlank() }
                call.respond(setups.list(app, PageRequest(page, size)))
            }.describe {
                summary = "Lista os setups, paginados e filtrados por nome do app"
                parameters {
                    query("page") { schema = jsonSchema<Int>() }
                    query("size") { schema = jsonSchema<Int>() }
                    query("app") { schema = jsonSchema<String>() }
                }
                responses {
                    HttpStatusCode.OK { schema = jsonSchema<Page<Setup>>() }
                    HttpStatusCode.BadRequest { schema = jsonSchema<Problem>() }
                }
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw BadRequestException("O id deve ser um número inteiro")
                val setup = setups.find(id)
                    ?: throw NotFoundException("O setup $id não existe")
                call.respond(setup)
            }.describe {
                summary = "Busca um setup pelo id"
                parameters { path("id") { schema = jsonSchema<Int>() } }
                responses {
                    HttpStatusCode.OK { schema = jsonSchema<Setup>() }
                    HttpStatusCode.BadRequest { schema = jsonSchema<Problem>() }
                    HttpStatusCode.NotFound { schema = jsonSchema<Problem>() }
                }
            }

            post {
                val new = call.receive<NewSetup>()
                val violations = new.violations()
                if (violations.isNotEmpty()) throw InvalidInput(violations)
                val created = setups.add(new)
                call.response.header(HttpHeaders.Location, "/setups/${created.id}")
                call.respond(HttpStatusCode.Created, created)
            }.describe {
                summary = "Cria um setup"
                requestBody { schema = jsonSchema<NewSetup>() }
                responses {
                    HttpStatusCode.Created { schema = jsonSchema<Setup>() }
                    HttpStatusCode.BadRequest { schema = jsonSchema<Problem>() }
                    HttpStatusCode.UnprocessableEntity { schema = jsonSchema<Problem>() }
                }
            }

            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw BadRequestException("O id deve ser um número inteiro")
                val new = call.receive<NewSetup>()
                val violations = new.violations()
                if (violations.isNotEmpty()) throw InvalidInput(violations)
                val updated = setups.update(id, new)
                    ?: throw NotFoundException("O setup $id não existe")
                call.respond(updated)
            }.describe {
                summary = "Substitui um setup"
                parameters { path("id") { schema = jsonSchema<Int>() } }
                requestBody { schema = jsonSchema<NewSetup>() }
                responses {
                    HttpStatusCode.OK { schema = jsonSchema<Setup>() }
                    HttpStatusCode.BadRequest { schema = jsonSchema<Problem>() }
                    HttpStatusCode.NotFound { schema = jsonSchema<Problem>() }
                    HttpStatusCode.UnprocessableEntity { schema = jsonSchema<Problem>() }
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw BadRequestException("O id deve ser um número inteiro")
                if (!setups.remove(id)) throw NotFoundException("O setup $id não existe")
                call.respond(HttpStatusCode.NoContent)
            }.describe {
                summary = "Remove um setup"
                parameters { path("id") { schema = jsonSchema<Int>() } }
                responses {
                    HttpStatusCode.NoContent { description = "Setup removido" }
                    HttpStatusCode.BadRequest { schema = jsonSchema<Problem>() }
                    HttpStatusCode.NotFound { schema = jsonSchema<Problem>() }
                }
            }

            route("/{id}/sessions") {
                get {
                    val setupId = call.parameters["id"]?.toIntOrNull()
                        ?: throw BadRequestException("O id deve ser um número inteiro")
                    val date = call.request.queryParameters["date"]?.let {
                        try {
                            LocalDate.parse(it)
                        } catch (e: DateTimeParseException) {
                            throw BadRequestException("date deve estar no formato AAAA-MM-DD")
                        }
                    }
                    if (setups.find(setupId) == null) throw NotFoundException("O setup $setupId não existe")
                    call.respond(sessions.list(setupId, date))
                }.describe {
                    summary = "Lista as sessões de um setup, filtradas por dia (UTC)"
                    parameters {
                        path("id") { schema = jsonSchema<Int>() }
                        query("date") { schema = jsonSchema<String>() }
                    }
                    responses {
                        HttpStatusCode.OK { schema = jsonSchema<List<Session>>() }
                        HttpStatusCode.BadRequest { schema = jsonSchema<Problem>() }
                        HttpStatusCode.NotFound { schema = jsonSchema<Problem>() }
                    }
                }

                post {
                    val setupId = call.parameters["id"]?.toIntOrNull()
                        ?: throw BadRequestException("O id deve ser um número inteiro")
                    if (setups.find(setupId) == null) throw NotFoundException("O setup $setupId não existe")
                    val new = call.receive<NewSession>()
                    val violations = new.violations()
                    if (violations.isNotEmpty()) throw InvalidInput(violations)
                    val created = sessions.add(setupId, new)
                    call.response.header(HttpHeaders.Location, "/setups/$setupId/sessions/${created.id}")
                    call.respond(HttpStatusCode.Created, created)
                }.describe {
                    summary = "Registra uma sessão de uso"
                    parameters { path("id") { schema = jsonSchema<Int>() } }
                    requestBody { schema = jsonSchema<NewSession>() }
                    responses {
                        HttpStatusCode.Created { schema = jsonSchema<Session>() }
                        HttpStatusCode.BadRequest { schema = jsonSchema<Problem>() }
                        HttpStatusCode.NotFound { schema = jsonSchema<Problem>() }
                        HttpStatusCode.UnprocessableEntity { schema = jsonSchema<Problem>() }
                    }
                }

                get("/{sessionId}") {
                    val setupId = call.parameters["id"]?.toIntOrNull()
                        ?: throw BadRequestException("O id deve ser um número inteiro")
                    val sessionId = call.parameters["sessionId"]?.toIntOrNull()
                        ?: throw BadRequestException("O sessionId deve ser um número inteiro")
                    val session = sessions.find(setupId, sessionId)
                        ?: throw NotFoundException("A sessão $sessionId não existe no setup $setupId")
                    call.respond(session)
                }.describe {
                    summary = "Busca uma sessão"
                    parameters {
                        path("id") { schema = jsonSchema<Int>() }
                        path("sessionId") { schema = jsonSchema<Int>() }
                    }
                    responses {
                        HttpStatusCode.OK { schema = jsonSchema<Session>() }
                        HttpStatusCode.BadRequest { schema = jsonSchema<Problem>() }
                        HttpStatusCode.NotFound { schema = jsonSchema<Problem>() }
                    }
                }

                put("/{sessionId}") {
                    val setupId = call.parameters["id"]?.toIntOrNull()
                        ?: throw BadRequestException("O id deve ser um número inteiro")
                    val sessionId = call.parameters["sessionId"]?.toIntOrNull()
                        ?: throw BadRequestException("O sessionId deve ser um número inteiro")
                    val new = call.receive<NewSession>()
                    val violations = new.violations()
                    if (violations.isNotEmpty()) throw InvalidInput(violations)
                    val updated = sessions.update(setupId, sessionId, new)
                        ?: throw NotFoundException("A sessão $sessionId não existe no setup $setupId")
                    call.respond(updated)
                }.describe {
                    summary = "Substitui uma sessão"
                    parameters {
                        path("id") { schema = jsonSchema<Int>() }
                        path("sessionId") { schema = jsonSchema<Int>() }
                    }
                    requestBody { schema = jsonSchema<NewSession>() }
                    responses {
                        HttpStatusCode.OK { schema = jsonSchema<Session>() }
                        HttpStatusCode.BadRequest { schema = jsonSchema<Problem>() }
                        HttpStatusCode.NotFound { schema = jsonSchema<Problem>() }
                        HttpStatusCode.UnprocessableEntity { schema = jsonSchema<Problem>() }
                    }
                }

                delete("/{sessionId}") {
                    val setupId = call.parameters["id"]?.toIntOrNull()
                        ?: throw BadRequestException("O id deve ser um número inteiro")
                    val sessionId = call.parameters["sessionId"]?.toIntOrNull()
                        ?: throw BadRequestException("O sessionId deve ser um número inteiro")
                    if (!sessions.remove(setupId, sessionId)) {
                        throw NotFoundException("A sessão $sessionId não existe no setup $setupId")
                    }
                    call.respond(HttpStatusCode.NoContent)
                }.describe {
                    summary = "Remove uma sessão"
                    parameters {
                        path("id") { schema = jsonSchema<Int>() }
                        path("sessionId") { schema = jsonSchema<Int>() }
                    }
                    responses {
                        HttpStatusCode.NoContent { description = "Sessão removida" }
                        HttpStatusCode.BadRequest { schema = jsonSchema<Problem>() }
                        HttpStatusCode.NotFound { schema = jsonSchema<Problem>() }
                    }
                }
            }
        }

        swaggerUI(path = "docs") {
            info = OpenApiInfo(title = "no-scroll", version = "1.0")
        }
    }
}
