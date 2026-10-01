package br.ufrn.noscroll.architecture.violation.domain

import io.ktor.http.HttpStatusCode

class Contaminated {
    fun status(): HttpStatusCode = HttpStatusCode.OK
}
