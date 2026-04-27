package au.com.shiftyjelly.pocketcasts.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.gms.genai.GenerativeModel
import com.google.android.gms.genai.GenerativeModelFutures
import com.google.android.gms.genai.type.GenerativeBackend
import com.google.android.gms.genai.type.content
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Production implementation of [AiCoreManager] backed by Android AICore (Gemini Nano on-device).
 *
 * On creation it checks model availability asynchronously on [Dispatchers.IO].  If the model is
 * downloadable and the device is on Wi-Fi it triggers the download automatically.  All state
 * changes are reflected in [availability].
 *
 * Requires the `play-services-genai-inferencing` Gradle dependency and a device running
 * Android 14 (API 34) or later that supports Android AICore.
 */
class AiCoreManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AiCoreManager {

    private val _availability = MutableStateFlow<AiAvailability>(AiAvailability.NotSupported)
    override val availability: StateFlow<AiAvailability> = _availability.asStateFlow()

    private var generativeModel: GenerativeModel? = null

    init {
        // Availability check is performed on a background thread to avoid blocking the calling
        // (typically main) thread during dependency injection.
        CoroutineScope(Dispatchers.IO).launch { checkAvailability() }
    }

    private suspend fun checkAvailability() {
        try {
            val model = GenerativeModel.builder()
                .setBackend(GenerativeBackend.aiCore())
                .build()
            generativeModel = model

            val status = model.checkAvailability().await()
            when (status) {
                com.google.android.gms.genai.type.AvailabilityStatus.AVAILABLE -> {
                    _availability.value = AiAvailability.Available
                }
                com.google.android.gms.genai.type.AvailabilityStatus.DOWNLOADABLE -> {
                    if (isOnWifi()) {
                        _availability.value = AiAvailability.Downloading
                        model.requestDownload()
                            .addOnSuccessListener { _availability.value = AiAvailability.Available }
                            .addOnFailureListener { e ->
                                Timber.w(e, "AICore model download failed")
                                _availability.value = AiAvailability.NotSupported
                            }
                    } else {
                        Timber.d("AICore model is downloadable but device is not on Wi-Fi; skipping download")
                        _availability.value = AiAvailability.NotSupported
                    }
                }
                else -> {
                    _availability.value = AiAvailability.NotSupported
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "AICore availability check failed")
            _availability.value = AiAvailability.NotSupported
        }
    }

    private fun isOnWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override fun isCapabilityEnabled(capability: AiCapability): Boolean =
        _availability.value == AiAvailability.Available

    override fun generateStreaming(prompt: String): Flow<AiGenerationState<String>> = flow {
        val model = generativeModel
        if (model == null || _availability.value != AiAvailability.Available) {
            emit(AiGenerationState.NotSupported)
            return@flow
        }
        try {
            val accumulated = StringBuilder()
            val futures = GenerativeModelFutures.from(model)
            val request = content { text(prompt) }
            futures.generateContentStream(request).collect { chunk ->
                val token = chunk.text ?: return@collect
                accumulated.append(token)
                emit(AiGenerationState.Generating(accumulated.toString()))
            }
            emit(AiGenerationState.Complete(accumulated.toString()))
        } catch (e: Exception) {
            Timber.w(e, "AICore streaming generation failed")
            emit(AiGenerationState.Error(e))
        }
    }

    override suspend fun generateOnce(prompt: String): String {
        val model = generativeModel
            ?: throw UnsupportedOperationException("Generative model is not initialised")
        if (_availability.value != AiAvailability.Available) {
            throw UnsupportedOperationException("AI is not available on this device")
        }
        val futures = GenerativeModelFutures.from(model)
        val request = content { text(prompt) }
        val response = futures.generateContent(request).await()
        return response.text ?: ""
    }
}
