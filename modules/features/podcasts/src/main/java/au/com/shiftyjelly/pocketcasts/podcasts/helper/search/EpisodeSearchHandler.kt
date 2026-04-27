package au.com.shiftyjelly.pocketcasts.podcasts.helper.search

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManagerImpl
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PodcastScreenSearchClearedEvent
import com.automattic.eventhorizon.PodcastScreenSearchPerformedEvent
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class EpisodeSearchHandler @Inject constructor(
    settings: Settings,
    private val cacheServiceManager: PodcastCacheServiceManagerImpl,
    private val episodeManager: EpisodeManager,
    private val eventHorizon: EventHorizon,
) : SearchHandler<BaseEpisode>() {
    private val searchDebounce = settings.getEpisodeSearchDebounceMs()

    override fun getSearchResultsObservable(podcastUuid: String): Observable<SearchResult> = searchQueryRelay.debounce {
        // Only debounce when search has a value otherwise it slows down loading the pages
        if (it.isEmpty()) {
            Observable.empty()
        } else {
            Observable.timer(searchDebounce, TimeUnit.MILLISECONDS)
        }
    }.switchMap { searchTerm ->
        if (searchTerm.length > 1) {
            // Perform local search immediately so the UI responds without a network round-trip.
            val localSearch = Single.fromCallable {
                episodeManager.searchInPodcastBlocking(podcastUuid, searchTerm)
            }
                .subscribeOn(Schedulers.io())
                .map { localEpisodes -> SearchResult(searchTerm, localEpisodes.map { it.uuid }) }
                .toObservable()

            // Perform remote search and merge, deduplicating by uuid.
            val remoteSearch = cacheServiceManager.searchEpisodes(podcastUuid, searchTerm)
                .map { remoteUuids ->
                    SearchResult(searchTerm, remoteUuids)
                }
                .onErrorReturnItem(noSearchResult)
                .toObservable()

            Observable.mergeDelayError(localSearch, remoteSearch)
                .scan(noSearchResult) { accumulated, next ->
                    if (next.searchUuids == null) {
                        accumulated
                    } else {
                        val merged = ((accumulated.searchUuids ?: emptyList()) + next.searchUuids)
                            .distinct()
                        SearchResult(searchTerm, merged)
                    }
                }
                .skip(1) // skip the initial noSearchResult seed value
                .onErrorReturnItem(noSearchResult)
        } else {
            Observable.just(noSearchResult)
        }
    }.distinctUntilChanged()

    override fun trackSearchIfNeeded(oldValue: String, newValue: String) {
        val event = if (oldValue.isEmpty() && newValue.isNotEmpty()) {
            PodcastScreenSearchPerformedEvent
        } else if (oldValue.isNotEmpty() && newValue.isEmpty()) {
            PodcastScreenSearchClearedEvent
        } else {
            null
        }
        event?.let(eventHorizon::track)
    }
}
