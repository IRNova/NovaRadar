package com.novaradar.app.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaradar.app.ui.components.Wc

private val wizardPages = listOf(
    WizardPage("NOVA RADAR", "IP Scanner & Proxy Config Builder", "Scan Cloudflare, Akamai & Vercel IPs to find the fastest routes for your proxy.", Color(0xFF0D7DB3)),
    WizardPage("SCAN ENGINE", "Two-Phase Probe Technology", "Quick TCP scan + Deep verification with TLS handshake. Find alive IPs in seconds.", Color(0xFF10B981)),
    WizardPage("CONFIG BUILDER", "VLESS / VMess / Clash / SingBox", "Build proxy configs directly from scan results. Copy or export with Nova suffix.", Color(0xFF8B5CF6)),
    WizardPage("WORKER SETUP", "Deploy Your Own Worker", "Use Cloudflare Workers for custom SNI routing. Set your Worker URL in the Config Builder.", Color(0xFFF59E0B)),
    WizardPage("READY", "Start Scanning Now", "Configure ports and IP sources in Settings, then hit the start button to begin.", Color(0xFF0D7DB3))
)

private data class WizardPage(val title: String, val subtitle: String, val desc: String, val accent: Color)

@Composable
fun WelcomeWizard(onDismiss: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { wizardPages.size })
    val scope = rememberCoroutineScope()
    var currentPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(pagerState.currentPage) { currentPage = pagerState.currentPage }

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0E1A)), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Spacer(Modifier.height(48.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { page ->
                val data = wizardPages[page]
                Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    // Accent circle
                    Box(
                        Modifier.size(120.dp).clip(CircleShape)
                            .background(data.accent.copy(alpha = 0.12f))
                            .border(1.5.dp, data.accent.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.size(60.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(data.accent, data.accent.copy(alpha = 0.6f))))
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                    Text(data.title, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = data.accent, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(data.subtitle, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF8892A8), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Text(data.desc, fontSize = 11.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center, lineHeight = 18.sp)
                }
            }

            // Page indicators
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                wizardPages.indices.forEach { i ->
                    Box(
                        Modifier.size(if (i == currentPage) 8.dp else 6.dp).clip(CircleShape)
                            .background(if (i == currentPage) wizardPages[i].accent else Color(0xFF334155))
                    )
                    if (i < wizardPages.size - 1) Spacer(Modifier.width(6.dp))
                }
            }

            // Action buttons
            Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (currentPage < wizardPages.size - 1) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).padding(4.dp)
                    ) { Text("Skip", fontSize = 11.sp, color = Color(0xFF64748B)) }
                }
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(wizardPages[currentPage].accent.copy(alpha = 0.12f))
                        .clickable {
                            if (currentPage < wizardPages.size - 1) {
                                scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                            } else onDismiss()
                        }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (currentPage < wizardPages.size - 1) "Next" else "Get Started",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = wizardPages[currentPage].accent
                    )
                }
            }
        }
    }
}
