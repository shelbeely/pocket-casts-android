package au.com.shiftyjelly.pocketcasts.ai

import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class TldlManagerImplTest {

    private val aiCoreManager: AiCoreManager = mock()
    private val manager = TldlManagerImpl(aiCoreManager)

    @Test
    fun `buildPrompt includes all episode titles`() {
        val episodes = listOf(
            TldlEpisodeInput("Episode One", Date(), "Description one", 1800),
            TldlEpisodeInput("Episode Two", Date(), "Description two", 2700),
        )
        val prompt = manager.buildPrompt("My Podcast", episodes)

        assertTrue(prompt.contains("Episode One"))
        assertTrue(prompt.contains("Episode Two"))
        assertTrue(prompt.contains("My Podcast"))
    }

    @Test
    fun `buildPrompt truncates per-episode description to 600 chars`() {
        val longDesc = "x".repeat(1200)
        val episodes = listOf(TldlEpisodeInput("Title", Date(), longDesc, 3600))

        val prompt = manager.buildPrompt("Podcast", episodes)

        // The description in the prompt should be at most 600 chars
        val descStart = prompt.indexOf("Title") + "Title".length
        // Find the truncated portion by checking total prompt doesn't embed more than 600 chars of 'x'
        val xCount = prompt.count { it == 'x' }
        assertTrue("Description should be truncated to at most 600 'x' chars, but got $xCount", xCount <= 600)
    }

    @Test
    fun `buildPrompt truncates total prompt to 12000 chars`() {
        val manyEpisodes = (1..20).map { i ->
            TldlEpisodeInput("Episode $i", Date(), "d".repeat(600), 3600)
        }
        val prompt = manager.buildPrompt("Big Podcast", manyEpisodes)

        assertTrue("Prompt exceeds 12000 chars: ${prompt.length}", prompt.length <= 12_000)
    }

    @Test
    fun `buildPrompt contains format section for each episode`() {
        val episodes = listOf(
            TldlEpisodeInput("Ep1", Date(), "Desc", 600),
            TldlEpisodeInput("Ep2", Date(), "Desc", 600),
            TldlEpisodeInput("Ep3", Date(), "Desc", 600),
        )
        val prompt = manager.buildPrompt("Show", episodes)

        assertTrue(prompt.contains("• Episode 1:"))
        assertTrue(prompt.contains("• Episode 2:"))
        assertTrue(prompt.contains("• Episode 3:"))
        assertTrue(prompt.contains("Overall:"))
    }

    @Test
    fun `buildPrompt handles single episode`() {
        val episodes = listOf(TldlEpisodeInput("Solo", Date(), "A description", 900))
        val prompt = manager.buildPrompt("Podcast", episodes)

        assertEquals(1, prompt.split("Episode 1:").size - 1)
    }

    @Test
    fun `buildPrompt handles ten episodes`() {
        val episodes = (1..10).map { i ->
            TldlEpisodeInput("Ep $i", Date(), "Some description", 1200)
        }
        val prompt = manager.buildPrompt("Series", episodes)

        assertTrue(prompt.contains("• Episode 10:"))
    }
}
