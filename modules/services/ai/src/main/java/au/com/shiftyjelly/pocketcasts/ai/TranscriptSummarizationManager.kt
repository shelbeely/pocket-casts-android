package au.com.shiftyjelly.pocketcasts.ai

import kotlinx.coroutines.flow.Flow

interface TranscriptSummarizationManager {
    fun summarize(transcript: String): Flow<AiGenerationState<String>>
}
