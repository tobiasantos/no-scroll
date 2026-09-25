package br.ufrn.noscroll.adapters.web

import br.ufrn.noscroll.domain.NewSetup
import br.ufrn.noscroll.domain.Setup
import br.ufrn.noscroll.domain.SetupRepository
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.OpenApiInfo
import io.ktor.openapi.jsonSchema
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
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
                call.respond(HttpStatusCode.Created, repository.add(new))
            }.describe {
                summary = "Cria um setup"
                requestBody { schema = jsonSchema<NewSetup>() }
                responses { HttpStatusCode.Created { schema = jsonSchema<Setup>() } }
            }
        }

        swaggerUI(path = "docs") {
            info = OpenApiInfo(title = "no-scroll", version = "1.0")
        }
    }
}
