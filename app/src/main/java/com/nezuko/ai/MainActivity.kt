package com.nezuko.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nezuko.ai.ai.*
import com.nezuko.ai.data.*
import com.nezuko.ai.phone.AndroidActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val Pink = Color(0xFFFF75B5)
private val Purple = Color(0xFF9B87F5)
private val Bg = Color(0xFF101017)
private val Panel = Color(0xFF1C1B27)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Pink, secondary = Purple, background = Bg, surface = Panel)) { NezukoApp() } }
    }
    override fun onInit(status: Int) { ttsReady = status == TextToSpeech.SUCCESS }
    fun speak(text: String) { if (ttsReady) tts?.speak(text.take(700), TextToSpeech.QUEUE_FLUSH, null, "nezuko-reply") }
    override fun onDestroy() { tts?.stop(); tts?.shutdown(); tts = null; super.onDestroy() }
}

@Composable
private fun NezukoApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { NezukoDatabase.get(context).dao() }
    val settings = remember { ProviderSettings(context) }
    val provider = remember { HttpAIProvider() }
    val actions = remember { AndroidActions(context) }
    val messages by dao.messages().collectAsState(initial = emptyList())
    val memories by dao.memories().collectAsState(initial = emptyList())
    val tasks by dao.tasks().collectAsState(initial = emptyList())
    var tab by remember { mutableStateOf("Voice") }
    var input by remember { mutableStateOf("") }
    var config by remember { mutableStateOf(ProviderConfig(ProviderKind.GEMINI, "", "gemini-2.0-flash")) }
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("gemini-2.0-flash") }
    var providerKind by remember { mutableStateOf(ProviderKind.GEMINI) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready when you are.") }
    var error by remember { mutableStateOf<String?>(null) }
    var memoryInput by remember { mutableStateOf("") }
    var taskInput by remember { mutableStateOf("") }
    var pendingCall by remember { mutableStateOf<String?>(null) }
    var voiceText by remember { mutableStateOf("") }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecognition(context) { voiceText = it; input = it } else status = "Microphone permission denied. You can still type."
    }
    LaunchedEffect(Unit) { config = settings.load(); apiKey = config.apiKey; model = config.model; providerKind = config.kind }

    Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).background(Brush.linearGradient(listOf(Pink, Purple)), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("NEZUKO AI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                    Text(status, style = MaterialTheme.typography.labelMedium, color = Pink)
                }
                IconButton(onClick = { tab = "Settings" }) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Voice", "Chat", "Memory", "Tasks", "Settings").forEach { label ->
                    FilterChip(selected = tab == label, onClick = { tab = label }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(12.dp))
            when (tab) {
                "Voice" -> {
                    val transition = rememberInfiniteTransition(label = "orb")
                    val scale by transition.animateFloat(1f, 1.07f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "scale")
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(188.dp).scale(scale).background(Brush.radialGradient(listOf(Pink.copy(alpha=.8f), Purple.copy(alpha=.35f), Bg)), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.GraphicEq, contentDescription = "Nezuko voice", tint = Color.White, modifier = Modifier.size(72.dp))
                            }
                            Spacer(Modifier.height(22.dp))
                            Text("I'm listening when you tap.", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text("Push-to-talk · Private by default", color = Color.LightGray)
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    startRecognition(context) { voiceText = it; input = it }
                                } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }, enabled = !busy) {
                                Icon(Icons.Default.Mic, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Push to talk")
                            }
                            if (voiceText.isNotBlank()) Text("Heard: $voiceText", color = Pink, modifier = Modifier.padding(12.dp))
                        }
                    }
                    Composer(input, { input = it }, busy) { sendMessage(input, context, settings, provider, dao, memories.map { it.content }, { busy = it }, { status = it }, { error = it }, { (context as? MainActivity)?.speak(it) }, { input = "" }) }
                }
                "Chat" -> {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(messages) { msg ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalAlignment = if (msg.role == "user") Alignment.End else Alignment.Start) {
                                Text(if (msg.role == "user") "YOU" else "NEZUKO", color = Pink, style = MaterialTheme.typography.labelSmall)
                                Surface(color = if (msg.role == "user") Color(0xFF34243B) else Panel, shape = RoundedCornerShape(16.dp)) {
                                    Text(msg.content, color = Color.White, modifier = Modifier.padding(12.dp))
                                }
                            }
                        }
                    }
                    Composer(input, { input = it }, busy) { sendMessage(input, context, settings, provider, dao, memories.map { it.content }, { busy = it }, { status = it }, { error = it }, { (context as? MainActivity)?.speak(it) }, { input = "" }) }
                    TextButton(onClick = { scope.launch { dao.clearMessages() } }) { Text("Clear conversation") }
                }
                "Memory" -> {
                    OutlinedTextField(memoryInput, { memoryInput = it }, label = { Text("Save a memory you approve") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { if (memoryInput.isNotBlank()) scope.launch { dao.addMemory(MemoryEntity(content = memoryInput.trim())); memoryInput = "" } }) { Text("Save memory") }
                    LazyColumn(Modifier.weight(1f)) {
                        items(memories) { memory ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(memory.content, color = Color.White, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { scope.launch { dao.deleteMemory(memory.id) } }) { Icon(Icons.Default.Delete, contentDescription = "Delete memory", tint = Pink) }
                                }
                            }
                        }
                    }
                    TextButton(onClick = { scope.launch { dao.clearMemories() } }) { Text("Clear all memories") }
                }
                "Tasks" -> {
                    OutlinedTextField(taskInput, { taskInput = it }, label = { Text("Task or reminder title") }, modifier = Modifier.fillMaxWidth())
                    Text("Tasks can be saved here. Timed notifications require selecting a date/time in a future update.", color = Color.LightGray)
                    Button(onClick = { if (taskInput.isNotBlank()) scope.launch { dao.addTask(TaskEntity(title = taskInput.trim(), dueAt = null)); taskInput = "" } }) { Text("Add task") }
                    LazyColumn(Modifier.weight(1f)) {
                        items(tasks) { task ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = task.completed, onCheckedChange = { scope.launch { dao.updateTask(task.copy(completed = it)) } })
                                    Text(task.title, color = Color.White, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { scope.launch { dao.deleteTask(task.id) } }) { Icon(Icons.Default.Delete, contentDescription = "Delete task") }
                                }
                            }
                        }
                    }
                }
                else -> {
                    Text("AI Provider", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = providerKind == ProviderKind.GEMINI, onClick = { providerKind = ProviderKind.GEMINI; model = "gemini-2.0-flash" }, label = { Text("Gemini") })
                        FilterChip(selected = providerKind == ProviderKind.OPENROUTER, onClick = { providerKind = ProviderKind.OPENROUTER; model = "openrouter/free" }, label = { Text("OpenRouter") })
                    }
                    OutlinedTextField(model, { model = it }, label = { Text("Model identifier") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(apiKey, { apiKey = it }, label = { Text("API key") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                    Text("Keys are encrypted with Android Keystore before local storage. Conversation text is sent to your selected provider when you chat.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                val cfg = ProviderConfig(providerKind, apiKey.trim(), model.trim())
                                try { settings.save(cfg); config = cfg; status = "Provider settings saved"; error = null }
                                catch (e: Exception) { error = "Could not save settings: ${e.message}" }
                            }
                        }) { Text("Save") }
                        OutlinedButton(onClick = {
                            scope.launch {
                                busy = true; error = null
                                try {
                                    val cfg = ProviderConfig(providerKind, apiKey.trim(), model.trim())
                                    withContext(Dispatchers.IO) { provider.test(cfg) }
                                    settings.save(cfg); config = cfg; status = "Connection test succeeded"
                                } catch (e: Exception) { error = e.message ?: "Connection test failed" }
                                finally { busy = false }
                            }
                        }, enabled = !busy) { Text(if (busy) "Testing…" else "Test connection") }
                    }
                    OutlinedButton(onClick = { scope.launch { settings.clearKey(); apiKey = ""; config = config.copy(apiKey = ""); status = "API key deleted" } }) { Text("Delete saved key") }
                    Text("Quick actions", color = Pink, style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { actions.openSettings().onFailure { error = it.message } }) { Text("Open Android Settings") }
                    OutlinedButton(onClick = { pendingCall = "https://www.google.com" }) { Text("Open website (confirmation)") }
                }
            }
            error?.let { msg ->
                Card(Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF47242F))) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, color = Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = { error = null }) { Icon(Icons.Default.Close, contentDescription = "Dismiss") }
                    }
                }
            }
        }
    }
    pendingCall?.let { url ->
        AlertDialog(onDismissRequest = { pendingCall = null }, title = { Text("Open website?") }, text = { Text(url) },
            confirmButton = { TextButton(onClick = { actions.openHttpsUrl(url).onFailure { error = it.message }; pendingCall = null }) { Text("Open") } },
            dismissButton = { TextButton(onClick = { pendingCall = null }) { Text("Cancel") } })
    }
}

