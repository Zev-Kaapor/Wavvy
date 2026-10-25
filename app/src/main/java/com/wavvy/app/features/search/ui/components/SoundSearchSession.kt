package com.wavvy.app.features.search.ui.components

// Android permission, settings and intents
import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
// Compose animation
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.recognition.MusicRecognizer
import com.wavvy.app.core.recognition.RecognitionException
import com.wavvy.app.core.recognition.RecognitionFailure
import com.wavvy.app.features.search.ui.SearchDimens

// What the search by sound is doing, listening, waiting for the song or telling why it was not found
internal sealed interface SoundPhase {
    data object Listening : SoundPhase
    data object Finding : SoundPhase
    data class Failed(val failure: RecognitionFailure) : SoundPhase
}

// What a screen that listens for a song shows and can do, the phase, how loud the sound is, whether the microphone was refused for good, and the ways on
internal class SoundSearch(
    val phase: SoundPhase,
    val level: Float,
    val isRefused: Boolean,
    val listenAgain: () -> Unit,
    val openSettings: () -> Unit
)

// Listens for a song and keeps what it is doing in state that any screen can read, the name of the song that is found goes to onFound
// The light and the pill in the app, the screen over the other apps and the windows of the service are all faces of this same search
internal class SoundSearchRunner(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onFound: (String) -> Unit
) {
    var phase by mutableStateOf<SoundPhase>(SoundPhase.Listening)
        private set
    var level by mutableFloatStateOf(0f)
        private set
    private var job: Job? = null

    fun listen() {
        job?.cancel()
        phase = SoundPhase.Listening
        level = 0f
        job = scope.launch {
            runCatching { MusicRecognizer.recognize(context, onLevel = { level = it }, onListened = { phase = SoundPhase.Finding }) }
                .onSuccess(onFound)
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    phase = SoundPhase.Failed((error as? RecognitionException)?.failure ?: RecognitionFailure.Offline)
                }
        }
    }

    fun fail(failure: RecognitionFailure) {
        phase = SoundPhase.Failed(failure)
    }

    fun cancel() {
        job?.cancel()
    }

    // A microphone that was refused for good cannot be asked again, so when it was refused the way on is the settings of the app
    fun isRefused(): Boolean {
        val current = phase
        return current is SoundPhase.Failed && current.failure == RecognitionFailure.NoMicrophone && !MusicRecognizer.hasPermission(context)
    }

    fun openSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

// Listens for a song as soon as it is drawn, asking for the microphone the first time, for the screens that are an activity
@Composable
internal fun rememberSoundSearch(onFound: (String) -> Unit): SoundSearch {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val runner = remember { SoundSearchRunner(context, scope, onFound) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) runner.listen() else runner.fail(RecognitionFailure.NoMicrophone)
    }
    val askAgain = {
        if (MusicRecognizer.hasPermission(context)) runner.listen() else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(Unit) { askAgain() }
    DisposableEffect(Unit) { onDispose { runner.cancel() } }

    return SoundSearch(
        phase = runner.phase,
        level = runner.level,
        isRefused = runner.isRefused(),
        listenAgain = askAgain,
        openSettings = runner::openSettings
    )
}

// The title of each phase, what is happening or why the song was not found
internal fun soundTitleRes(phase: SoundPhase): Int = when (phase) {
    SoundPhase.Listening -> R.string.search_sound_listening
    SoundPhase.Finding -> R.string.search_sound_finding
    is SoundPhase.Failed -> when (phase.failure) {
        RecognitionFailure.NoMatch -> R.string.search_sound_failed
        RecognitionFailure.Offline -> R.string.search_sound_failed_offline
        RecognitionFailure.NoMicrophone -> R.string.search_sound_failed_microphone
    }
}

// How high the bars rise in each phase, with the sound while it listens, calm while it waits and flat when the song was not found
internal fun soundAmplitude(phase: SoundPhase, loudness: Float): Float = when (phase) {
    SoundPhase.Listening -> SearchDimens.SoundBarIdleAmplitude + (1f - SearchDimens.SoundBarIdleAmplitude) * loudness
    SoundPhase.Finding -> SearchDimens.SoundBarFindingAmplitude
    is SoundPhase.Failed -> 0f
}

// Turns the loudness of the microphone into what the bars follow, a little smoothed so they do not jump
@Composable
internal fun rememberLoudness(level: Float, millis: Int = WavvyMotion.ListeningLevelMillis): Float {
    val loudness by animateFloatAsState(
        targetValue = (level * SearchDimens.SoundLevelGain).coerceIn(0f, 1f),
        animationSpec = tween(millis, easing = LinearEasing),
        label = "loudness"
    )
    return loudness
}

// The turn of the wave of the bars, one lap each time it repeats
@Composable
internal fun rememberWave(millis: Int = WavvyMotion.ListeningWaveMillis): Float {
    val transition = rememberInfiniteTransition(label = "wave")
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(millis, easing = LinearEasing), RepeatMode.Restart),
        label = "wave"
    )
    return wave
}

// The bars of the sound, five of them with the middle one the tallest, they rise and fall in a wave
internal fun DrawScope.drawSoundBars(color: Color, amplitude: Float, wave: Float, barWidth: Dp, gap: Dp, min: Dp, max: Dp) {
    val count = SearchDimens.SoundBarEnvelope.size
    val width = barWidth.toPx()
    val space = gap.toPx()
    val low = min.toPx()
    val high = max.toPx()
    val startX = (size.width - (count * width + (count - 1) * space)) / 2f

    SearchDimens.SoundBarEnvelope.forEachIndexed { index, envelope ->
        val swing = (sin(wave + index * SearchDimens.SoundBarPhaseStep) + 1f) / 2f
        val height = low + (high - low) * envelope * amplitude * (1f - SearchDimens.SoundBarWaveShare + SearchDimens.SoundBarWaveShare * swing)
        drawRoundRect(
            color = color,
            topLeft = Offset(startX + index * (width + space), (size.height - height) / 2f),
            size = Size(width, height),
            cornerRadius = CornerRadius(width / 2f)
        )
    }
}
