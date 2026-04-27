package au.com.shiftyjelly.pocketcasts.ai

sealed interface AiGenerationState<out T> {
    data object Idle : AiGenerationState<Nothing>
    data object CheckingAvailability : AiGenerationState<Nothing>
    data object Downloading : AiGenerationState<Nothing>
    data class Generating(val partialText: String) : AiGenerationState<Nothing>
    data class Complete<T>(val result: T) : AiGenerationState<T>
    data class Error(val cause: Throwable) : AiGenerationState<Nothing>
    data object NotSupported : AiGenerationState<Nothing>
}
