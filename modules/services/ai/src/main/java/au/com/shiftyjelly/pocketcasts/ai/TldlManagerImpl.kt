package au.com.shiftyjelly.pocketcasts.ai

import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.Flow

private const val MAX_DESCRIPTION_CHARS = 600
private const val MAX_PROMPT_CHARS = 12_000
private val DATE_FORMAT = SimpleDateFormat("MMM d, yyyy", Locale.US)

class TldlManagerImpl(
    private val aiCoreManager: AiCoreManager,
) : TldlManager {

    override fun generateCatchUp(
        podcastTitle: String,
        episodes: List<TldlEpisodeInput>,
    ): Flow<AiGenerationState<String>> {
        val prompt = buildPrompt(podcastTitle, episodes)
        return aiCoreManager.generateStreaming(prompt)
    }

    internal fun buildPrompt(podcastTitle: String, episodes: List<TldlEpisodeInput>): String {
        val sb = StringBuilder()
        sb.append("You are a helpful podcast assistant. The user has missed several episodes of '$podcastTitle'.\n")
        sb.append("Below are the ${episodes.size} most recent unheard episodes in reverse-chronological order.\n")
        sb.append("For each, write 1–2 sentences summarizing the key topic or news.\n")
        sb.append("Then write a 2–3 sentence 'Overall' paragraph tying the themes together.\n\n")

        episodes.forEachIndexed { index, episode ->
            val date = DATE_FORMAT.format(episode.publishedDate)
            val truncatedDesc = episode.description.take(MAX_DESCRIPTION_CHARS)
            sb.append("Episode ${index + 1}: ${episode.title} ($date)\n")
            sb.append("$truncatedDesc\n\n")
        }

        sb.append("Format:\n")
        episodes.forEachIndexed { index, _ ->
            sb.append("• Episode ${index + 1}: <summary>\n")
        }
        sb.append("Overall: <paragraph>")

        return sb.toString().take(MAX_PROMPT_CHARS)
    }
}
