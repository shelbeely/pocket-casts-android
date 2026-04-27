package au.com.shiftyjelly.pocketcasts.ai.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.ai.AiCoreManager
import au.com.shiftyjelly.pocketcasts.ai.AiCoreManagerImpl
import au.com.shiftyjelly.pocketcasts.ai.NoOpAiCoreManager
import au.com.shiftyjelly.pocketcasts.ai.TranscriptSummarizationManager
import au.com.shiftyjelly.pocketcasts.ai.TranscriptSummarizationManagerImpl
import au.com.shiftyjelly.pocketcasts.ai.TldlManager
import au.com.shiftyjelly.pocketcasts.ai.TldlManagerImpl
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideAiCoreManager(
        @ApplicationContext context: Context,
    ): AiCoreManager = if (FeatureFlag.isEnabled(Feature.GEMINI_NANO_AI)) {
        AiCoreManagerImpl(context)
    } else {
        NoOpAiCoreManager()
    }

    @Provides
    @Singleton
    fun provideTranscriptSummarizationManager(
        aiCoreManager: AiCoreManager,
    ): TranscriptSummarizationManager = TranscriptSummarizationManagerImpl(aiCoreManager)

    @Provides
    @Singleton
    fun provideTldlManager(
        aiCoreManager: AiCoreManager,
    ): TldlManager = TldlManagerImpl(aiCoreManager)
}
