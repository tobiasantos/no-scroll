package br.ufrn.noscroll.adapters.evaluator

import br.ufrn.noscroll.domain.Session
import br.ufrn.noscroll.domain.Setup
import br.ufrn.noscroll.domain.UsageEvaluator

class InMemoryUsageEvaluator : UsageEvaluator {
    val evaluated = mutableListOf<Pair<Setup, List<Session>>>()

    override suspend fun evaluate(setup: Setup, todaySessions: List<Session>): Boolean {
        evaluated += setup to todaySessions
        return true
    }
}
