package br.ufrn.noscroll.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class ValidationTest {

    private val valid = NewSetup("com.instagram.android", "Instagram", 60)

    @Test
    fun `setup valido nao tem violacoes`() {
        assertTrue(valid.violations().isEmpty())
        assertTrue(valid.copy(alertMode = AlertMode.PERIODIC, alertIntervalMinutes = 15).violations().isEmpty())
    }

    @Test
    fun `nome em branco e limite fora da faixa sao violacoes`() {
        val violations = valid.copy(appName = "  ", dailyLimitMinutes = 0).violations()
        assertEquals(listOf("appName: não pode ficar em branco", "dailyLimitMinutes: entre 1 e 1440"), violations)
    }

    @Test
    fun `alerta periodico exige intervalo`() {
        val violations = valid.copy(alertMode = AlertMode.PERIODIC).violations()
        assertEquals(listOf("alertIntervalMinutes: obrigatório quando alertMode é PERIODIC"), violations)
    }

    @Test
    fun `sessao exige duracao positiva`() {
        val start = Instant.parse("2026-09-28T10:00:00Z")
        assertTrue(NewSession(start, 25).violations().isEmpty())
        assertEquals(listOf("durationMinutes: entre 1 e 1440"), NewSession(start, 0).violations())
    }
}
