package com.spendwise.app.domain

/** Frequency of a saved consumption description within exactly one category. */
data class NoteSuggestion(val text: String, val useCount: Long, val lastUsedAtMillis: Long)

object NoteSuggestionRules {
    fun query(raw: String): String = raw.trim().take(1000)
    fun matches(text: String, rawQuery: String): Boolean = text.contains(query(rawQuery), ignoreCase = true)
}
