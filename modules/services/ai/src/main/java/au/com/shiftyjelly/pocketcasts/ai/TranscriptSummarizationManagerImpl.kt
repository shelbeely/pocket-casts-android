package au.com.shiftyjelly.pocketcasts.ai

import kotlinx.coroutines.flow.Flow

private const val MAX_TRANSCRIPT_CHARS = 14_000

class TranscriptSummarizationManagerImpl(
    private val aiCoreManager: AiCoreManager,
) : TranscriptSummarizationManager {

    override fun summarize(transcript: String): Flow<AiGenerationState<String>> {
        val truncated = transcript.take(MAX_TRANSCRIPT_CHARS)
        val prompt = "Summarize this podcast episode transcript in 3–5 sentences:\n\n$truncated"
        return aiCoreManager.generateStreaming(prompt)
    }
}
