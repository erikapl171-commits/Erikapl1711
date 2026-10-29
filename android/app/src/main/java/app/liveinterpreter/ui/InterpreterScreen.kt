package app.liveinterpreter.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.Build
import androidx.compose.runtime.DisposableEffect
import app.liveinterpreter.capture.MicrophoneService
import app.liveinterpreter.capture.SessionRepository
import app.liveinterpreter.recognition.SpeechModelInstaller
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.liveinterpreter.core.DemoPhase
import app.liveinterpreter.core.InterpreterUiState
import app.liveinterpreter.core.MicPermission
import app.liveinterpreter.core.SourceLanguage
import app.liveinterpreter.core.SubtitleSize
import app.liveinterpreter.speech.EnglishSpeaker

private val Ink = Color(0xFF121B20)
private val Mint = Color(0xFF54CA9B)
private val InkSurface = Color(0xFF1C292B)
private val SubtleInk = Color(0xFF9FAFB2)

@Composable
fun InterpreterScreen(model: InterpreterViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    var settingsVisible by rememberSaveable { mutableStateOf(false) }
    var speechStatus by remember { mutableStateOf("Audio off") }
    var playbackSpeed by rememberSaveable { mutableStateOf(1.0f) }
    var chosenVoice by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    var captureStatus by remember { mutableStateOf("Stopped") }
    var capturedSamples by remember { mutableStateOf(0L) }
    var liveTranscript by remember { mutableStateOf("") }
    var translatedSegments by remember { mutableStateOf(listOf<String>()) }
    var translationStatus by remember { mutableStateOf("Waiting for microphone") }
    var lastTranslationSequence by remember { mutableStateOf(0L) }
    var modelPresent by remember { mutableStateOf(SpeechModelInstaller.isInstalled(context)) }
    var downloadingModel by remember { mutableStateOf(false) }
    var modelProgress by remember { mutableStateOf(0) }
    var modelMessage by remember { mutableStateOf("") }
    val modelScope = rememberCoroutineScope()
    val speaker = remember(context) { EnglishSpeaker(context) { speechStatus = it } }
    DisposableEffect(speaker) { onDispose { speaker.close() } }
    LaunchedEffect(state.audioEnabled, speaker) { speaker.setEnabled(state.audioEnabled) }
    LaunchedEffect(playbackSpeed, speaker) { speaker.setSpeed(playbackSpeed) }
    LaunchedEffect(chosenVoice, speaker) {
        if (chosenVoice.isNotEmpty() && !speaker.chooseVoice(chosenVoice)) {
            speechStatus = "Selected offline voice unavailable"
        }
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, intent: Intent?) {
                if (intent?.action == MicrophoneService.ACTION_TRANSCRIPT) {
                    val text = intent.getStringExtra(MicrophoneService.EXTRA_TRANSCRIPT).orEmpty()
                    if (text.isNotBlank()) liveTranscript = (liveTranscript + " " + text).trim().takeLast(5000)
                } else if (intent?.action == MicrophoneService.ACTION_TRANSLATION) {
                    val sequence = intent.getLongExtra(MicrophoneService.EXTRA_SEQUENCE, 0L)
                    val english = intent.getStringExtra(MicrophoneService.EXTRA_ENGLISH).orEmpty()
                    if (sequence > lastTranslationSequence && english.isNotBlank()) {
                        lastTranslationSequence = sequence
                        translatedSegments = (translatedSegments + english).takeLast(100)
                        speaker.submit(english)
                    }
                } else if (intent?.action == MicrophoneService.ACTION_TRANSLATION_STATE) {
                    translationStatus = intent.getStringExtra(MicrophoneService.EXTRA_STATUS).orEmpty()
                } else {
                    captureStatus = intent?.getStringExtra(MicrophoneService.EXTRA_STATUS) ?: "Unknown"
                    capturedSamples = intent?.getLongExtra(MicrophoneService.EXTRA_SAMPLES, 0L) ?: 0L
                }
            }
        }
        val filter = IntentFilter(MicrophoneService.ACTION_STATUS)
        filter.addAction(MicrophoneService.ACTION_TRANSCRIPT)
        filter.addAction(MicrophoneService.ACTION_TRANSLATION)
        filter.addAction(MicrophoneService.ACTION_TRANSLATION_STATE)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        model.setMicPermission(if (granted) MicPermission.GRANTED else MicPermission.DENIED)
    }
    LaunchedEffect(Unit) {
        SessionRepository.snapshot().let { snapshot ->
            captureStatus = snapshot.status
            capturedSamples = snapshot.samples
            liveTranscript = snapshot.source
            translatedSegments = snapshot.english
            lastTranslationSequence = snapshot.sequence
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            model.setMicPermission(MicPermission.GRANTED)
        }
    }

    val dark = isSystemInDarkTheme()
    val palette = if (dark) darkColorScheme(
        primary = Mint, background = Ink, surface = InkSurface,
        onSurface = Color(0xFFF1F8F5), onBackground = Color(0xFFF1F8F5)
    ) else lightColorScheme(
        primary = Color(0xFF096E51), background = Color(0xFFF6F8F6),
        surface = Color.White, onSurface = Ink, onBackground = Ink
    )

    MaterialTheme(colorScheme = palette) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { safePadding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(safePadding).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .13f)) {
                        Icon(Icons.Outlined.MicNone, null,
                            modifier = Modifier.padding(11.dp).size(25.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Live Interpreter", fontWeight = FontWeight.Bold, fontSize = 21.sp)
                        Text("Your personal live subtitles", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                    }
                    IconButton(onClick = { settingsVisible = true }, modifier = Modifier.testTag("open_settings")) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }

                DemoNotice()
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Speech model: " + if (modelPresent) "Installed and verified" else "Not installed", fontWeight = FontWeight.SemiBold)
                        Text("Multilingual Whisper base model, approximately 148 MB. Download over Wi-Fi to avoid mobile data charges. Keep this screen open until installation completes.", fontSize = 12.sp)
                        if (!modelPresent) {
                            Spacer(Modifier.height(8.dp))
                            Button(enabled = !downloadingModel, onClick = {
                                modelScope.launch {
                                    downloadingModel = true
                                    modelMessage = "Downloading model..."
                                    try {
                                        SpeechModelInstaller.install(context) { modelProgress = it }
                                        modelPresent = SpeechModelInstaller.isInstalled(context)
                                        modelMessage = if (modelPresent) "Model installed. Start a new microphone session." else "Verification failed"
                                    } catch (e: Exception) {
                                        modelMessage = "Download failed: ${e.message ?: "connection error"}"
                                    } finally { downloadingModel = false }
                                }
                            }) { Text(if (downloadingModel) "Downloading $modelProgress%" else "Download speech model") }
                        }
                        if (modelMessage.isNotBlank()) Text(modelMessage, fontSize = 12.sp)
                    }
                }
                OutlinedButton(onClick = {
                    val english = translatedSegments.joinToString("\n")
                    if (english.isNotBlank()) {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Live Interpreter transcript")
                            putExtra(Intent.EXTRA_TEXT, english)
                        }
                        context.startActivity(Intent.createChooser(share, "Export English transcript"))
                    }
                }, enabled = translatedSegments.isNotEmpty(), modifier = Modifier.fillMaxWidth().testTag("export_transcript")) {
                    Text("Export English transcript")
                }
                Button(onClick = {
                    val isCapturing = captureStatus.startsWith("Capturing") || captureStatus == "Starting microphone"
                    if (isCapturing) {
                        speaker.reset()
                        context.startService(Intent(context, MicrophoneService::class.java)
                            .setAction(MicrophoneService.ACTION_STOP))
                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                        captureStatus = "Starting microphone"
                        speaker.reset()
                        liveTranscript = ""
                        translatedSegments = emptyList()
                        lastTranslationSequence = 0L
                        translationStatus = "Preparing translation"
                        try {
                            ContextCompat.startForegroundService(context,
                                Intent(context, MicrophoneService::class.java)
                                    .setAction(MicrophoneService.ACTION_START)
                                    .putExtra(MicrophoneService.EXTRA_LANGUAGE, state.language.code))
                        } catch (e: RuntimeException) {
                            captureStatus = "Start failed: ${e.javaClass.simpleName}"
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }, modifier = Modifier.fillMaxWidth().testTag("microphone_toggle")) {
                    Text(if (captureStatus.startsWith("Capturing") || captureStatus == "Starting microphone")
                        "Stop real microphone" else "Start real microphone")
                }
                Text("Microphone: $captureStatus; captured ${capturedSamples / 16000} seconds. " +
                    "No recording is saved. Recognition requires a local multilingual model. English translation downloads its model on Wi-Fi the first time.", fontSize = 12.sp)

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("LIVE ORIGINAL-LANGUAGE TRANSCRIPT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(if (liveTranscript.isBlank()) "No recognized speech yet. Install the local model to enable ASR." else liveTranscript,
                            modifier = Modifier.testTag("real_transcript"))
                    }
                }
                Card(modifier = Modifier.fillMaxWidth().testTag("english_translation_panel")) {
                    Column(Modifier.padding(16.dp)) {
                        Text("LIVE ENGLISH TRANSLATION", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(translationStatus, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                            modifier = Modifier.testTag("translation_status"))
                        Spacer(Modifier.height(8.dp))
                        Text(if (translatedSegments.isEmpty()) "English captions appear here after local speech recognition and translation are ready."
                            else translatedSegments.joinToString(" "),
                            modifier = Modifier.testTag("translated_caption"), fontSize = 20.sp)
                        if (liveTranscript.isNotBlank() && translationStatus.startsWith("Translating")) {
                            Text("New speech awaiting translation…", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f),
                                modifier = Modifier.testTag("translation_provisional"))
                        }
                    }
                }
                Text("Spoken English: $speechStatus. Offline voices depend on Android voice data.", fontSize = 12.sp)
                SourceRow(language = state.language, onSelectLanguage = model::setLanguage)
                StatusRow(state)

                CaptionPanel(state = state)

                Button(
                    onClick = {
                        if (state.phase == DemoPhase.RUNNING) model.stopDemo() else model.startDemo()
                    },
                    modifier = Modifier.fillMaxWidth().height(58.dp).testTag("demo_toggle"),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        if (state.phase == DemoPhase.RUNNING) Icons.Outlined.StopCircle else Icons.Outlined.MicNone,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(if (state.phase == DemoPhase.RUNNING) "Stop sample" else "Play sample captions",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                        modifier = Modifier.size(17.dp))
                    Text(
                        "Real transcription requires locally installed whisper.cpp and model. " +
                            "Sample captions below are fictional and never spoken. Spoken English only uses real translated segments.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f),
                        modifier = Modifier.testTag("demo_disclosure")
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        if (settingsVisible) {
            SettingsSheet(
                state = state,
                onDismiss = { settingsVisible = false },
                requestMicrophonePermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                changeAudio = { enabled -> model.setAudioEnabled(enabled); speaker.setEnabled(enabled) },
                playbackSpeed = playbackSpeed,
                changePlaybackSpeed = { playbackSpeed = it },
                availableVoices = speaker.availableVoices().map { it.name },
                chosenVoice = chosenVoice,
                changeVoice = { chosenVoice = it },
                changeScreenAwake = model::setKeepScreenOn,
                changeSubtitleSize = model::setSubtitleSize
            )
        }
    }
}

