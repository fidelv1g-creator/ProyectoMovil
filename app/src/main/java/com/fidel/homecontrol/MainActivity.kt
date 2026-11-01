package com.fidel.homecontrol

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF0B0D12)
private val Surface = Color(0xFF171B24)
private val Accent = Color(0xFF8B9CFF)
private val Muted = Color(0xFF9299A8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Background,
                    surface = Surface,
                    primary = Accent
                )
            ) {
                HomeScreen(
                    onNotConnected = {
                        Toast.makeText(
                            this,
                            "La conexión con Google Home aún no está configurada",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(onNotConnected: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text("HOME CONTROL", color = Color.White, fontSize = 23.sp)
            Spacer(Modifier.height(5.dp))
            Text("Tus dispositivos, a un toque", color = Muted, fontSize = 14.sp)
            Spacer(Modifier.height(30.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = Accent)
                Spacer(Modifier.width(10.dp))
                Text("SALÓN", color = Color.White, fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DeviceButton(
                    modifier = Modifier.weight(1f),
                    title = "ENCENDER\nTELE",
                    subtitle = "Encender",
                    onClick = onNotConnected
                )
                DeviceButton(
                    modifier = Modifier.weight(1f),
                    title = "APAGAR\nTELE",
                    subtitle = "Apagar",
                    onClick = onNotConnected
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "2 controles · Más dispositivos próximamente",
                color = Muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun DeviceButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(142.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                Icons.Default.PowerSettingsNew,
                contentDescription = subtitle,
                tint = Accent,
                modifier = Modifier.size(25.dp)
            )
            Column {
                Text(title, color = Color.White, fontSize = 14.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Muted, fontSize = 11.sp)
            }
        }
    }
}
