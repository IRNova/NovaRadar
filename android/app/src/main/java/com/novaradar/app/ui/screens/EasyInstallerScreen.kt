package com.novaradar.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
import com.novaradar.app.ui.theme.VazirmatnFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun EasyInstallerScreen(viewModel: NovaRadarViewModel) {
    val lang by viewModel.selectedLanguage.collectAsState()
    val theme by viewModel.selectedTheme.collectAsState()
    val isLight = theme == AppTheme.PRISM_LIGHT
    val isFa = lang == AppLanguage.FA
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var tab by remember { mutableStateOf(0) }
    var apiToken by remember { mutableStateOf("") }
    var workerName by remember { mutableStateOf("nova-proxy") }
    var userUuid by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var cleanIp by remember { mutableStateOf("104.16.2.2") }
    var isDeploying by remember { mutableStateOf(false) }
    var deployLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var deployedUrl by remember { mutableStateOf("") }
    var deployError by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetCard(isLightTheme = isLight) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Wc.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Download, null, tint = Wc.primary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(if (isFa) "نوا ویزارد" else "Nova Wizard", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isLight) Color(0xFF1A202C) else Wc.onSurfaceDark)
                        Text(if (isFa) "دیپلوی خودکار ورکر" else "Auto Worker Deployment", fontSize = 11.sp, color = if (isLight) Color(0xFF4A5568) else Wc.onSurfaceVariantDark)
                    }
                }
            }

            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isLight) Color(0xFFF1F5F9) else Wc.surfaceContainerDark.copy(alpha = 0.5f)).padding(4.dp)) {
                listOf("Deployer" to 0, "Guides" to 1).forEach { (label, idx) ->
                    val active = tab == idx
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (active) Wc.primary else Color.Transparent).clickable { tab = idx }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(label, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, color = if (active) Color.White else if (isLight) Color(0xFF475569) else Color.Gray)
                    }
                }
            }

            if (tab == 0) {
                WidgetCard(isLightTheme = isLight) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (isFa) "تنظیمات دیپلوی" else "Deployment Settings", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Wc.primary)

                        OutlinedTextField(value = apiToken, onValueChange = { apiToken = it }, label = { Text("API Token") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Wc.primary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)))

                        OutlinedTextField(value = workerName, onValueChange = { workerName = it }, label = { Text("Worker Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Wc.primary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)))

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(value = userUuid, onValueChange = { userUuid = it }, label = { Text("UUID") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Wc.primary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)))
                            IconButton(onClick = { userUuid = java.util.UUID.randomUUID().toString() }, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Wc.primary.copy(alpha = 0.1f))) {
                                Icon(Icons.Default.Refresh, null, tint = Wc.primary)
                            }
                        }

                        OutlinedTextField(value = cleanIp, onValueChange = { cleanIp = it }, label = { Text("Proxy IP") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Wc.primary, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)))

                        Button(onClick = {
                            isDeploying = true; deployError = ""; deployedUrl = ""; deployLogs = emptyList()
                            coroutineScope.launch(Dispatchers.IO) {
                                val isDemo = apiToken.trim().isEmpty() || apiToken.trim().lowercase() == "demo"
                                executeCloudflareDeployment(apiToken, workerName, userUuid, cleanIp, isDemo, isFa, { deployLogs = deployLogs + it }, { deployedUrl = it; isDeploying = false }, { deployError = it; isDeploying = false })
                            }
                        }, enabled = !isDeploying, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Wc.primary)) {
                            if (isDeploying) { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                            Text(if (isDeploying) "Deploying..." else "Deploy to Cloudflare", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                if (deployLogs.isNotEmpty() || deployError.isNotEmpty() || deployedUrl.isNotEmpty()) {
                    WidgetCard(isLightTheme = isLight, borderColor = Wc.primary.copy(alpha = 0.12f)) {
                        Text("Terminal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Wc.primary)
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF030712)).border(0.5.dp, Wc.primary.copy(alpha = 0.2f), RoundedCornerShape(10.dp)).padding(10.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                deployLogs.forEach { Text(it, fontSize = 8.sp, color = Wc.primary.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace) }
                                if (deployError.isNotEmpty()) Text("ERROR: $deployError", fontSize = 8.sp, color = Wc.error, fontFamily = FontFamily.Monospace)
                                if (isDeploying) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { CircularProgressIndicator(modifier = Modifier.size(8.dp), strokeWidth = 1.dp, color = Wc.primary); Text("Processing...", fontSize = 8.sp, color = Color.Gray) }
                            }
                        }
                    }
                }

                if (deployedUrl.isNotEmpty()) {
                    val subUrl = "https://sub.novaproxy.online/sub?url=$deployedUrl"
                    WidgetCard(isLightTheme = isLight, borderColor = Wc.success.copy(alpha = 0.3f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Deployment Succeeded!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Wc.success)
                            Text("A private gateway is live on Cloudflare edge.", fontSize = 11.sp, color = if (isLight) Color(0xFF4A5568) else Wc.onSurfaceVariantDark, textAlign = TextAlign.Center)
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (isLight) Color(0xFFF8FAFC) else Color.Black.copy(alpha = 0.3f)).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(subUrl, modifier = Modifier.weight(1f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = if (isLight) Color(0xFF334155) else Color.LightGray, maxLines = 1)
                                Spacer(Modifier.width(4.dp))
                                IconButton(onClick = { clipboardManager.setText(AnnotatedString(subUrl)); Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show() }, modifier = Modifier.size(22.dp)) {
                                    Icon(Icons.Default.ContentCopy, null, tint = Wc.primary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                WidgetCard(isLightTheme = isLight) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (isFa) "راهنمای اتصال" else "Connection Guide", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Wc.primary)
                        Text(if (isFa) "کلاینت مناسب نصب کنید، سپس سابسکرایب لینک را وارد کنید." else "Install a client app, then import the subscription link.", fontSize = 11.sp, color = if (isLight) Color(0xFF4A5568) else Wc.onSurfaceVariantDark)
                    }
                }
                WidgetCard(isLightTheme = isLight) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(Wc.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Text("1", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Wc.primary) }
                        Column(Modifier.weight(1f)) {
                            Text("Install Client", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isLight) Color(0xFF1A202C) else Wc.onSurfaceDark)
                            Text("v2rayNG, Nekobox, or V2Box", fontSize = 10.sp, color = if (isLight) Color(0xFF4A5568) else Wc.onSurfaceVariantDark)
                        }
                    }
                }
                WidgetCard(isLightTheme = isLight) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(Wc.success.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Text("2", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Wc.success) }
                        Column(Modifier.weight(1f)) {
                            Text("Import Subscription", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isLight) Color(0xFF1A202C) else Wc.onSurfaceDark)
                            Text("Use subscription URL from Deployer tab", fontSize = 10.sp, color = if (isLight) Color(0xFF4A5568) else Wc.onSurfaceVariantDark)
                        }
                    }
                }
                WidgetCard(isLightTheme = isLight) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(Wc.warning.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Text("3", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Wc.warning) }
                        Column(Modifier.weight(1f)) {
                            Text("Scan with Radar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isLight) Color(0xFF1A202C) else Wc.onSurfaceDark)
                            Text("Go to Radar tab and start scanning", fontSize = 10.sp, color = if (isLight) Color(0xFF4A5568) else Wc.onSurfaceVariantDark)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
