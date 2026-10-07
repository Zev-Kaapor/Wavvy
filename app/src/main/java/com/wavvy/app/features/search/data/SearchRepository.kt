package com.wavvy.app.features.search.data

// Android context
import android.content.Context
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.currentSession
import com.wavvy.app.core.innertube.YouTubeSession
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.search.ui.SearchDimens
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

// What the search asks of YouTube Music, with the account only when the user signed in with Google
class SearchRepository(context: Context) {
    private val appContext = context.applicationContext

    // Words and matches for what was typed so far
    suspend fun suggestions(input: String): Result<List<SearchSuggestion>> =
        InnerTubeClient.searchSuggestions(currentSession(appContext), input).mapCatching(SearchParser::parseSuggestions)

    // The searches kept by the account, null when the user did not sign in with Google
    // With no input the account gives only its latest ones, so the older ones are asked by their first letter when probing
    suspend fun accountHistory(probe: Boolean): Result<AccountHistory?> {
        val session = currentSession(appContext)
        if (session.cookies == null) return Result.success(null)

        val recent = historyOf(session, "").getOrElse { return Result.failure(it) }
        if (!probe) return Result.success(AccountHistory(recent, emptyList()))

        val gate = Semaphore(SearchDimens.HistoryProbeParallel)
        val known = recent.map { it.text.lowercase() }.toSet()
        val older = coroutineScope {
            SearchDimens.HistoryProbes.map { letter ->
                async { gate.withPermit { historyOf(session, letter.toString()).getOrDefault(emptyList()) } }
            }.awaitAll()
        }.flatten()
            .distinctBy { it.text.lowercase() }
            .filter { it.text.lowercase() !in known }
            .sortedBy { it.text.lowercase() }

        return Result.success(AccountHistory(recent, older))
    }

    // The searches of the account that the suggestions of an input bring
    private suspend fun historyOf(session: YouTubeSession, input: String): Result<List<SearchSuggestion.Words>> =
        InnerTubeClient.searchSuggestions(session, input)
            .mapCatching { SearchParser.parseSuggestions(it).filterIsInstance<SearchSuggestion.Words>().filter { words -> words.isHistory } }

    // Takes searches out of the history of the account
    suspend fun removeFromAccount(tokens: List<String>): Result<Unit> {
        if (tokens.isEmpty()) return Result.success(Unit)

        return InnerTubeClient.feedback(currentSession(appContext), tokens).map { }
    }

    // The first page of a search under a filter
    suspend fun search(query: String, category: SearchCategory): Result<SearchPage> {
        val page = InnerTubeClient.search(currentSession(appContext), query = query, params = category.params)
            .mapCatching { SearchParser.parsePage(it, if (category == SearchCategory.Episodes) SearchCategory.All else category) }

        // Episodes are the ones of the search of everything, which has no more pages
        return if (category == SearchCategory.Episodes) {
            page.map { it.copy(topMatch = null, items = it.items.filter { item -> item.kind == HomeItemKind.Episode }) }
        } else {
            page
        }
    }

    // The next page of a search under a filter
    suspend fun more(continuation: String): Result<SearchPage> =
        InnerTubeClient.search(currentSession(appContext), continuation = continuation).mapCatching(SearchParser::parseContinuation)
}
