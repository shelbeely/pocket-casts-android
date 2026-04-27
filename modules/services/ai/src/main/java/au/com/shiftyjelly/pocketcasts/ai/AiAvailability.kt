package au.com.shiftyjelly.pocketcasts.ai

sealed interface AiAvailability {
    data object Available : AiAvailability
    data object Downloading : AiAvailability
    data object NotSupported : AiAvailability
}
