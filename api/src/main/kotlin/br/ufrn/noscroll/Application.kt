package br.ufrn.noscroll

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
        routing { get("/") { call.respondText("no ar") } }
    }.start(wait = true)
}