@Composable
private fun DemoNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("demo_banner"),
        color = MaterialTheme.colorScheme.primary.copy(alpha = .11f),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Column {
                Text("STAGE 6 · SOURCE INTEGRATION", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text("Real captions require Whisper models. Sample captions are separate.", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun SourceRow(language: SourceLanguage, onSelectLanguage: (SourceLanguage) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("LANGUAGES", fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("source_language"),
                    shape = RoundedCornerShape(13.dp)) {
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                        Text("From", fontSize = 10.sp, lineHeight = 13.sp)
                        Text(language.displayName, fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    SourceLanguage.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = { onSelectLanguage(option); expanded = false }
                        )
                    }
                }
            }
            Surface(modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text("To", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                    Text("English", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StatusRow(state: InterpreterUiState) {
    val headline = when (state.phase) {
        DemoPhase.READY -> "Ready for sample"
        DemoPhase.RUNNING -> "Showing example subtitles"
        DemoPhase.COMPLETE -> "Sample complete"
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(Icons.Outlined.CheckCircleOutline, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp))
            Text(headline, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("demo_status"))
        }
        Text("MIC OFF", fontSize = 10.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
    }
}

@Composable
private fun CaptionPanel(state: InterpreterUiState) {
    val scrollState = rememberScrollState()
    LaunchedEffect(state.committed.size, state.provisional) {
        if (!scrollState.isScrollInProgress) scrollState.animateScrollTo(scrollState.maxValue)
    }
    val captionFont = when (state.subtitleSize) {
        SubtitleSize.NORMAL -> 20.sp
        SubtitleSize.LARGE -> 24.sp
        SubtitleSize.EXTRA_LARGE -> 29.sp
    }
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ENGLISH SUBTITLES", fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .54f))
                Text("EXAMPLE", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(13.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .09f))
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth().height(235.dp).verticalScroll(scrollState)
                    .testTag("captions"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!state.hasAnyCaptions) {
                    Spacer(Modifier.height(42.dp))
                    Text("Your subtitles will appear here.", fontSize = captionFont,
                        lineHeight = captionFont * 1.3f,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .42f))
                    Text("Play the sample to preview the layout.", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .54f))
                }
                state.committed.forEach { caption ->
                    Text(caption.english, fontSize = captionFont, lineHeight = captionFont * 1.35f,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("confirmed_caption_${caption.id}"))
                }
                state.provisional?.let { draft ->
                    Text(draft, fontSize = captionFont, lineHeight = captionFont * 1.35f,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f),
                        modifier = Modifier.testTag("provisional_caption"))
                }
                if (state.phase == DemoPhase.COMPLETE) {
                    Text("End of example", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f),
                        fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    state: InterpreterUiState,
    onDismiss: () -> Unit,
    requestMicrophonePermission: () -> Unit,
    changeAudio: (Boolean) -> Unit,
    playbackSpeed: Float,
    changePlaybackSpeed: (Float) -> Unit,
    availableVoices: List<String>,
    chosenVoice: String,
    changeVoice: (String) -> Unit,
    changeScreenAwake: (Boolean) -> Unit,
    changeSubtitleSize: (SubtitleSize) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Preferences", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close settings") }
            }
            SettingToggle(
                title = "Spoken English", subtitle = "Speak only real English translations through device audio.",
                icon = { Icon(Icons.Outlined.Headphones, null) },
                enabled = state.audioEnabled, onChange = changeAudio
            )
            Text("Speaking speed: ${"%.2f".format(playbackSpeed)}×")
            androidx.compose.material3.Slider(value = playbackSpeed, onValueChange = changePlaybackSpeed,
                valueRange = 0.75f..1.5f, enabled = state.audioEnabled)
            Text("Offline English voice", fontWeight = FontWeight.SemiBold)
            if (availableVoices.isEmpty()) Text("No offline English voice listed. Install voice data in Android TTS settings.")
            else {
                availableVoices.take(12).forEach { voice ->
                    androidx.compose.material3.TextButton(onClick = { changeVoice(voice) }) {
                        Text(if (voice == chosenVoice) "Selected: $voice" else voice)
                    }
                }
            }
            HorizontalDivider()
            SettingToggle(
                title = "Keep screen awake",
                subtitle = "While sample captions are playing",
                icon = { Icon(Icons.Outlined.MicNone, null) },
                enabled = state.keepScreenOn, onChange = changeScreenAwake
            )
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.TextFields, null)
                    Text("Subtitle size", fontWeight = FontWeight.SemiBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    SubtitleSize.entries.forEach { size ->
                        val label = when (size) {
                            SubtitleSize.NORMAL -> "Normal"
                            SubtitleSize.LARGE -> "Large"
                            SubtitleSize.EXTRA_LARGE -> "Extra large"
                        }
                        if (size == state.subtitleSize) {
                            Button(onClick = { changeSubtitleSize(size) },
                                contentPadding = PaddingValues(horizontal = 11.dp),
                                modifier = Modifier.weight(1f)) {
                                Text(label, fontSize = 11.sp, maxLines = 1)
                            }
                        } else {
                            OutlinedButton(onClick = { changeSubtitleSize(size) },
                                contentPadding = PaddingValues(horizontal = 11.dp),
                                modifier = Modifier.weight(1f)) {
                                Text(label, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Microphone permission", fontWeight = FontWeight.SemiBold)
                val detail = when (state.microphonePermission) {
                    MicPermission.NOT_CHECKED -> "Not checked"
                    MicPermission.GRANTED -> "Allowed for future live listening"
                    MicPermission.DENIED -> "Not allowed. You can change this in Android settings."
                }
                Text(detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                OutlinedButton(onClick = requestMicrophonePermission,
                    modifier = Modifier.testTag("request_mic_permission")) {
                    Text("Check microphone access")
                }
                Text("Mic permission enables real capture when Start real microphone is pressed.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun SettingToggle(
    title: String, subtitle: String, icon: @Composable () -> Unit,
    enabled: Boolean, onChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        icon()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
        }
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}