@Composable
private fun Composer(value: String, onValue: (String) -> Unit, busy: Boolean, onSend: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value, onValue, modifier = Modifier.weight(1f), placeholder = { Text("Ask Nezuko anything…") }, maxLines = 4)
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onSend, enabled = value.isNotBlank() && !busy) {
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.Send, contentDescription = "Send", tint = Pink)
        }
    }
}

private fun sendMessage(
    prompt: String, context: android.content.Context, settings: ProviderSettings, provider: AIProvider, dao: NezukoDao,
    memories: List<String>, setBusy: (Boolean) -> Unit, setStatus: (String) -> Unit, setError: (String?) -> Unit,
    speak: (String) -> Unit, clearInput: () -> Unit
) {
    if (prompt.isBlank()) return
    // A model is not allowed to claim device actions. This initial implementation keeps tool execution separate.
    val cleaned = prompt.trim()
    clearInput()
    setBusy(true); setStatus("Thinking…"); setError(null)
    kotlinx.coroutines.MainScope().launch {
        try {
            dao.addMessage(MessageEntity(role = "user", content = cleaned))
            val config = settings.load()
            val history = dao.messages().let { flow -> kotlinx.coroutines.flow.first(flow).takeLast(12).map { it.role to it.content } }
            val memoryContext = if (memories.isEmpty()) "" else "\nUser-approved memories (use only if relevant):\n" + memories.take(10).joinToString("\n") { "- $it" }
            val response = withContext(Dispatchers.IO) {
                provider.generate(config, cleaned + memoryContext, history.filter { it.first != "user" || it.second != cleaned })
            }
            dao.addMessage(MessageEntity(role = "assistant", content = response))
            setStatus("Ready"); speak(response)
        } catch (e: Exception) {
            setStatus("Needs attention")
            setError(e.message ?: "Request failed. Check provider settings and internet.")
        } finally { setBusy(false) }
    }
}

private fun startRecognition(context: android.content.Context, onResult: (String) -> Unit) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) return
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
    }
    // Android speech UI manages recognition lifecycle and releases microphone when finished.
    runCatching { context.startActivity(Intent.createChooser(intent, "Speak to Nezuko AI")) }
}
