package com.fidel.homecontrol

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

private val Background = Color(0xFF0B0D12)
private val Surface = Color(0xFF171B24)
private val Accent = Color(0xFF8B9CFF)
private val Muted = Color(0xFF9299A8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("home_control", Context.MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Background, surface = Surface, primary = Accent)) {
                HomeScreen(
                    initialIp = prefs.getString("tv_ip", "") ?: "",
                    initialMac = prefs.getString("tv_mac", "") ?: "",
                    paired = !prefs.getString("tv_client_key", null).isNullOrBlank(),
                    onSave = { ip, mac ->
                        prefs.edit().putString("tv_ip", ip.trim()).putString("tv_mac", mac.trim()).apply()
                    },
                    onPair = { ip ->
                        val key = WebOsTv.pair(ip, prefs.getString("tv_client_key", null))
                        prefs.edit().putString("tv_client_key", key).apply()
                    },
                    onPowerOff = { ip ->
                        WebOsTv.command(ip, prefs.getString("tv_client_key", null) ?: "", "ssap://system/turnOff")
                    },
                    onPowerOn = { mac ->
                        WebOsTv.wake(mac)
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    initialIp: String,
    initialMac: String,
    paired: Boolean,
    onSave: (String, String) -> Unit,
    onPair: suspend (String) -> Unit,
    onPowerOff: suspend (String) -> Unit,
    onPowerOn: suspend (String) -> Unit
) {
    var ip by remember { mutableStateOf(initialIp) }
    var mac by remember { mutableStateOf(initialMac) }
    var isPaired by remember { mutableStateOf(paired) }
    var status by remember { mutableStateOf("Configura tu televisor LG para empezar") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runAction(action: suspend () -> Unit, success: String) {
        if (busy) return
        scope.launch {
            busy = true
            status = "Conectando…"
            try {
                action()
                status = success
            } catch (e: Exception) {
                status = "Error: ${e.message ?: "no se pudo completar la operación"}"
            } finally {
                busy = false
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = Background) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("HOME CONTROL", color = Color.White, fontSize = 23.sp)
            Text("Control local de tus dispositivos", color = Muted, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = Accent)
                Spacer(Modifier.width(10.dp))
                Text("SALÓN · TELEVISOR LG", color = Color.White, fontSize = 16.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DeviceButton(Modifier.weight(1f), "ENCENDER\nTELE", "Wake-on-LAN", !busy) {
                    runAction({ onSave(ip, mac); onPowerOn(mac) }, "Orden de encendido enviada")
                }
                DeviceButton(Modifier.weight(1f), "APAGAR\nTELE", "webOS", !busy) {
                    runAction({ onSave(ip, mac); onPowerOff(ip) }, "Orden de apagado enviada")
                }
            }
            Text("CONFIGURACIÓN DEL TELEVISOR", color = Accent, fontSize = 12.sp)
            OutlinedTextField(
                value = ip, onValueChange = { ip = it },
                label = { Text("Dirección IP del televisor") },
                placeholder = { Text("Ej. 192.168.1.50") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = mac, onValueChange = { mac = it },
                label = { Text("Dirección MAC (para encender)") },
                placeholder = { Text("Ej. AA:BB:CC:DD:EE:FF") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    onSave(ip, mac)
                    runAction({ onPair(ip); isPaired = true }, "Televisor vinculado. Acepta la solicitud en la pantalla de la TV si aparece.")
                },
                enabled = !busy && ip.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isPaired) "VOLVER A VINCULAR TV" else "VINCULAR TELEVISOR") }
            Text(if (isPaired) "Conexión webOS autorizada en este móvil" else "Aún no se ha autorizado la conexión webOS", color = Muted, fontSize = 12.sp)
            Text(status, color = if (status.startsWith("Error")) Color(0xFFFF8A80) else Muted, fontSize = 12.sp)
            Text(
                "Nota: el encendido requiere que el televisor admita Wake-on-LAN y que esté activado el inicio por red. La orden enviada no confirma por sí sola que la TV haya arrancado.",
                color = Muted, fontSize = 11.sp, lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun DeviceButton(
    modifier: Modifier,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick, enabled = enabled, modifier = modifier.height(126.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface, disabledContainerColor = Surface.copy(alpha = 0.5f))
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Default.PowerSettingsNew, contentDescription = subtitle, tint = Accent, modifier = Modifier.size(25.dp))
            Column {
                Text(title, color = Color.White, fontSize = 14.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Muted, fontSize = 11.sp)
            }
        }
    }
}

private object WebOsTv {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    suspend fun pair(ip: String, existingKey: String?): String = withTimeout(120_000) {
        websocket(ip, existingKey, register = true, uri = null)
    }

    suspend fun command(ip: String, key: String, uri: String) {
        withTimeout(15_000) { websocket(ip, key, register = false, uri = uri) }
    }

    private suspend fun websocket(ip: String, key: String?, register: Boolean, uri: String?): String =
        suspendCancellableCoroutine { continuation ->
            val request = Request.Builder().url("ws://${ip.trim()}:3000").build()
            var socket: WebSocket? = null
            var finished = false
            fun fail(message: String) {
                if (!finished && continuation.isActive) {
                    finished = true
                    continuation.resumeWithException(IllegalStateException(message))
                    socket?.close(1000, "done")
                }
            }
            socket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val payload = if (register) {
                        JSONObject().put("type", "register").put("id", "home-control-register")
                            .put("payload", JSONObject()
                                .put("pairingType", "PROMPT")
                                .put("manifest", JSONObject()
                                    .put("manifestVersion", 1)
                                    .put("appVersion", "1.0")
                                    .put("signed", JSONObject().put("created", "2026-11-01").put("appId", "com.fidel.homecontrol").put("vendorId", "fidel"))
                                    .put("permissions", org.json.JSONArray(listOf(
                                        "LAUNCH", "LAUNCH_WEBAPP", "APP_TO_APP", "CONTROL_AUDIO",
                                        "CONTROL_POWER", "READ_RUNNING_APPS", "READ_TV_CURRENT_TIME"
                                    )))
                                    .put("localizedAppNames", JSONObject().put("", "Home Control"))
                                    .put("localizedVendorNames", JSONObject().put("", "Home Control"))
                                    .put("serial", "home-control-android")
                                )
                            ).toString()
                    } else {
                        JSONObject().put("type", "request").put("id", "home-control-power")
                            .put("uri", uri).put("payload", JSONObject()).toString()
                    }
                    if (register) {
                        webSocket.send(payload)
                    } else {
                        if (key.isNullOrBlank()) {
                            fail("Primero debes vincular el televisor")
                            return
                        }
                        webSocket.send(JSONObject().put("type", "register").put("payload",
                            JSONObject().put("client-key", key).put("manifest", JSONObject()
                                .put("manifestVersion", 1).put("appVersion", "1.0")
                                .put("signed", JSONObject().put("appId", "com.fidel.homecontrol").put("vendorId", "fidel"))
                                .put("permissions", org.json.JSONArray(listOf("CONTROL_POWER")))
                            )).toString())
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val clientKey = json.optJSONObject("payload")?.optString("client-key")
                        if (!register && json.optString("type") == "registered") {
                            webSocket.send(JSONObject().put("type", "request").put("id", "home-control-power")
                                .put("uri", uri).put("payload", JSONObject()).toString())
                        } else if (register && !clientKey.isNullOrBlank()) {
                            finished = true
                            if (continuation.isActive) continuation.resume(clientKey)
                            webSocket.close(1000, "paired")
                        } else if (!register && json.optString("type") == "response") {
                            if (json.optBoolean("returnValue", false)) {
                                finished = true
                                if (continuation.isActive) continuation.resume(key ?: "")
                                webSocket.close(1000, "command sent")
                            } else if (json.has("error")) fail(json.optString("error"))
                        } else if (json.optString("type") == "error") {
                            fail(json.optString("error", "Error devuelto por webOS"))
                        }
                    } catch (e: Exception) { fail(e.message ?: "Respuesta no válida del televisor") }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    fail(t.message ?: "No se pudo conectar con el televisor")
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    if (!finished) fail("La conexión se cerró antes de completar la operación")
                }
            })
            continuation.invokeOnCancellation { socket?.cancel() }
        }

    suspend fun wake(mac: String) {
        val bytes = mac.split(Regex("[:-]")).map { it.toInt(16).toByte() }
        require(bytes.size == 6) { "La dirección MAC debe tener 6 pares hexadecimales" }
        val packet = ByteArray(102)
        for (i in 0 until 6) packet[i] = 0xFF.toByte()
        for (i in 0 until 16) System.arraycopy(bytes.toByteArray(), 0, packet, 6 + i * 6, 6)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.send(DatagramPacket(packet, packet.size, InetAddress.getByName("255.255.255.255"), 9))
            }
        }
    }
}
