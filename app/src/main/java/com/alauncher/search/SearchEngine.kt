package com.alauncher.search

import com.alauncher.data.model.LauncherApp
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fast fuzzy search engine for apps.
 * Supports negative search with `-` or `NOT` prefix.
 */
@Singleton
class SearchEngine @Inject constructor() {

    /**
     * Search apps by query string.
     * Supports:
     * - Fuzzy matching against app label and package name
     * - Negative filters: `-games` or `NOT games` excludes matches
     * - Multiple terms: all positive terms must match, any negative excludes
     */
    fun search(query: String, apps: List<LauncherApp>): List<LauncherApp> {
        if (query.isBlank()) return emptyList()

        val terms = query.trim().split("\\s+".toRegex())
        val positiveTerms = mutableListOf<String>()
        val negativeTerms = mutableListOf<String>()

        var i = 0
        while (i < terms.size) {
            val term = terms[i]
            when {
                term.equals("NOT", ignoreCase = true) && i + 1 < terms.size -> {
                    negativeTerms.add(terms[i + 1].lowercase())
                    i += 2
                }
                term.startsWith("-") && term.length > 1 -> {
                    negativeTerms.add(term.substring(1).lowercase())
                    i++
                }
                else -> {
                    positiveTerms.add(term.lowercase())
                    i++
                }
            }
        }

        return apps.filter { app ->
            val label = app.label.lowercase()
            val pkg = app.packageName.lowercase()
            val searchable = "$label $pkg"

            // All positive terms must match
            val positiveMatch = positiveTerms.isEmpty() || positiveTerms.all { term ->
                fuzzyMatch(searchable, term)
            }

            // No negative terms should match
            val negativeMatch = negativeTerms.any { term ->
                fuzzyMatch(searchable, term)
            }

            positiveMatch && !negativeMatch
        }.sortedByDescending { app ->
            // Score: exact prefix > contains > fuzzy
            val label = app.label.lowercase()
            val firstTerm = positiveTerms.firstOrNull() ?: ""
            when {
                label.startsWith(firstTerm) -> 3f + app.gravityScore
                label.contains(firstTerm) -> 2f + app.gravityScore
                else -> 1f + app.gravityScore
            }
        }
    }

    /**
     * Fuzzy match: checks if all characters of the term appear in order in the text.
     * Also matches substrings.
     */
    private fun fuzzyMatch(text: String, term: String): Boolean {
        // First check substring match
        if (text.contains(term)) return true

        // Then fuzzy: all chars appear in order
        var termIdx = 0
        for (char in text) {
            if (termIdx < term.length && char == term[termIdx]) {
                termIdx++
            }
        }
        return termIdx == term.length
    }
}
