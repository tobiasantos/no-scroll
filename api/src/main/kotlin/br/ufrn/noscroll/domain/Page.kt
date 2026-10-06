package br.ufrn.noscroll.domain

import kotlinx.serialization.Serializable

@Serializable
data class Page<T>(val items: List<T>, val page: Int, val size: Int, val total: Long)

data class PageRequest(val page: Int, val size: Int) {
    val offset: Long get() = page.toLong() * size

    companion object {
        const val DEFAULT_SIZE = 20
        const val MAX_SIZE = 100
    }
}
