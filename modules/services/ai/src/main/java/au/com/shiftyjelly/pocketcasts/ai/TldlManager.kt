package au.com.shiftyjelly.pocketcasts.ai

import java.util.Date
import kotlinx.coroutines.flow.Flow

data class TldlEpisodeInput(
    val title: String,
    val publishedDate: Date,
    val description: String,
    val durationSeconds: Int,
)

interface TldlManager {
    fun generateCatchUp(
        podcastTitle: String,
        episodes: List<TldlEpisodeInput>,
    ): Flow<AiGenerationState<String>>
}
