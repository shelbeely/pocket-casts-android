package au.com.shiftyjelly.pocketcasts.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * No-op implementation of [AiCoreManager] used when the GEMINI_NANO_AI feature flag is disabled.
 * Always reports [AiAvailability.NotSupported] and never generates any content.
 */
class NoOpAiCoreManager : AiCoreManager {

    private val _availability = MutableStateFlow<AiAvailability>(AiAvailability.NotSupported)
    override val availability: StateFlow<AiAvailability> = _availability.asStateFlow()

    override fun isCapabilityEnabled(capability: AiCapability): Boolean = false

    override fun generateStreaming(prompt: String): Flow<AiGenerationState<String>> = flow {
        emit(AiGenerationState.NotSupported)
    }

    override suspend fun generateOnce(prompt: String): String =
        throw UnsupportedOperationException("AI is not available on this device")
}
