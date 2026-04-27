package au.com.shiftyjelly.pocketcasts.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AiCoreManager {
    val availability: StateFlow<AiAvailability>
    fun isCapabilityEnabled(capability: AiCapability): Boolean
    fun generateStreaming(prompt: String): Flow<AiGenerationState<String>>
    suspend fun generateOnce(prompt: String): String
}
