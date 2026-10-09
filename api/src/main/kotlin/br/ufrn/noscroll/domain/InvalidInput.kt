package br.ufrn.noscroll.domain

class InvalidInput(val violations: List<String>) : RuntimeException(violations.joinToString("; "))
