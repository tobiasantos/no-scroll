package br.ufrn.noscroll.adapters.web

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
                    ?.let { it.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest) } ?: 0
                val size = call.request.queryParameters["size"]
                    ?.let { it.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest) } ?: PageRequest.DEFAULT_SIZE
                if (page < 0 || size !in 1..PageRequest.MAX_SIZE) return@get call.respond(HttpStatusCode.BadRequest)
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
                    HttpStatusCode.BadRequest { description = "page ou size inválido (size entre 1 e 100)" }
                }
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val setup = setups.find(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(setup)
            }.describe {
                summary = "Busca um setup pelo id"
                parameters { path("id") { schema = jsonSchema<Int>() } }
                responses {
                    HttpStatusCode.OK { schema = jsonSchema<Setup>() }
                    HttpStatusCode.NotFound { description = "Setup inexistente" }
                }
            }

            post {
                val new = call.receive<NewSetup>()
                val created = setups.add(new)
                call.response.header(HttpHeaders.Location, "/setups/${created.id}")
                call.respond(HttpStatusCode.Created, created)
            }.describe {
                summary = "Cria um setup"
                requestBody { schema = jsonSchema<NewSetup>() }
                responses { HttpStatusCode.Created { schema = jsonSchema<Setup>() } }
            }

            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
                val updated = setups.update(id, call.receive<NewSetup>())
                    ?: return@put call.respond(HttpStatusCode.NotFound)
                call.respond(updated)
            }.describe {
                summary = "Substitui um setup"
                parameters { path("id") { schema = jsonSchema<Int>() } }
                requestBody { schema = jsonSchema<NewSetup>() }
                responses {
                    HttpStatusCode.OK { schema = jsonSchema<Setup>() }
                    HttpStatusCode.NotFound { description = "Setup inexistente" }
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)
                if (!setups.remove(id)) return@delete call.respond(HttpStatusCode.NotFound)
                call.respond(HttpStatusCode.NoContent)
            }.describe {
                summary = "Remove um setup"
                parameters { path("id") { schema = jsonSchema<Int>() } }
                responses {
                    HttpStatusCode.NoContent { description = "Setup removido" }
                    HttpStatusCode.NotFound { description = "Setup inexistente" }
                }
            }

            route("/{id}/sessions") {
                get {
                    val setupId = call.parameters["id"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val date = call.request.queryParameters["date"]?.let {
                        try {
                            LocalDate.parse(it)
                        } catch (e: DateTimeParseException) {
                            return@get call.respond(HttpStatusCode.BadRequest)
                        }
                    }
                    if (setups.find(setupId) == null) return@get call.respond(HttpStatusCode.NotFound)
                    call.respond(sessions.list(setupId, date))
                }.describe {
                    summary = "Lista as sessões de um setup, filtradas por dia (UTC)"
                    parameters {
                        path("id") { schema = jsonSchema<Int>() }
                        query("date") { schema = jsonSchema<String>() }
                    }
                    responses {
                        HttpStatusCode.OK { schema = jsonSchema<List<Session>>() }
                        HttpStatusCode.BadRequest { description = "date fora do formato AAAA-MM-DD" }
                        HttpStatusCode.NotFound { description = "Setup inexistente" }
                    }
                }

                post {
                    val setupId = call.parameters["id"]?.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest)
                    if (setups.find(setupId) == null) return@post call.respond(HttpStatusCode.NotFound)
                    val created = sessions.add(setupId, call.receive<NewSession>())
                    call.response.header(HttpHeaders.Location, "/setups/$setupId/sessions/${created.id}")
                    call.respond(HttpStatusCode.Created, created)
                }.describe {
                    summary = "Registra uma sessão de uso"
                    parameters { path("id") { schema = jsonSchema<Int>() } }
                    requestBody { schema = jsonSchema<NewSession>() }
                    responses {
                        HttpStatusCode.Created { schema = jsonSchema<Session>() }
                        HttpStatusCode.NotFound { description = "Setup inexistente" }
                    }
                }

                get("/{sessionId}") {
                    val setupId = call.parameters["id"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val sessionId = call.parameters["sessionId"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val session = sessions.find(setupId, sessionId)
                        ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.respond(session)
                }.describe {
                    summary = "Busca uma sessão"
                    parameters {
                        path("id") { schema = jsonSchema<Int>() }
                        path("sessionId") { schema = jsonSchema<Int>() }
                    }
                    responses {
                        HttpStatusCode.OK { schema = jsonSchema<Session>() }
                        HttpStatusCode.NotFound { description = "Sessão inexistente neste setup" }
                    }
                }

                put("/{sessionId}") {
                    val setupId = call.parameters["id"]?.toIntOrNull()
                        ?: return@put call.respond(HttpStatusCode.BadRequest)
                    val sessionId = call.parameters["sessionId"]?.toIntOrNull()
                        ?: return@put call.respond(HttpStatusCode.BadRequest)
                    val updated = sessions.update(setupId, sessionId, call.receive<NewSession>())
                        ?: return@put call.respond(HttpStatusCode.NotFound)
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
                        HttpStatusCode.NotFound { description = "Sessão inexistente neste setup" }
                    }
                }

                delete("/{sessionId}") {
                    val setupId = call.parameters["id"]?.toIntOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    val sessionId = call.parameters["sessionId"]?.toIntOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    if (!sessions.remove(setupId, sessionId)) return@delete call.respond(HttpStatusCode.NotFound)
                    call.respond(HttpStatusCode.NoContent)
                }.describe {
                    summary = "Remove uma sessão"
                    parameters {
                        path("id") { schema = jsonSchema<Int>() }
                        path("sessionId") { schema = jsonSchema<Int>() }
                    }
                    responses {
                        HttpStatusCode.NoContent { description = "Sessão removida" }
                        HttpStatusCode.NotFound { description = "Sessão inexistente neste setup" }
                    }
                }
            }
        }

        swaggerUI(path = "docs") {
            info = OpenApiInfo(title = "no-scroll", version = "1.0")
        }
    }
}
