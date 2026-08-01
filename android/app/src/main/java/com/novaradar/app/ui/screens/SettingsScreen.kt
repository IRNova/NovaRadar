package com.novaradar.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaradar.app.ui.components.ParticleBackground
import com.novaradar.app.ui.components.Wc
import com.novaradar.app.ui.components.WidgetCard
import com.novaradar.app.ui.localization.Localization
import com.novaradar.app.ui.viewmodel.AppTheme
import com.novaradar.app.ui.viewmodel.NovaRadarViewModel

@Composable
fun SettingsScreen(viewModel: NovaRadarViewModel) {
    val theme by viewModel.selectedTheme.collectAsState()
    val isLight = theme == AppTheme.PRISM_LIGHT
    val ports by viewModel.portConfigs.collectAsState()
    val sources by viewModel.ipSources.collectAsState()

    val vibrateFinish by viewModel.vibrateOnFinish.collectAsState()
    val vibrateError by viewModel.vibrateOnError.collectAsState()
    val notifyError by viewModel.notifyOnError.collectAsState()

    var speedLimit by remember { mutableFloatStateOf(10f) }
    var maxPing by remember { mutableIntStateOf(9999) }

    Box(Modifier.fillMaxSize()) {
        ParticleBackground(isLight = isLight, modifier = Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Scanner Settings
            WidgetCard(isLightTheme = isLight) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Wc.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Radar, null, tint = Wc.primary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Scanner", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isLight) Color(0xFF1A202C) else Wc.onSurfaceDark)
                        Text("Ports, sources & speed", fontSize = 10.sp, color = if (isLight) Color(0xFF4A5568) else Wc.onSurfaceVariantDark)
                    }
                }
            }

            WidgetCard(isLightTheme = isLight) {
                Text("Target Ports", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Wc.primary)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Wc.primary.copy(alpha = 0.1f)).clickable { viewModel.selectAllPorts() }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Text("Select All", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Wc.primary)
                    }
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Wc.error.copy(alpha = 0.1f)).clickable { viewModel.clearAllPorts() }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Text("Clear All", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Wc.error)
                    }
                }
                Spacer(Modifier.height(4.dp))
                ports.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { pc ->
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (pc.isEnabled) Wc.primary else if (isLight) Color(0xFFE2E8F0) else Wc.surfaceContainerDark)
                                .border(0.5.dp, if (pc.isEnabled) Wc.primary.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .clickable { viewModel.togglePortConfig(pc) }
                                .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center) {
                                Text("${pc.port}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (pc.isEnabled) Color.White else if (isLight) Color(0xFF334155) else Color.Gray)
                            }
                        }
                    }
                }
            }

            WidgetCard(isLightTheme = isLight) {
                Text("IP Sources", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Wc.primary)
                Spacer(Modifier.height(4.dp))
                sources.forEach { src ->
                    Card(Modifier.fillMaxWidth().clickable { viewModel.toggleIpSource(src) }.padding(vertical = 1.dp), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = if (isLight) Color.White.copy(alpha = 0.6f) else Wc.surfaceContainerDark.copy(alpha = 0.7f)), border = BorderStroke(0.5.dp, if (src.isEnabled) Wc.primary.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.1f))) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = src.isEnabled, onCheckedChange = { viewModel.toggleIpSource(src) }, modifier = Modifier.height(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(src.nameEn, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isLight) Color(0xFF1A202C) else Wc.onSurfaceDark)
                            Icon(Icons.Default.Language, null, tint = if (src.isEnabled) Wc.primary else Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            WidgetCard(isLightTheme = isLight) {
                Text("Speed Limit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Wc.primary)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Max display speed:", fontSize = 10.sp, color = if (isLight) Color(0xFF4A5568) else Wc.onSurfaceVariantDark)
                    Text("%.0f MB/s".format(speedLimit), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Wc.primary)
                }
                Slider(value = speedLimit, onValueChange = { speedLimit = it }, valueRange = 1f..20f, steps = 18, colors = SliderDefaults.colors(thumbColor = Wc.primary, activeTrackColor = Wc.primary))
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

            // Section 3: General Settings
            WidgetCard(isLightTheme = isLight) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF8B5CF6).copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Settings, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                Column {
                    Text("General", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isLight) Color(0xFF1A202C) else Wc.onSurfaceDark)
                    Text("Theme & alerts", fontSize = 10.sp, color = if (isLight) Color(0xFF4A5568) else Wc.onSurfaceVariantDark)
                }
                }
            }

            WidgetCard(isLightTheme = isLight) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Theme", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Wc.primary)
                            Text(if (theme == AppTheme.PRISM_DARK) "Dark" else "Light", fontSize = 10.sp, color = if (isLight) Color(0xFF4A5568) else Wc.onSurfaceVariantDark)
                        }
                        Switch(checked = theme == AppTheme.PRISM_LIGHT, onCheckedChange = { viewModel.selectTheme(if (it) AppTheme.PRISM_LIGHT else AppTheme.PRISM_DARK) })
                    }
                }
            }

            WidgetCard(isLightTheme = isLight) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Notifications", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Wc.primary)
                    SettingsToggleItem("Vibrate on Finish", vibrateFinish, { viewModel.toggleVibrateOnFinish() }, isLight)
                    SettingsToggleItem("Vibrate on Error", vibrateError, { viewModel.toggleVibrateOnError() }, isLight)
                    SettingsToggleItem("Notify on Error", notifyError, { viewModel.toggleNotifyOnError() }, isLight)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingsToggleItem(label: String, checked: Boolean, onToggle: () -> Unit, isLight: Boolean) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp, color = if (isLight) Color(0xFF1A202C) else Wc.onSurfaceDark)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
