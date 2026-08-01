package com.novaradar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaradar.app.ui.components.Wc
import com.novaradar.app.ui.components.WidgetCard
import com.novaradar.app.ui.localization.Localization
import com.novaradar.app.ui.viewmodel.AppLanguage
import com.novaradar.app.ui.viewmodel.AppTheme
import com.novaradar.app.ui.viewmodel.NovaRadarViewModel

@Composable
fun ImportScreen(viewModel: NovaRadarViewModel) {
    val lang by viewModel.selectedLanguage.collectAsState()
    val theme by viewModel.selectedTheme.collectAsState()
    val isLight = theme == AppTheme.PRISM_LIGHT
    val context = LocalContext.current
    val importOutput by viewModel.importOutput.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val aliveIps by viewModel.allAliveIps.collectAsState()

    var ipText by remember { mutableStateOf("") }
    var mode by remember { mutableIntStateOf(1) }
    var outputText by remember { mutableStateOf("") }
    var rawOutput by remember { mutableStateOf("") }
    var importTab by remember { mutableIntStateOf(0) }
    var selectedOperator by remember { mutableStateOf("all") }
    var ipCount by remember { mutableIntStateOf(20) }
    var showSuffixPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(isScanning, mode) {
        if (!isScanning && mode == 1 && aliveIps.isNotEmpty() && rawOutput.isEmpty()) {
            showSuffixPrompt = true
        }
    }

    val displayText = when (mode) {
        0 -> outputText
        1 -> if (rawOutput.isNotEmpty()) rawOutput else importOutput
        2 -> importOutput
        else -> ""
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetCard(isLightTheme = isLight) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Wc.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.List, null, tint = Wc.primary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(Localization.get("import_ip", lang), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isLight) Color(0xFF1A202C) else Wc.onSurfaceDark)
                        Text(if (importTab == 0) "Manual Paste" else "Auto Scanner", fontSize = 11.sp, color = if (isLight) Color(0xFF4A5568) else Wc.onSurfaceVariantDark)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Manual" to 0, "Auto" to 1).forEach { (l, id) ->
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (importTab == id) Wc.primary.copy(alpha = 0.15f) else Color.Transparent).clickable { importTab = id }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(l, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (importTab == id) Wc.primary else Color.Gray)
                            }
                        }
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (isLight) Color(0xFFF8FAFC) else Wc.surfaceContainerDark.copy(alpha = 0.5f)).border(0.5.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(14.dp)).padding(8.dp)) {
                if (importTab == 0) {
                    TextField(value = ipText, onValueChange = { ipText = it }, modifier = Modifier.fillMaxSize(), placeholder = { Text(Localization.get("import_placeholder", lang), fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.4f), fontFamily = FontFamily.Monospace) }, textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = if (isLight) Color(0xFF1A202C) else Wc.onSurfaceDark), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), maxLines = Int.MAX_VALUE)
                } else {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Operator", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Wc.primary.copy(alpha = 0.5f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("all" to "All", "mci" to "MCI", "mtn" to "MTN", "ict" to "ICT").forEach { (key, label) ->
                                Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (selectedOperator == key) Wc.primary.copy(alpha = 0.15f) else Color.Transparent).clickable { selectedOperator = key }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selectedOperator == key) Wc.primary else Color.Gray)
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Count:", fontSize = 10.sp, color = if (isLight) Color(0xFF4A5568) else Wc.onSurfaceVariantDark)
                            Slider(value = ipCount.toFloat(), onValueChange = { ipCount = it.toInt() }, valueRange = 5f..100f, steps = 18, modifier = Modifier.weight(1f))
                            Text("$ipCount", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Wc.primary)
                        }
                        Button(onClick = { ipText = viewModel.generateOperatorIps(selectedOperator, ipCount); importTab = 0 }, modifier = Modifier.fillMaxWidth().height(34.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Wc.primary.copy(alpha = 0.15f), contentColor = Wc.primary)) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)); Text("Generate", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (importTab == 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(Triple(0, "Suffix Only", Color(0xFF818CF8)), Triple(1, "Scan", Wc.primary), Triple(2, "Scan+Suffix", Wc.success)).forEach { (id, label, color) ->
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (mode == id) color.copy(alpha = 0.15f) else Color.Transparent).clickable { mode = id }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (mode == id) color else Color.Gray)
                        }
                    }
                }

                Button(onClick = {
                    viewModel.setImportedIps(ipText); outputText = ""; rawOutput = ""; viewModel.clearImportOutput()
                    if (mode == 0) outputText = viewModel.suffixOnly(context) else viewModel.startScanWithImportedIps(mode == 2)
                }, modifier = Modifier.fillMaxWidth().height(38.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Wc.primary.copy(alpha = 0.15f), contentColor = Wc.primary)) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(6.dp)); Text("Start", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            if (displayText.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().weight(0.6f).clip(RoundedCornerShape(14.dp)).background(if (isLight) Color(0xFFF8FAFC) else Wc.surfaceContainerDark.copy(alpha = 0.5f)).border(0.5.dp, Wc.primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp)).padding(8.dp)) {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Output", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Wc.primary.copy(alpha = 0.5f))
                            Row { listOf(Icons.Default.ContentCopy to { viewModel.copyImportOutput(context) }, Icons.Default.Save to { viewModel.saveImportOutputToFile(context) }).forEach { (icon, action) -> IconButton(onClick = action, modifier = Modifier.size(22.dp)) { Icon(icon, null, tint = Wc.primary, modifier = Modifier.size(13.dp)) } } }
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.weight(1f)) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                items(displayText.lines()) { line ->
                                    Text(line, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = if (line.contains("#Nova-")) Wc.success.copy(alpha = 0.9f) else if (isLight) Color(0xFF1A202C) else Wc.onSurfaceDark.copy(alpha = 0.7f), maxLines = 1)
                                }
                            }
                        }
                    }
                }
            } else if (isScanning) {
                Box(Modifier.fillMaxWidth().weight(0.6f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Wc.primary, strokeWidth = 2.dp)
                        Spacer(Modifier.height(6.dp)); Text("Scanning...", fontSize = 10.sp, color = Wc.primary.copy(alpha = 0.6f))
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().weight(0.6f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Cloud, null, tint = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(4.dp)); Text(Localization.get("import_no_ips", lang), fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    if (showSuffixPrompt) {
        AlertDialog(onDismissRequest = { rawOutput = aliveIps.joinToString("\n") { "${it.ip}:${it.port}" }; showSuffixPrompt = false },
            title = { Text("Nova Proxy Suffix", fontWeight = FontWeight.Bold) },
            text = { Text("Add Nova suffix to ${aliveIps.size} verified IPs?") },
            confirmButton = { TextButton(onClick = { viewModel.suffixForNovaProxy(context); showSuffixPrompt = false }) { Text("Yes, Suffix", color = Wc.success) } },
            dismissButton = { TextButton(onClick = { rawOutput = aliveIps.joinToString("\n") { "${it.ip}:${it.port}" }; showSuffixPrompt = false }) { Text("Raw Only", color = Wc.error) } },
            containerColor = if (isLight) Color.White else Wc.surfaceDark, shape = RoundedCornerShape(20.dp))
    }
}
