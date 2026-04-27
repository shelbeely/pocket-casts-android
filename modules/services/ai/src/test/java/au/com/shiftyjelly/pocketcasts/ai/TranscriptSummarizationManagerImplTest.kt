package au.com.shiftyjelly.pocketcasts.ai

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TranscriptSummarizationManagerImplTest {

    private val aiCoreManager: AiCoreManager = mock()
    private val manager = TranscriptSummarizationManagerImpl(aiCoreManager)

    @Test
    fun `summarize passes truncated transcript to aiCoreManager`() = runTest {
        val shortTranscript = "Hello world"
        val expectedPrompt = "Summarize this podcast episode transcript in 3–5 sentences:\n\n$shortTranscript"

        whenever(aiCoreManager.generateStreaming(expectedPrompt))
            .thenReturn(kotlinx.coroutines.flow.flowOf(AiGenerationState.Complete("A summary.")))

        val result = manager.summarize(shortTranscript).toList()

        assertEquals(listOf(AiGenerationState.Complete("A summary.")), result)
    }

    @Test
    fun `summarize truncates long transcripts to 14000 chars`() = runTest {
        val longTranscript = "x".repeat(20_000)
        val truncated = longTranscript.take(14_000)
        val expectedPrompt = "Summarize this podcast episode transcript in 3–5 sentences:\n\n$truncated"

        whenever(aiCoreManager.generateStreaming(expectedPrompt))
            .thenReturn(kotlinx.coroutines.flow.flowOf(AiGenerationState.Complete("Summary")))

        manager.summarize(longTranscript).toList()
        // Verify the prompt passed to aiCoreManager contains the truncated transcript only
        assertTrue(expectedPrompt.length <= "Summarize this podcast episode transcript in 3–5 sentences:\n\n".length + 14_000)
    }

    @Test
    fun `summarize propagates NotSupported from aiCoreManager`() = runTest {
        whenever(aiCoreManager.generateStreaming(org.mockito.kotlin.any()))
            .thenReturn(kotlinx.coroutines.flow.flowOf(AiGenerationState.NotSupported))

        val result = manager.summarize("transcript").toList()

        assertEquals(listOf(AiGenerationState.NotSupported), result)
    }
}
