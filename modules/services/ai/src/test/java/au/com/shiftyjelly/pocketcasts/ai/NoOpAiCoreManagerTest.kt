package au.com.shiftyjelly.pocketcasts.ai

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NoOpAiCoreManagerTest {

    private val manager = NoOpAiCoreManager()

    @Test
    fun `availability is always NotSupported`() {
        assertEquals(AiAvailability.NotSupported, manager.availability.value)
    }

    @Test
    fun `isCapabilityEnabled always returns false`() {
        AiCapability.entries.forEach { capability ->
            assertEquals(false, manager.isCapabilityEnabled(capability))
        }
    }

    @Test
    fun `generateStreaming emits NotSupported`() = runTest {
        val state = manager.generateStreaming("any prompt").first()
        assertEquals(AiGenerationState.NotSupported, state)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `generateOnce throws UnsupportedOperationException`() = runTest {
        manager.generateOnce("any prompt")
    }
}
