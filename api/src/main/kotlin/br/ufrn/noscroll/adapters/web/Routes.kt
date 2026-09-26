package br.ufrn.noscroll.adapters.web

import br.ufrn.noscroll.domain.NewSetup
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

@OptIn(ExperimentalKtorApi::class)
fun Application.routes() {
    val repository by inject<SetupRepository>()

    routing {
        route("/setups") {
            get { call.respond(repository.list()) }.describe {
                summary = "Lista os setups"
                responses { HttpStatusCode.OK { schema = jsonSchema<List<Setup>>() } }
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val setup = repository.find(id)
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
                val created = repository.add(new)
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
                val updated = repository.update(id, call.receive<NewSetup>())
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
                if (!repository.remove(id)) return@delete call.respond(HttpStatusCode.NotFound)
                call.respond(HttpStatusCode.NoContent)
            }.describe {
                summary = "Remove um setup"
                parameters { path("id") { schema = jsonSchema<Int>() } }
                responses {
                    HttpStatusCode.NoContent { description = "Setup removido" }
                    HttpStatusCode.NotFound { description = "Setup inexistente" }
                }
            }
        }

        swaggerUI(path = "docs") {
            info = OpenApiInfo(title = "no-scroll", version = "1.0")
        }
    }
}
