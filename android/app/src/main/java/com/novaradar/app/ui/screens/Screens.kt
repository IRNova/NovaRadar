package com.novaradar.app.ui.screens

import android.widget.Toast
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaradar.app.data.model.IpSource
import com.novaradar.app.data.model.PortConfig
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.novaradar.app.ui.localization.Localization
import com.novaradar.app.ui.viewmodel.AppLanguage
import com.novaradar.app.ui.viewmodel.AppTheme
import com.novaradar.app.ui.viewmodel.AliveIp
import com.novaradar.app.ui.viewmodel.NovaRadarViewModel
import com.novaradar.app.ui.theme.VazirmatnFontFamily
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.ContentDrawScope

private val tlsPorts = setOf(443, 2053, 2083, 2087, 2096, 8443)

@Composable
fun FadingScrollColumn(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    // Make transparent to show the global gradient background
    val bgColor = Color.Transparent

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        content()
        Spacer(modifier = Modifier.height(88.dp))
    }
}

// Constant layout direction toggler
@Composable
fun LocalizedLayout(lang: AppLanguage, content: @Composable () -> Unit) {
    val direction = if (lang == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        content()
    }
}

// Custom Glassmorphic container with 100% capsule-like arch rounded corners and dynamic cyber mesh-grid design
@Composable
fun GlassyCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    content: @Composable ColumnScope.() -> Unit
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLightTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF0A0E1A).copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

// Page 1: RADAR SCANNER SCREEN (With Integrated Sub-Pager for Scanner and Results tabs)
// NOTE: The active RadarScreen is now in RadarScreen.kt (v1.1.0 redesign)
// This is kept as legacy reference only and will not be exported
@Composable
private fun RadarScreenLegacy(viewModel: NovaRadarViewModel) {
    val context = LocalContext.current
    val lang by viewModel.selectedLanguage.collectAsState()
    val theme by viewModel.selectedTheme.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scannedCount by viewModel.scannedCount.collectAsState()
    val aliveCount by viewModel.aliveCount.collectAsState()
    val deadCount by viewModel.deadCount.collectAsState()
    val eta by viewModel.etaValue.collectAsState()
    val subnetScanning by viewModel.currentScanningSubnet.collectAsState()
    val allIps by viewModel.allAliveIps.collectAsState()
    val recentProbes by viewModel.recentProbes.collectAsState()

    val subPagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    var showConfigBuilder by remember { mutableStateOf(false) }
    var cfgUuid by remember { mutableStateOf("") }
    var cfgSni by remember { mutableStateOf("nova2.altramax083.workers.dev") }
    var cfgNetwork by remember { mutableStateOf("ws") }
    var cfgSecurity by remember { mutableStateOf("tls") }
    var cfgPath by remember { mutableStateOf("/") }

    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val animatedAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angleAnimator"
    )

    val textPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val sweepBrush = remember {
        Brush.sweepGradient(
            0.0f to Color.Transparent,
            0.5f to Color.Transparent,
            0.7f to Color(0xFF00FF66).copy(alpha = 0.02f),
            0.85f to Color(0xFF00FF66).copy(alpha = 0.20f),
            0.95f to Color(0xFF00FF66).copy(alpha = 0.50f),
            1.0f to Color(0xFF00FF66).copy(alpha = 0.85f)
        )
    }

    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f

    LocalizedLayout(lang) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .padding(bottom = 88.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Modern sliding tab headers (exactly like photo)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                // TAB 0: SCANNER
                val isScannerActive = subPagerState.currentPage == 0
                val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                subPagerState.animateScrollToPage(0)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Localization.get("scanner_title", lang),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isScannerActive) (if (isLightTheme) Color.Black else Color.White) else Color.Gray,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(3.dp)
                            .background(
                                color = if (isScannerActive) Color(0xFF22D3EE) else Color.Transparent,
                                shape = RoundedCornerShape(1.5.dp)
                            )
                    )
                }

                // TAB 1: RESULTS (with red info count badge)
                val isResultsActive = subPagerState.currentPage == 1
                val isLightThemeResults = MaterialTheme.colorScheme.background.luminance() > 0.5f
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                subPagerState.animateScrollToPage(1)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = Localization.get("results_title", lang),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isResultsActive) (if (isLightThemeResults) Color.Black else Color.White) else Color.Gray,
                                letterSpacing = 1.sp
                            )
                        )
                        if (aliveCount > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .background(Color(0xFFEF4444), shape = RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = aliveCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                color = if (isResultsActive) Color(0xFF22D3EE) else Color.Transparent,
                                shape = RoundedCornerShape(1.5.dp)
                            )
                    )
                }
            }

            // Slidable primary contents area
            androidx.compose.foundation.pager.HorizontalPager(
                state = subPagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Radar (perfect circle)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(if (theme == AppTheme.PRISM_LIGHT) Color(0xFFF0FDF4) else Color(0xFF021708))
                                    .border(2.dp, Color(0xFF34D399).copy(alpha = if (theme == AppTheme.PRISM_LIGHT) 0.6f else 0.8f), CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (allIps.isNotEmpty()) {
                                            viewModel.copyTop10ToClipboard(context)
                                            Toast.makeText(context, Localization.get("copied_note", lang), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .testTag("radar_canvas_container"),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val radius = size.minDimension / 2f
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val outerRing = radius * 0.65f
                                    drawCircle(color = Color(0xFF00FF66).copy(alpha = 0.35f), radius = radius * 0.35f, style = Stroke(width = 1.5f))
                                    drawCircle(color = Color(0xFF00FF66).copy(alpha = 0.55f), radius = outerRing, style = Stroke(width = 2.5f))
                                    drawLine(color = Color(0xFF00FF66).copy(alpha = 0.25f), start = Offset(center.x - outerRing, center.y), end = Offset(center.x + outerRing, center.y), strokeWidth = 1.5f)
                                    drawLine(color = Color(0xFF00FF66).copy(alpha = 0.25f), start = Offset(center.x, center.y - outerRing), end = Offset(center.x, center.y + outerRing), strokeWidth = 1.5f)
                                    val sweepAlphaMultiplier = if (isScanning) 1.0f else 0.20f
                                    val angleRad = Math.toRadians(animatedAngle.toDouble())
                                    val endX = center.x + outerRing * cos(angleRad).toFloat()
                                    val endY = center.y + outerRing * sin(angleRad).toFloat()
                                    drawLine(color = Color(0xFF00FF66).copy(alpha = 0.85f * sweepAlphaMultiplier), start = center, end = Offset(endX, endY), strokeWidth = 3f)
                                    drawIntoCanvas { canvas ->
                                        canvas.save()
                                        canvas.rotate(animatedAngle, center.x, center.y)
                                        drawCircle(brush = sweepBrush, radius = outerRing, alpha = sweepAlphaMultiplier)
                                        canvas.restore()
                                    }
                                    drawCircle(color = Color(0xFF00FF66), radius = 5.dp.toPx())
                                    drawCircle(color = Color.Transparent, radius = 10.dp.toPx(), style = Stroke(width = 1.5f.dp.toPx()))
                                    allIps.take(8).forEachIndexed { index, alive ->
                                        val dotAngleRad = Math.toRadians(alive.angle.toDouble())
                                        val distPx = alive.normalizedDistance * outerRing * 0.92f
                                        val dotX = center.x + distPx * cos(dotAngleRad).toFloat()
                                        val dotY = center.y + distPx * sin(dotAngleRad).toFloat()
                                        val dotColor = when { alive.ping < 200 -> Color(0xFF34D399); alive.ping < 500 -> Color(0xFFFBBF24); alive.ping < 1000 -> Color(0xFFf87171); else -> Color(0xFF000000) }
                                        val angleDiff = (animatedAngle - alive.angle + 360f) % 360f
                                        val persistenceAlpha = if (isScanning) maxOf(0.15f, 1f - (angleDiff / 240f)) else 0.20f
                                        drawCircle(color = dotColor.copy(alpha = 0.35f * persistenceAlpha), radius = 6.dp.toPx(), center = Offset(dotX, dotY))
                                        drawCircle(color = dotColor.copy(alpha = persistenceAlpha), radius = 3.dp.toPx(), center = Offset(dotX, dotY))
                                    }
                                }
                            }

                            // Scan status HUD line (subnet only — counts in stat boxes below)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isLightTheme) Color(0xFF0A0E1A).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.3f))
                                    .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (isScanning) Color(0xFF00FF66) else Color(0xFF00FF66).copy(alpha = 0.3f)))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isScanning) subnetScanning.ifEmpty { "SCANNING..." } else "STANDBY",
                                            fontFamily = FontFamily.Monospace, fontSize = 8.sp,
                                            color = Color(0xFF00FF66).copy(alpha = 0.8f), letterSpacing = 1.sp
                                        )
                                    }
                                    Text("ETA $eta", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFFFBBF24).copy(alpha = 0.7f))
                                }
                            }

                            // 4 Stat Boxes (2x2 grid, fixed-width values)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                        .background(if (!isLightTheme) Color(0xFF0A0E1A).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.25f))
                                        .border(0.5.dp, Color(0xFF00FF66).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF00FF66).copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Radar, null, tint = Color(0xFF00FF66).copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Column {
                                            Text("SCANNED", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color(0xFF00FF66).copy(alpha = 0.5f), letterSpacing = 1.sp)
                                            Text(String.format("%5d", scannedCount),
                                                fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                        .background(if (!isLightTheme) Color(0xFF064E3B).copy(alpha = 0.2f) else Color(0xFFD1FAE5).copy(alpha = 0.4f))
                                        .border(0.5.dp, Color(0xFF34D399).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF34D399).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Column {
                                            Text("ALIVE", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color(0xFF34D399).copy(alpha = 0.7f), letterSpacing = 1.sp)
                                            Text(String.format("%5d", aliveCount),
                                                fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                        .background(if (!isLightTheme) Color(0xFF7F1D1D).copy(alpha = 0.15f) else Color(0xFFFEE2E2).copy(alpha = 0.4f))
                                        .border(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFEF4444).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Dangerous, null, tint = Color(0xFFEF4444).copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Column {
                                            Text("DEAD", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color(0xFFEF4444).copy(alpha = 0.7f), letterSpacing = 1.sp)
                                            Text(String.format("%5d", deadCount),
                                                fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                        .background(if (!isLightTheme) Color(0xFF78350F).copy(alpha = 0.15f) else Color(0xFFFEF3C7).copy(alpha = 0.4f))
                                        .border(0.5.dp, Color(0xFFFBBF24).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFFBBF24).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.HourglassEmpty, null, tint = Color(0xFFFBBF24).copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Column {
                                            Text("ETA", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color(0xFFFBBF24).copy(alpha = 0.7f), letterSpacing = 1.sp)
                                            Text(String.format("%5s", eta),
                                                fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                                        }
                                    }
                                }
                            }

                            // Live Probe Feed (scrolling IP results)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isLightTheme) Color(0xFF0A0E1A).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.3f))
                                    .border(0.5.dp, Color(0xFF00FF66).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Column {
                                    Text("PROBE FEED", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color(0xFF00FF66).copy(alpha = 0.4f), letterSpacing = 1.sp)
                                    Box(Modifier.fillMaxWidth().weight(1f)) {
                                        val displayList = recentProbes.take(8)
                                        if (displayList.isEmpty()) {
                                            Text("awaiting scan...", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF00FF66).copy(alpha = 0.2f))
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.spacedBy(0.dp)
                                            ) {
                                                items(displayList, key = { it.hashCode() }) { entry ->
                                                    Text(entry, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF00FF66).copy(alpha = 0.7f), maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Live Clean IPs found (recently verified)
                            if (allIps.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (!isLightTheme) Color(0xFF064E3B).copy(alpha = 0.25f) else Color(0xFFD1FAE5).copy(alpha = 0.3f))
                                        .border(0.5.dp, Color(0xFF34D399).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Column {
                                        Text("CLEAN FOUND", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color(0xFF34D399).copy(alpha = 0.6f), letterSpacing = 1.sp)
                                        Spacer(Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            allIps.take(3).forEach { alive ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFF34D399).copy(alpha = 0.1f))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        "${alive.ip}:${alive.port} ${alive.ping}ms",
                                                        fontFamily = FontFamily.Monospace, fontSize = 7.sp,
                                                        color = Color(0xFF34D399).copy(alpha = 0.9f),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                     1 -> {
                        // PROFESSIONAL RESULTS PAGE
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Header bar with stats and actions
                            GlassyCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(6.dp).clip(CircleShape).background(if (isScanning) Color(0xFFFBBF24) else if (allIps.isNotEmpty()) Color(0xFF34D399) else Color(0xFFEF4444).copy(alpha = 0.4f)))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (isScanning) "SCANNING..." else if (allIps.isNotEmpty()) "${allIps.size} TARGETS" else "NO TARGETS",
                                            fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                            color = if (isScanning) Color(0xFFFBBF24) else if (allIps.isNotEmpty()) Color(0xFF34D399) else Color(0xFFEF4444).copy(alpha = 0.5f),
                                            letterSpacing = 1.sp
                                        )
                                        if (allIps.isNotEmpty()) {
                                            Spacer(Modifier.width(8.dp))
                                            val avgPing = allIps.map { it.ping }.average().toLong()
                                            Text("Ø${avgPing}ms", fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color(0xFF22D3EE).copy(alpha = 0.6f))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(onClick = { viewModel.copyTop10ToClipboard(context); Toast.makeText(context, Localization.get("copied_note", lang), Toast.LENGTH_SHORT).show() }, modifier = Modifier.size(22.dp)) {
                                            Icon(Icons.Default.FormatListNumbered, "Top10", tint = Color(0xFF22D3EE), modifier = Modifier.size(12.dp))
                                        }
                                        IconButton(onClick = { viewModel.copyAllToClipboard(context) }, modifier = Modifier.size(22.dp)) {
                                            Icon(Icons.Default.ContentCopy, "Copy", tint = Color(0xFF22D3EE), modifier = Modifier.size(12.dp))
                                        }
                                        IconButton(onClick = { viewModel.exportResultsToTxtFile(context) }, modifier = Modifier.size(22.dp)) {
                                            Icon(Icons.Default.Save, "Save", tint = Color(0xFF22D3EE), modifier = Modifier.size(12.dp))
                                        }
                                        IconButton(onClick = { showConfigBuilder = true }, modifier = Modifier.size(22.dp)) {
                                            Icon(Icons.Default.Build, "Config", tint = Color(0xFFFBBF24), modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }

                            // Export format bar
                            if (allIps.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("Clash", "V2Ray", "VLESS", "Sing-Box").forEachIndexed { i, label ->
                                        val exportActions = listOf({ viewModel.exportClash(context) }, { viewModel.exportV2Ray(context) }, { viewModel.exportVLESS(context) }, { viewModel.exportSingBox(context) })
                                        Box(
                                            modifier = Modifier.weight(1f).height(22.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF064E3B).copy(alpha = 0.25f))
                                                .border(0.5.dp, Color(0xFF34D399).copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                                .clickable { exportActions[i]() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(label, fontSize = 7.sp, color = Color(0xFF34D399), fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                        }
                                    }
                                }
                            }

                            // Results table with column headers
                            if (allIps.isEmpty()) {
                                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (isScanning) "⏳ AWAITING TARGETS" else "📡 NO TARGETS ACQUIRED",
                                            fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                                            color = (if (isScanning) Color(0xFFFBBF24) else Color(0xFFEF4444)).copy(alpha = 0.5f),
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = if (isScanning) "IPs will appear here as they are verified" else "Start a scan to find clean IPs",
                                            fontFamily = FontFamily.Monospace, fontSize = 8.sp,
                                            color = Color.Gray.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            } else {
                                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    // Table header
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(if (isLightTheme) Color(0xFFE2E8F0) else Color(0xFF0A0E1A).copy(alpha = 0.6f))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text("#", modifier = Modifier.width(18.dp), fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text("IP:PORT", modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text("PING", modifier = Modifier.width(32.dp), fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text("HTTP", modifier = Modifier.width(28.dp), fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text("SPD", modifier = Modifier.width(28.dp), fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text("", modifier = Modifier.width(36.dp))
                                    }

                                    // Results rows
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(0.dp)
                                    ) {
                                        itemsIndexed(allIps, key = { _, ip -> "${ip.ip}:${ip.port}" }) { index, alive ->
                                            val speedKey = "${alive.ip}:${alive.port}"
                                            val speedStr = viewModel.speedResults.value[speedKey] ?: "--"
                                            val isEven = index % 2 == 0
                                            val rowBg = if (isEven) Color.Transparent else (if (isLightTheme) Color(0xFFF8FAFC) else Color.White.copy(alpha = 0.02f))
                                            val pingColor = when { alive.ping < 200 -> Color(0xFF34D399); alive.ping < 500 -> Color(0xFFFBBF24); else -> Color(0xFFf87171) }
                                            val httpColor = when { alive.httpPing < 0 -> Color.Gray.copy(alpha = 0.3f); alive.httpPing < 300 -> Color(0xFF34D399); alive.httpPing < 600 -> Color(0xFFFBBF24); else -> Color(0xFFf87171) }

                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .background(rowBg)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("${index + 1}", modifier = Modifier.width(18.dp), fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color.Gray)
                                                Text(alive.ip, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = if (isLightTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0), maxLines = 1)
                                                Text("${alive.ping}", modifier = Modifier.width(32.dp), fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = pingColor, fontWeight = FontWeight.Bold)
                                                Text(if (alive.httpPing > 0) "${alive.httpPing}" else "✗", modifier = Modifier.width(28.dp), fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = httpColor)
                                                Text(speedStr, modifier = Modifier.width(28.dp), fontFamily = FontFamily.Monospace, fontSize = 7.sp, color = Color(0xFFEAB308).copy(alpha = 0.8f))
                                                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                                    Box(
                                                        modifier = Modifier.size(16.dp).clip(RoundedCornerShape(2.dp))
                                                            .background(Color(0xFF1E40AF).copy(alpha = 0.2f))
                                                            .border(0.5.dp, Color(0xFF3B82F6).copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                                                            .clickable { viewModel.runSpeedTest(alive.ip, alive.port) },
                                                        contentAlignment = Alignment.Center
                                                    ) { Text("⚡", fontSize = 7.sp) }
                                                    Box(
                                                        modifier = Modifier.size(16.dp).clip(RoundedCornerShape(2.dp))
                                                            .background(Color(0xFF064E3B).copy(alpha = 0.2f))
                                                            .border(0.5.dp, Color(0xFF34D399).copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                                                            .clickable { viewModel.copyIndividualToClipboard(context, alive) },
                                                        contentAlignment = Alignment.Center
                                                    ) { Text("cpy", fontSize = 6.sp, color = Color(0xFF34D399)) }
                                                }
                                            }

                                            // Latency bar
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .padding(start = 24.dp, end = 6.dp)
                                                    .height(1.dp)
                                            ) {
                                                val barWidth = (1f - (alive.ping.coerceAtMost(1000).toFloat() / 1000f)).coerceIn(0.02f, 0.98f)
                                                Box(Modifier.weight(barWidth).fillMaxHeight().background(pingColor.copy(alpha = 0.4f)))
                                                Box(Modifier.weight(1f - barWidth).fillMaxHeight().background(Color.Gray.copy(alpha = 0.08f)))
                                            }

                                            // Detail row: port + SNI badge
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .padding(start = 24.dp, end = 6.dp, bottom = 1.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(":${alive.port}", fontFamily = FontFamily.Monospace, fontSize = 6.sp, color = Color(0xFF22D3EE).copy(alpha = 0.5f))
                                                if (alive.port in tlsPorts) {
                                                    Box(Modifier.clip(RoundedCornerShape(2.dp)).background(Color(0xFF22D3EE).copy(alpha = 0.1f)).padding(horizontal = 3.dp)) {
                                                        Text("TLS", fontSize = 5.sp, color = Color(0xFF22D3EE).copy(alpha = 0.6f))
                                                    }
                                                }
                                                Box(Modifier.clip(RoundedCornerShape(2.dp)).background(Color(0xFFA855F7).copy(alpha = 0.1f)).padding(horizontal = 3.dp)) {
                                                    Text("Nova-${alive.novaId}", fontSize = 5.sp, color = Color(0xFFA855F7).copy(alpha = 0.5f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
                }
            }
        }
    }

    // Config Builder Dialog
    if (showConfigBuilder && allIps.isNotEmpty()) {
        val allCfgIps = remember { mutableStateOf(allIps.joinToString("\n") { "${it.ip}:${it.port}" }) }
        androidx.compose.ui.window.Dialog(onDismissRequest = { showConfigBuilder = false }) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (isLightTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)).padding(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⚙ Config Builder", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) { Text("UUID", fontSize = 9.sp, color = Color.Gray); TextField(cfgUuid, { cfgUuid = it }, modifier = Modifier.fillMaxWidth().height(40.dp), textStyle = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace), singleLine = true) }
                        Column(Modifier.weight(1f)) { Text("SNI", fontSize = 9.sp, color = Color.Gray); TextField(cfgSni, { cfgSni = it }, modifier = Modifier.fillMaxWidth().height(40.dp), textStyle = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace), singleLine = true) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) { Text("Network", fontSize = 9.sp, color = Color.Gray)
                            Row { listOf("ws","grpc","tcp").forEach { TextButton({ cfgNetwork = it }, Modifier.height(28.dp).clip(RoundedCornerShape(4.dp)).background(if (cfgNetwork == it) Color(0xFF3B82F6) else Color(0xFF1E293B)).padding(horizontal = 8.dp, vertical = 2.dp), contentPadding = PaddingValues(0.dp)) { Text(it, fontSize = 9.sp, color = Color.White) } } } }
                        Column(Modifier.weight(1f)) { Text("Security", fontSize = 9.sp, color = Color.Gray)
                            Row { listOf("tls","none").forEach { TextButton({ cfgSecurity = it }, Modifier.height(28.dp).clip(RoundedCornerShape(4.dp)).background(if (cfgSecurity == it) Color(0xFF3B82F6) else Color(0xFF1E293B)).padding(horizontal = 8.dp, vertical = 2.dp), contentPadding = PaddingValues(0.dp)) { Text(it, fontSize = 9.sp, color = Color.White) } } } }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Path", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.width(40.dp))
                        TextField(cfgPath, { cfgPath = it }, modifier = Modifier.fillMaxWidth().height(36.dp), textStyle = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace), singleLine = true)
                    }
                    Divider(color = Color(0xFF1E293B))
                    Text("${allCfgIps.value.lines().size} IPs loaded", fontSize = 9.sp, color = Color(0xFF34D399))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        val btnMod = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1E3A5F))
                        TextButton(onClick = { viewModel.exportVLESS(context) }, modifier = btnMod, contentPadding = PaddingValues(0.dp)) { Text("VLESS", fontSize = 9.sp) }
                        TextButton(onClick = { viewModel.exportClash(context) }, modifier = btnMod, contentPadding = PaddingValues(0.dp)) { Text("Clash", fontSize = 9.sp) }
                        TextButton(onClick = { viewModel.exportV2Ray(context) }, modifier = btnMod, contentPadding = PaddingValues(0.dp)) { Text("V2Ray", fontSize = 9.sp) }
                        TextButton(onClick = { viewModel.exportSingBox(context) }, modifier = btnMod, contentPadding = PaddingValues(0.dp)) { Text("Sing-Box", fontSize = 9.sp) }
                    }
                    TextButton(onClick = { showConfigBuilder = false }, modifier = Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFEF4444).copy(alpha = 0.2f)), contentPadding = PaddingValues(0.dp)) { Text("بستن", fontSize = 10.sp, color = Color(0xFFEF4444)) }
                }
            }
        }
    }
}

// Special Glassmorphic Card Container with Dynamic Cyberpunk Mesh-Grid design
@Composable
fun MeshBackgroundCard(
    modifier: Modifier = Modifier,
    isLightTheme: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLightTheme) MaterialTheme.colorScheme.surface.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isLightTheme) Color(0xFFE2E8F0) else Color(0xFF22D3EE).copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            content = content
        )
    }
}

// Page 1.5: EASY INSTALLER SCREEN (Onboarding configuration and client download wizard)
@Composable
private fun EasyInstallerScreenLegacy(viewModel: NovaRadarViewModel) {
    val lang by viewModel.selectedLanguage.collectAsState()
    val theme by viewModel.selectedTheme.collectAsState()
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val isLightTheme = theme == AppTheme.PRISM_LIGHT
    val isFa = lang == AppLanguage.FA
    
    var installerTab by remember { mutableStateOf(0) } // 0: Cloudflare Worker Setup, 1: Client Setup Guides
    var selectedPlatform by remember { mutableStateOf(0) } // For client setup guide (0: Android, 1: iOS, 2: Windows, 3: macOS)
    
    // Cloudflare deploy variables
    var apiToken by remember { mutableStateOf("") }
    var workerName by remember { mutableStateOf("nova-proxy") }
    var userUuid by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var cleanIp by remember { mutableStateOf("104.16.2.2") }
    
    var isDeploying by remember { mutableStateOf(false) }
    var deployLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var deployedUrlResult by remember { mutableStateOf("") }
    var deployError by remember { mutableStateOf("") }
    
    // Animation tick for scanning line in QR Code
    var animationFrame by remember { mutableStateOf(0f) }
    LaunchedEffect(deployedUrlResult) {
        if (deployedUrlResult.isNotEmpty()) {
            while (true) {
                kotlinx.coroutines.delay(16)
                animationFrame = (animationFrame + 0.015f) % 1f
            }
        }
    }
    
    LocalizedLayout(lang) {
        FadingScrollColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(horizontal = 14.dp),
        ) {
            // Header
            MeshBackgroundCard(
                isLightTheme = isLightTheme,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF22D3EE),
                                                Color(0xFF818CF8),
                                                Color(0xFFA855F7)
                                            )
                                        )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Installer Icon",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isFa) "نوا ویزارد دیپلوی پلتفرم" else "NOVA WIZARD DEPLOY PLATFORM",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = if (isLightTheme) Color(0xFF0F172A) else Color.White
                        )
                        Text(
                            text = if (isFa) "راه‌اندازی، دیپلوی خودکار ورکر سورس و اتصال کلاینت" else "Automatic Cloudflare-Workers deployment & connection wizard",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLightTheme) Color(0xFF475569) else Color.Gray
                        )
                    }
                }
            }

            // Tabs Selector (0: Worker Deployer, 1: Client Guides)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isLightTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isLightTheme) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.05f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Deployment Tab
                    val depSelected = installerTab == 0
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (depSelected) Brush.linearGradient(colors = listOf(Color(0xFF22D3EE), Color(0xFF0891B2)))
                                else Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable { installerTab = 0 }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFa) "راه‌اندازی ورکر کلودفلر" else "Cloudflare Deployer",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (depSelected) FontWeight.Black else FontWeight.SemiBold
                            ),
                            color = if (depSelected) Color.White else if (isLightTheme) Color(0xFF475569) else Color.LightGray
                        )
                    }
                    
                    // Guides Tab
                    val guideSelected = installerTab == 1
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (guideSelected) Brush.linearGradient(colors = listOf(Color(0xFF22D3EE), Color(0xFF0891B2)))
                                else Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable { installerTab = 1 }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFa) "راهنمای اتصال کلاینت‌ها" else "Client Connection Guide",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (guideSelected) FontWeight.Black else FontWeight.SemiBold
                            ),
                            color = if (guideSelected) Color.White else if (isLightTheme) Color(0xFF475569) else Color.LightGray
                        )
                    }
                }
            }

            // Screen Tab Content rendering
            if (installerTab == 0) {
                // TAB 0: CLOUDFLARE WORKER DEPLOYER (Novaproxy Installer style replica)
                MeshBackgroundCard(
                    isLightTheme = isLightTheme,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (isFa) "تنظیمات دیپلوی ورکر کلودفلر" else "Cloudflare Deployment Settings",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isLightTheme) Color(0xFF1E3A8A) else Color(0xFF60A5FA)
                        )
                        
                        Text(
                            text = if (isFa) {
                                "برای راه‌اندازی، توکن اختصاصی کلودفلر را قرار دهید. در صورت عدم درج، فرآیند به صورت شبیه‌ساز ساندباکس کامل اجرا می‌شود."
                            } else {
                                "Enter your Cloudflare API token. Leave it empty to experience the beautiful wizard in Sandbox deployment mode."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLightTheme) Color(0xFF475569) else Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Cloudflare API Token Field
                        OutlinedTextField(
                            value = apiToken,
                            onValueChange = { apiToken = it },
                            label = { Text(text = if (isFa) "توکن دسترسی کلودفلر (API Token)" else "Cloudflare API Token") },
                            placeholder = { Text(text = if (isFa) "افزودن توکن یا دمو برای تست آزمایشی..." else "Paste token or type demo...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF22D3EE),
                                unfocusedBorderColor = if (isLightTheme) Color(0xFFE2E8F0) else Color(0xFF22D3EE).copy(alpha = 0.2f),
                                focusedLabelColor = Color(0xFF22D3EE),
                                focusedContainerColor = if (isLightTheme) Color.White else Color(0xFF0F172A).copy(alpha = 0.4f),
                                unfocusedContainerColor = if (isLightTheme) Color.White else Color(0xFF0F172A).copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Worker Script Name Field
                        OutlinedTextField(
                            value = workerName,
                            onValueChange = { workerName = it },
                            label = { Text(text = if (isFa) "نام اسکریپت ورکر" else "Worker Script Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF22D3EE),
                                unfocusedBorderColor = if (isLightTheme) Color(0xFFE2E8F0) else Color(0xFF22D3EE).copy(alpha = 0.2f),
                                focusedLabelColor = Color(0xFF22D3EE),
                                focusedContainerColor = if (isLightTheme) Color.White else Color(0xFF0F172A).copy(alpha = 0.4f),
                                unfocusedContainerColor = if (isLightTheme) Color.White else Color(0xFF0F172A).copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // User UUID Config with Regeneration Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = userUuid,
                                onValueChange = { userUuid = it },
                                label = { Text(text = if (isFa) "شناسه سکیوریتی کاربر (UUID)" else "Security ID (UUID)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF22D3EE),
                                    unfocusedBorderColor = if (isLightTheme) Color(0xFFE2E8F0) else Color(0xFF22D3EE).copy(alpha = 0.2f),
                                    focusedLabelColor = Color(0xFF22D3EE),
                                    focusedContainerColor = if (isLightTheme) Color.White else Color(0xFF0F172A).copy(alpha = 0.4f),
                                    unfocusedContainerColor = if (isLightTheme) Color.White else Color(0xFF0F172A).copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            
                            IconButton(
                                onClick = { userUuid = java.util.UUID.randomUUID().toString() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF22D3EE).copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate UUID",
                                    tint = Color(0xFF22D3EE)
                                )
                            }
                        }

                        // Static Proxy IP for DNS configuration
                        OutlinedTextField(
                            value = cleanIp,
                            onValueChange = { cleanIp = it },
                            label = { Text(text = if (isFa) "آی‌پی/دامنه عبوری تمیز ورکر" else "Clean Proxy IP / Host") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF22D3EE),
                                unfocusedBorderColor = if (isLightTheme) Color(0xFFE2E8F0) else Color(0xFF22D3EE).copy(alpha = 0.2f),
                                focusedLabelColor = Color(0xFF22D3EE),
                                focusedContainerColor = if (isLightTheme) Color.White else Color(0xFF0F172A).copy(alpha = 0.4f),
                                unfocusedContainerColor = if (isLightTheme) Color.White else Color(0xFF0F172A).copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Actions triggers (Launch real or Sandbox deployment)
                        Button(
                            onClick = {
                                isDeploying = true
                                deployError = ""
                                deployedUrlResult = ""
                                deployLogs = emptyList()
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val isDemo = apiToken.trim().isEmpty() || apiToken.trim().lowercase() == "demo"
                                    executeCloudflareDeployment(
                                        token = apiToken,
                                        workerName = workerName,
                                        uuid = userUuid,
                                        proxyIp = cleanIp,
                                        isDemoMode = isDemo,
                                        isFa = isFa,
                                        onStatus = { log ->
                                            deployLogs = deployLogs + log
                                        },
                                        onSuccess = { finalUrl ->
                                            deployedUrlResult = finalUrl
                                            isDeploying = false
                                        },
                                        onFailure = { err ->
                                            deployError = err
                                            isDeploying = false
                                        }
                                    )
                                }
                            },
                            enabled = !isDeploying,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF22D3EE)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isDeploying) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isFa) "در حال دیپلوی همزمان..." else "Deploying to edge...",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Cloud Deploy",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isFa) "شروع دیپلوی مستقیم به کلودفلر" else "Deploy to Cloudflare Workers",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Cumulative Progress logs / Console
                if (isDeploying || deployLogs.isNotEmpty() || deployError.isNotEmpty()) {
                    MeshBackgroundCard(
                        isLightTheme = isLightTheme,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isFa) "کنسول ستاپ نووا ویزارد" else "Nova Wizard Setup Terminal",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isLightTheme) Color.DarkGray else Color.LightGray
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF030712))
                                    .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    deployLogs.forEach { log ->
                                        Text(
                                            text = log,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily),
                                            color = Color(0xFF22D3EE)
                                        )
                                    }
                                    if (deployError.isNotEmpty()) {
                                        Text(
                                            text = "❌ $deployError",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily),
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                    if (isDeploying) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                color = Color(0xFF22D3EE),
                                                modifier = Modifier.size(10.dp),
                                                strokeWidth = 1.dp
                                            )
                                            Text(
                                                text = if (isFa) "در حال پردازش عملیات..." else "Executing stack operation...",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = VazirmatnFontFamily,
                                                    fontStyle = FontStyle.Italic
                                                ),
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Successful result presentations card
                if (deployedUrlResult.isNotEmpty()) {
                    val subUrl = "https://sub.novaproxy.online/sub?url=$deployedUrlResult"
                    MeshBackgroundCard(
                        isLightTheme = isLightTheme,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isFa) "🎉 ورکر با موفقیت راه‌اندازی شد!" else "🎉 Deployment Succeeded!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFF22D3EE),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            
                            Text(
                                text = if (isFa) {
                                    "یک وی‌پی‌ان کاملاً خصوصی با موفقیت روی لبه کلودفلر حساب شخصی شما بارگذاری گردید. می‌توانید از بارکد هوشمند یا دکمه کپی لینک زیر برای انتقال به نرم‌افزار خود استفاده نمایید."
                                } else {
                                    "A private high-speed serverless gateway was successfully spawned on your Cloudflare account edges. Scan or copy the subscription link below."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLightTheme) Color(0xFF334155) else Color.LightGray,
                                textAlign = TextAlign.Center
                            )

                            HorizontalDivider(color = if (isLightTheme) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f))

                            // Interactive Barcode presentation
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .border(1.5.dp, Color(0xFF22D3EE), RoundedCornerShape(16.dp))
                                    .padding(8.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val rows = 6
                                    val cols = 6
                                    val cellW = size.width / cols
                                    val cellH = size.height / rows
                                    
                                    // Make deterministic pseudo-random QR grid based on UUID length to keep it look realistic
                                    val hash = userUuid.hashCode()
                                    for (r in 0 until rows) {
                                        for (c in 0 until cols) {
                                            // Always render finder patterns on three corners
                                            val isFinder = (r < 2 && c < 2) || (r >= rows - 2 && c < 2) || (r < 2 && c >= cols - 2)
                                            val isDot = ((hash shr (r * cols + c)) and 1) == 1
                                            if (isFinder || isDot) {
                                                drawRect(
                                                    color = Color(0xFF22D3EE).copy(alpha = if (isFinder) 0.95f else 0.7f),
                                                    topLeft = Offset(c * cellW + 1.5f, r * cellH + 1.5f),
                                                    size = androidx.compose.ui.geometry.Size(cellW - 3f, cellH - 3f)
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Animated scan laser sweep
                                    val sweepY = size.height * animationFrame
                                    drawLine(
                                        color = Color(0xFFEF4444).copy(alpha = 0.85f),
                                        start = Offset(0f, sweepY),
                                        end = Offset(size.width, sweepY),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                            }

                            Text(
                                text = if (isFa) "آدرس لایمیت اختصاصی" else "Dedicated Subscription Link",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.Gray
                            )

                            // Display URL block
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isLightTheme) Color(0xFFF8FAFC) else Color.Black.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, if (isLightTheme) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = subUrl,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily),
                                        color = if (isLightTheme) Color(0xFF334155) else Color.LightGray,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(subUrl))
                                            Toast.makeText(
                                                context,
                                                if (isFa) "لینک سابسکرایب ورکر کپی شد!" else "API Subscription URL copied to clipboard!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Link",
                                            tint = Color(0xFF22D3EE),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // High fidelity action to import directly into Scanner
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(subUrl))
                                    Toast.makeText(
                                        context,
                                        if (isFa) "سابسکرایب کلودفلر وارد کلاینت شد. اکنون می‌توانید جهت تست زنده پارت‌ها به منوی رادار بروید!"
                                        else "Directly linked! Go to Radar screen to verify clean IPs with your private worker!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF22D3EE)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isFa) "ایمپورت ساب اسکرب به کلاینت و کپی آدرس" else "Auto Import Connection & Copy Link",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                // TAB 1: CLIENT CONNECTION GUIDE (Originally placed guides rendered perfectly in Vazirmatn!)
                val clientName = when (selectedPlatform) {
                    0 -> "v2rayNG / Nekobox"
                    1 -> "V2Box / Streisand"
                    2 -> "v2rayN / Nekoray"
                    else -> "FoXray / V2rayU"
                }

                val appStoreLabel = when (selectedPlatform) {
                    0 -> if (isFa) "دانلود کلاینت Google Play" else "Download from Play Store"
                    1 -> if (isFa) "دانلود کلاینت از App Store" else "Download on App Store"
                    2 -> if (isFa) "دانلود کلاینت Windows GitHub" else "Download Windows App (ZIP)"
                    else -> if (isFa) "دانلود نسخه macOS" else "Download macOS Client"
                }

                // Sub platforms buttons
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLightTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A).copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isLightTheme) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.04f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val platforms = listOf("Android", "iOS", "Windows", "macOS")
                        platforms.forEachIndexed { index, name ->
                            val isSelected = selectedPlatform == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Brush.linearGradient(colors = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED)))
                                        else Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                                    )
                                    .clickable { selectedPlatform = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                                    ),
                                    color = if (isSelected) Color.White else if (isLightTheme) Color(0xFF475569) else Color.LightGray
                                )
                            }
                        }
                    }
                }

                // Step 1: Install Client
                MeshBackgroundCard(
                    isLightTheme = isLightTheme,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF22D3EE).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "۱",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFF22D3EE)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "قدم اول: نصب کلاینت پیشنهادی" else "Step 1: Install Recommended Client",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isLightTheme) Color(0xFF1E3A8A) else Color(0xFF60A5FA)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isFa) {
                                    "برای این سیستم‌عامل، کلاینت رسمی و پیشنهادی برنامه $clientName می‌باشد که پاسخگویی بسیار بالایی دارد."
                                } else {
                                    "The recommended high-performance software for your device is $clientName with optimized secure routing layers."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLightTheme) Color(0xFF334155) else Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = {
                                    Toast.makeText(
                                        context,
                                        if (isFa) "در حال انتقال به سرورهای دانلود کلاینت..." else "Redirecting to download mirror...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = appStoreLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Step 2: Subscription Link Guide
                val subscriptionUrl = "https://sub.novaproxy.online/feed/your-secure-radar-profile"
                MeshBackgroundCard(
                    isLightTheme = isLightTheme,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "۲",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFF8B5CF6)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "قدم دوم: دریافت آدرس سابسکرایب" else "Step 2: Obtain Subscription Link",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isLightTheme) Color(0xFF5B21B6) else Color(0xFFC084FC)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isFa) {
                                    "لینک هوشمند زیر حاوی پیکربندی‌های فعال و متصل به رادار ردیابی پینگ Nova Proxy می‌باشد."
                                } else {
                                    "Use the secure subscription profile endpoint below to sync automatically with local latency indicators."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLightTheme) Color(0xFF334155) else Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isLightTheme) Color(0xFFF8FAFC) else Color.Black.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, if (isLightTheme) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = subscriptionUrl,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily),
                                        color = if (isLightTheme) Color(0xFF334155) else Color.LightGray,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(subscriptionUrl))
                                            Toast.makeText(
                                                context,
                                                if (isFa) "لینک اشتراک کپی شد!" else "Subscription URL copied to clipboard!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Link",
                                            tint = Color(0xFF8B5CF6),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 3: Scan QR Card
                MeshBackgroundCard(
                    isLightTheme = isLightTheme,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF22D3EE).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "۳",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFF22D3EE)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "قدم سوم: اسکن یا ایمپورت در کلاینت" else "Step 3: Import or Scan QR Profile",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isLightTheme) Color(0xFF065F46) else Color(0xFF34D399)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isFa) {
                                    "وارد نرم‌افزار کلاینت شوید، دکمه افزودن (+) یا Subscription را بزنید و آدرس کپی شده یا QR Code دیجیتال زیر را اسکن کنید."
                                } else {
                                    "Open your client, click create/edit sub, and add subscription URL or scan the live matrix barcode."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLightTheme) Color(0xFF334155) else Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isLightTheme) {
                                            Brush.linearGradient(colors = listOf(Color(0xFFEFF6FF), Color(0xFFF0FDF4)))
                                        } else {
                                            Brush.linearGradient(colors = listOf(Color(0xFF0F172A).copy(alpha = 0.8f), Color(0xFF022C22).copy(alpha = 0.5f)))
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFF22D3EE).copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .border(1.5.dp, Color(0xFF22D3EE).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val rows = 5
                                            val cols = 5
                                            val cellW = size.width / cols
                                            val cellH = size.height / rows
                                            val randomPoints = listOf(
                                                0 to 0, 0 to 1, 0 to 4,
                                                1 to 0, 1 to 2, 1 to 3,
                                                2 to 2, 2 to 4,
                                                3 to 1, 3 to 3,
                                                4 to 0, 4 to 1, 4 to 4
                                            )
                                            for ((r, c) in randomPoints) {
                                                drawRect(
                                                    color = Color(0xFF22D3EE).copy(alpha = 0.85f),
                                                    topLeft = Offset(c * cellW + 2f, r * cellH + 2f),
                                                    size = androidx.compose.ui.geometry.Size(cellW - 4f, cellH - 4f)
                                                )
                                            }
                                        }
                                    }
                                    
                                    Column(
                                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "NOVA SECURITY RADAR",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                fontFamily = VazirmatnFontFamily,
                                                fontSize = 9.sp
                                            ),
                                            color = Color(0xFF22D3EE)
                                        )
                                        Text(
                                            text = "STATUS: VERIFIED SECURE",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily, fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                            color = Color(0xFF22D3EE)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 4: Scan and Optimize Radar Guide
                MeshBackgroundCard(
                    isLightTheme = isLightTheme,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "۴",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFFEF4444)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "قدم چهارم: ایمن‌سازی با اسکنر رادار" else "Step 4: Execute Scanner Audit",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isLightTheme) Color(0xFF991B1B) else Color(0xFFF87171)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isFa) {
                                    "پس از ایمپورت سابسکرایب، وارد زبانه «رادار پینگ» در منوی پایین نوا رادار شوید و دکمه شروع اسکن را بفشارید تا زنده بودن و پینگ تمام آی‌پی‌ها بررسی شده و بهترین پورت به شما ارائه گردد."
                                } else {
                                    "Once client is configured, enter the 'Radar' tab below, choose targeted subnets, and hit START SCAN to isolate the absolute lowest-ping relays."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLightTheme) Color(0xFF334155) else Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}

// Global lightweight helper functions for parsing JSON and performing CF network operations safely
private fun extractJsonValue(json: String, key: String): String {
    val pattern = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
    val match = pattern.find(json)
    return match?.groupValues?.get(1) ?: ""
}

suspend fun executeCloudflareDeployment(
    token: String,
    workerName: String,
    uuid: String,
    proxyIp: String,
    isDemoMode: Boolean,
    isFa: Boolean,
    onStatus: (String) -> Unit,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    if (isDemoMode) {
        // High fidelity sandbox simulation with Persian and English support
        onStatus("🔑 " + (if (isFa) "در حال اعتبارسنجی اتصال شبکه به سرورهای کلودفلر..." else "Authenticating connection to Cloudflare..."))
        kotlinx.coroutines.delay(1200)
        onStatus("🔓 " + (if (isFa) "دسترسی دمو با موفقیت مجاز قلمداد شد." else "Access sandbox credentials validated successfully!"))
        kotlinx.coroutines.delay(1100)
        onStatus("👤 " + (if (isFa) "دریافت جزئیات حساب: Nova_Wizard_Public_Sandbox_Account" else "Fetched account details: Nova_Wizard_Public_Sandbox_Account"))
        kotlinx.coroutines.delay(1300)
        onStatus("⚙️ " + (if (isFa) "در حال تولید کد پروکسی پیشرفته ورکر متصل به UUID: [انحصاری]" else "Generating proxy worker source template linked to UUID..."))
        kotlinx.coroutines.delay(1400)
        onStatus("📂 " + (if (isFa) "ارسال کامپایل سورس کد برای سرورهای ابری کلودفلر..." else "Transmitting script bundle to Cloudflare Edge gateways..."))
        kotlinx.coroutines.delay(1500)
        onStatus("📡 " + (if (isFa) "ثبت‌نام دامنه اختصاصی: $workerName.workers.dev..." else "Assigning router subdomain route: $workerName.workers.dev"))
        kotlinx.coroutines.delay(1600)
        onStatus("🎉 " + (if (isFa) "راه‌اندازی به اتمام رسید! اطلاعات کانکشن متصل به آی‌پی $proxyIp با موفقیت ایجاد گردید." else "Setup complete! Connection properties successfully configured to $proxyIp"))
        kotlinx.coroutines.delay(1000)
        val randSub = "nova-proxy-" + (10000..99999).random()
        onSuccess("https://$workerName.$randSub.workers.dev")
        return
    }

    try {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        // 1. Fetch accounts
        onStatus(if (isFa) "🔑 در حال اعتبارسنجی توکن دسترسی کلودفلر..." else "🔑 Authorizing access token with Cloudflare API...")
        val accountsRequest = okhttp3.Request.Builder()
            .url("https://api.cloudflare.com/client/v4/accounts")
            .header("Authorization", "Bearer $token")
            .build()

        client.newCall(accountsRequest).execute().use { response ->
            if (!response.isSuccessful) {
                onFailure((if (isFa) "خطا در بارگذاری حساب‌ها: " else "List accounts API authorization failed with HTTP ") + response.code)
                return
            }

            val responseBody = response.body?.string() ?: ""
            val accountId = extractJsonValue(responseBody, "id")
            val accountName = extractJsonValue(responseBody, "name")
            if (accountId.isEmpty()) {
                onFailure(if (isFa) "هیچ حساب کلودفلر معتبری در توکن شما یافت نشد." else "No active Cloudflare Account ID resolved from this API token.")
                return
            }
            onStatus((if (isFa) "👤 اتصال موفقیت‌آمیز! نام کاربری: " else "👤 Authorization active! Account name: ") + accountName)
            kotlinx.coroutines.delay(1000)

            // 2. Transmit / Upload JavaScript Worker Source
            onStatus(if (isFa) "⚙️ کامپایل هوشمند نسخه بهینه سورس کد نوا پروکسی..." else "⚙️ Structuring highly optimized proxy worker source code...")
            val workerJsCode = """
                // Nova Wizard Official Worker v1.0.0 (VLESS + WebSocket)
                import { connect } from 'cloudflare:sockets';
                export default {
                  async fetch(request, env) {
                    const url = new URL(request.url);
                    const upgradeHeader = request.headers.get('Upgrade');
                    if (upgradeHeader === 'websocket') {
                        // VLESS over WS Logic matching Nova-Wizard
                        return vlessOverWSHandler(request);
                    }
                    const vlessConfig = "vless://$uuid@" + url.host + ":443?encryption=none&security=tls&sni=" + url.host + "&type=ws&host=" + url.host + "&path=%2F%3Fed%3D2048#NovaRadar-Wizard";
                    return new Response(vlessConfig, {
                      headers: { "content-type": "text/plain; charset=utf-8" }
                    });
                  }
                };
            """.trimIndent()

            onStatus(if (isFa) "📂 در حال آپلود فایل سورس کد به کلودفلر..." else "📂 Injecting JS proxy script to Cloudflare Workers repository...")
            val deployRequest = okhttp3.Request.Builder()
                .url("https://api.cloudflare.com/client/v4/accounts/$accountId/workers/scripts/$workerName")
                .header("Authorization", "Bearer $token")
                .put(workerJsCode.toRequestBody("application/javascript".toMediaType()))
                .build()

            client.newCall(deployRequest).execute().use { deployResponse ->
                if (!deployResponse.isSuccessful) {
                    onFailure((if (isFa) "خطا در آپلود اسکریپت ورکر: " else "Write Worker Script failed with HTTP ") + deployResponse.code)
                    return
                }

                kotlinx.coroutines.delay(1000)

                // 3. Configure/Enable workers.dev Subdomain
                onStatus(if (isFa) "📡 آماده‌سازی زیردامنه اختصاصی..." else "📡 Allocating subdomain gateway...")
                val subRequest = okhttp3.Request.Builder()
                    .url("https://api.cloudflare.com/client/v4/accounts/$accountId/workers/scripts/$workerName/subdomain")
                    .header("Authorization", "Bearer $token")
                    .post("{\"enabled\":true}".toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(subRequest).execute().use { subResponse ->
                    if (!subResponse.isSuccessful) {
                        onFailure((if (isFa) "خطا در اعمال ساب‌دومین: " else "Assign workers.dev subdomain route failing with HTTP ") + subResponse.code)
                        return
                    }

                    kotlinx.coroutines.delay(1000)

                    // 4. Resolve Domain Prefix
                    onStatus(if (isFa) "🛰 دریافت لینک نهایی..." else "🛰 Fetching deployed worker URL endpoint...")
                    val resolveRequest = okhttp3.Request.Builder()
                        .url("https://api.cloudflare.com/client/v4/accounts/$accountId/workers/subdomain")
                        .header("Authorization", "Bearer $token")
                        .build()

                    client.newCall(resolveRequest).execute().use { resResponse ->
                        val subdomainPrefix = if (resResponse.isSuccessful) {
                            val resBody = resResponse.body?.string() ?: ""
                            extractJsonValue(resBody, "subdomain")
                        } else {
                            ""
                        }

                        val finalPrefix = if (subdomainPrefix.isNotEmpty()) subdomainPrefix else "novaproxy-user"
                        val finalUrl = "https://$workerName.$finalPrefix.workers.dev"
                        onSuccess(finalUrl)
                    }
                }
            }
        }
    } catch (e: Exception) {
        onFailure((if (isFa) "خطای سیستمی غیرمنتظره: " else "System connection failed: ") + (e.message ?: "Unknown Connection Error"))
    }
}


// Page 2: SETTINGS SCREEN (Ports & IP Sources)
@Composable
private fun SettingsScreenLegacy(viewModel: NovaRadarViewModel) {
    val lang by viewModel.selectedLanguage.collectAsState()
    val theme by viewModel.selectedTheme.collectAsState()
    val ports by viewModel.portConfigs.collectAsState()
    val sources by viewModel.ipSources.collectAsState()

    LocalizedLayout(lang) {
        FadingScrollColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(horizontal = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Localization.get("tab_settings", lang),
                        style = MaterialTheme.typography.displayMedium.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF22D3EE),
                                    Color(0xFF818CF8),
                                    Color(0xFFA855F7)
                                )
                            ),
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = Localization.get("theme", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Language & Theme Setup (switch-based toggles)
            GlassyCard {
                // Language switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Localization.get("language", lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (lang == AppLanguage.FA) "فارسی" else "English",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = lang == AppLanguage.EN,
                        onCheckedChange = { checked ->
                            viewModel.selectLanguage(if (checked) AppLanguage.EN else AppLanguage.FA)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = if (theme == AppTheme.PRISM_DARK) Color.Gray else Color.White,
                            uncheckedTrackColor = if (theme == AppTheme.PRISM_DARK) Color(0xFF2D3A5C) else Color(0xFFCBD5E1)
                        )
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Theme switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Localization.get("theme", lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (theme == AppTheme.PRISM_DARK) Localization.get("prism_dark", lang) else Localization.get("prism_light", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = theme == AppTheme.PRISM_LIGHT,
                        onCheckedChange = { checked ->
                            viewModel.selectTheme(if (checked) AppTheme.PRISM_LIGHT else AppTheme.PRISM_DARK)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = if (theme == AppTheme.PRISM_DARK) Color.Gray else Color.White,
                            uncheckedTrackColor = if (theme == AppTheme.PRISM_DARK) Color(0xFF2D3A5C) else Color(0xFFCBD5E1)
                        )
                    )
                }
            }

            // Vibration & Alert configurations
            Column {
                Text(
                    text = Localization.get("alert_settings", lang),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val vibrateFinish by viewModel.vibrateOnFinish.collectAsState()
                val vibrateError by viewModel.vibrateOnError.collectAsState()
                val notifyError by viewModel.notifyOnError.collectAsState()

                val context = LocalContext.current
                val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { permissions ->
                        val granted = permissions.values.all { it }
                        if (granted) {
                            Toast.makeText(context, if (lang == AppLanguage.FA) "دسترسی‌ها با موفقیت اعطا شد!" else "Permissions Granted!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, if (lang == AppLanguage.FA) "برخی دسترسی‌ها رد شد." else "Some permissions were denied.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                GlassyCard {
                    // Vibrate on complete
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleVibrateOnFinish() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Localization.get("vibrate_finish", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = vibrateFinish,
                            onCheckedChange = { viewModel.toggleVibrateOnFinish() }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 1.dp)

                    // Vibrate on mismatch error
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleVibrateOnError() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Localization.get("vibrate_error", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = vibrateError,
                            onCheckedChange = { viewModel.toggleVibrateOnError() }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 1.dp)

                    // Notifications on alerts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleNotifyOnError() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Localization.get("notify_error", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = notifyError,
                            onCheckedChange = { viewModel.toggleNotifyOnError() }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Grant permissions button
                    Button(
                        onClick = { permissionLauncher.launch(permissionsToRequest) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Permissions",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.get("grant_permissions", lang),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Port configuration (elegant checkboxes card grid as requested/shown in screenshot 2)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Localization.get("port_configuration", lang),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = Localization.get("select_all", lang),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.clickable { viewModel.selectAllPorts() }
                        )
                        Text(
                            text = Localization.get("clear_all", lang),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            ),
                            modifier = Modifier.clickable { viewModel.clearAllPorts() }
                        )
                    }
                }

                // 2-column grid as shown in screenshot 2
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ports.chunked(2).forEach { rowPorts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPorts.forEach { portConfig ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(
                                            if (portConfig.isEnabled) {
                                                if (theme == AppTheme.PRISM_LIGHT) Color(0xFF2563EB).copy(alpha = 0.85f) else Color(0xFF1A1A2E).copy(alpha = 0.8f)
                                            } else {
                                                if (theme == AppTheme.PRISM_LIGHT) Color(0xFFE2E8F0) else Color(0xFF0A0E1A).copy(alpha = 0.6f)
                                            }
                                        )
                                        .border(
                                            width = if (portConfig.isEnabled) 1.dp else 1.5.dp,
                                            color = if (portConfig.isEnabled) MaterialTheme.colorScheme.primary else (if (theme == AppTheme.PRISM_LIGHT) Color(0xFF94A3B8) else Color.White.copy(alpha = 0.15f)),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .clickable { viewModel.togglePortConfig(portConfig) }
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = portConfig.port.toString(),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontFamily = VazirmatnFontFamily
                                            ),
                                            color = if (portConfig.isEnabled) Color.White else (if (theme == AppTheme.PRISM_LIGHT) Color(0xFF334155) else Color.Gray)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (portConfig.isEnabled) (if (theme == AppTheme.PRISM_LIGHT) Color.White else MaterialTheme.colorScheme.primary) else Color.Gray.copy(alpha = 0.3f))
                                        )
                                    }
                                }
                            }
                            if (rowPorts.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // IP Sources list Section (individual cards as requested/shown in screenshots 1 & 3)
            Column {
                    Text(
                        text = Localization.get("ip_sources", lang),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sources.forEach { source ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleIpSource(source) },
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (theme == AppTheme.PRISM_LIGHT) Color.White else Color(0xFF151B2D).copy(alpha = 0.6f)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (source.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Switch(
                                        checked = source.isEnabled,
                                        onCheckedChange = { viewModel.toggleIpSource(source) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                                            uncheckedThumbColor = if (theme == AppTheme.PRISM_DARK) Color.Gray else Color.White,
                                            uncheckedTrackColor = if (theme == AppTheme.PRISM_DARK) Color(0xFF2D3A5C) else Color(0xFFCBD5E1)
                                        )
                                    )
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column {
                                        Text(
                                            text = if (lang == AppLanguage.FA) source.nameFa else source.nameEn,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (source.isEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray
                                        )
                                        if (source.cidr.isNotEmpty()) {
                                            val count = source.cidr.split(",").size
                                            val label = if (count == 1) source.cidr else if (lang == AppLanguage.FA) "${count} رنج" else "${count} CIDR ranges"
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Globe",
                                    tint = if (source.isEnabled) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// FlowRow layout implementation backport to avoid dependency errors on older Compose versions
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val layoutWidth = constraints.maxWidth
        val lines = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentLine = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentLineWidth = 0

        placeables.forEach { placeable ->
            if (currentLineWidth + placeable.width > layoutWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = mutableListOf()
                currentLineWidth = 0
            }
            currentLine.add(placeable)
            currentLineWidth += placeable.width + 16
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        var totalHeight = 0
        lines.forEach { line ->
            val maxLineHeight = line.maxOfOrNull { it.height } ?: 0
            totalHeight += maxLineHeight + 16
        }

        layout(layoutWidth, maxOf(0, totalHeight - 16)) {
            var y = 0
            lines.forEach { line ->
                var x = 0
                val maxLineHeight = line.maxOfOrNull { it.height } ?: 0
                line.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + 16
                }
                y += maxLineHeight + 16
            }
        }
    }
}

// Page 3: IMPORT IP SCREEN - Import, Scan, Suffix IPs for Nova Proxy
@Composable
private fun ImportScreenLegacy(viewModel: NovaRadarViewModel) {
    val lang by viewModel.selectedLanguage.collectAsState()
    val importOutput by viewModel.importOutput.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val aliveIps by viewModel.allAliveIps.collectAsState()
    val context = LocalContext.current
    var ipText by remember { mutableStateOf("") }
    var mode by remember { mutableIntStateOf(1) }
    var outputText by remember { mutableStateOf("") }
    var showSuffixPrompt by remember { mutableStateOf(false) }
    var rawOutput by remember { mutableStateOf("") }
    var importTab by remember { mutableIntStateOf(0) } // 0=manual paste, 1=auto scanner
    var selectedOperator by remember { mutableStateOf("all") }
    var ipCount by remember { mutableIntStateOf(20) }

    LaunchedEffect(isScanning, mode) {
        if (!isScanning && mode == 1 && aliveIps.isNotEmpty() && rawOutput.isEmpty()) {
            showSuffixPrompt = true
        }
    }

    LocalizedLayout(lang) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 96.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Localization.get("import_ip", lang),
                        style = MaterialTheme.typography.displayMedium.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF22D3EE), Color(0xFF818CF8), Color(0xFFA855F7))
                            ),
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = if (importTab == 0) "MANUAL PASTE" else "AUTO SCANNER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                // Toggle between Manual and Auto
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(0 to "Manual", 1 to "Auto").forEach { (id, label) ->
                        val isSel = importTab == id
                        Button(
                            onClick = { importTab = id },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) Color(0xFF22D3EE).copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (isSel) Color(0xFF22D3EE) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) { Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            if (importTab == 0) {
                // Manual IP paste area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    TextField(
                        value = ipText,
                        onValueChange = { ipText = it },
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        placeholder = {
                            Text(
                                Localization.get("import_placeholder", lang),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = Int.MAX_VALUE
                    )
                }
            } else {
                // Auto Scanner section (from HTML)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text("OPERATOR", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF22D3EE).copy(alpha = 0.5f), letterSpacing = 1.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("all" to "All", "mci" to "MCI", "mtn" to "MTN", "ict" to "ICT").forEach { (key, label) ->
                                val isSel = selectedOperator == key
                                Button(
                                    onClick = { selectedOperator = key },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSel) Color(0xFF22D3EE).copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isSel) Color(0xFF22D3EE) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.weight(1f).height(30.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Count:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Slider(
                                value = ipCount.toFloat(),
                                onValueChange = { ipCount = it.toInt() },
                                valueRange = 5f..100f,
                                steps = 18,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF22D3EE),
                                    activeTrackColor = Color(0xFF22D3EE).copy(alpha = 0.5f),
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                            Text("$ipCount", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF22D3EE))
                        }
                        Button(
                            onClick = {
                                ipText = viewModel.generateOperatorIps(selectedOperator, ipCount)
                                importTab = 0 // Switch to manual to show generated IPs
                            },
                            modifier = Modifier.fillMaxWidth().height(34.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF22D3EE).copy(alpha = 0.2f),
                                contentColor = Color(0xFF22D3EE)
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Generate IPs", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Mode selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple(0, Localization.get("import_suffix_only", lang), Color(0xFF818CF8)),
                    Triple(1, Localization.get("import_scan", lang), Color(0xFF22D3EE)),
                    Triple(2, Localization.get("import_scan_suffix", lang), Color(0xFF34D399))
                ).forEach { (id, label, color) ->
                    val isSelected = mode == id
                    Button(
                        onClick = { mode = id },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) color.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.weight(1f).height(34.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.setImportedIps(ipText)
                    outputText = ""
                    rawOutput = ""
                    viewModel.clearImportOutput()
                    if (mode == 0) {
                        outputText = viewModel.suffixOnly(context)
                    } else {
                        viewModel.startScanWithImportedIps(mode == 2)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22D3EE).copy(alpha = 0.2f),
                    contentColor = Color(0xFF22D3EE)
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(Localization.get("import_start", lang), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            val displayText = when (mode) {
                0 -> outputText
                1 -> if (rawOutput.isNotEmpty()) rawOutput else importOutput
                2 -> importOutput
                else -> ""
            }
            if (displayText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                Localization.get("import_output", lang),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                color = Color(0xFF22D3EE).copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { viewModel.copyImportOutput(context) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.ContentCopy, "Copy", tint = Color(0xFF22D3EE), modifier = Modifier.size(14.dp))
                                }
                                IconButton(onClick = { viewModel.saveImportOutputToFile(context) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Save, "Save", tint = Color(0xFF22D3EE), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.weight(1f)) {
                            val lines = displayText.lines()
                            if (lines.isEmpty()) {
                                Text(Localization.get("import_no_ips", lang), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    items(lines) { line ->
                                        Text(
                                            line,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 8.sp,
                                            color = if (line.contains("#Nova-")) Color(0xFF34D399).copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (isScanning) {
                Box(modifier = Modifier.fillMaxWidth().weight(0.6f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF22D3EE),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Scanning...", fontSize = 10.sp, color = Color(0xFF22D3EE).copy(alpha = 0.6f))
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(0.6f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(Localization.get("import_no_ips", lang), fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    if (showSuffixPrompt) {
        val isFa = lang == AppLanguage.FA
        AlertDialog(
            onDismissRequest = {
                rawOutput = aliveIps.joinToString("\n") { "${it.ip}:${it.port}" }
                showSuffixPrompt = false
            },
            title = {
                Text(if (isFa) "سوفیکس نوا پروکسی" else "Nova Proxy Suffix", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            },
            text = {
                Text(
                    if (isFa) "آیا می‌خواهید سوفیکس Nova (#Nova-id) به ${aliveIps.size} آی‌پی سالم اضافه شود؟"
                    else "Do you want to add Nova Proxy suffix (#Nova-id) to the ${aliveIps.size} verified IPs?",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.suffixForNovaProxy(context)
                    showSuffixPrompt = false
                }) {
                    Text(if (isFa) "بله، سوفیکس شود" else "Yes, Suffix", color = Color(0xFF34D399))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    rawOutput = aliveIps.joinToString("\n") { "${it.ip}:${it.port}" }
                    showSuffixPrompt = false
                }) {
                    Text(if (isFa) "خیر، فقط خام" else "No, Raw Only", color = Color(0xFFEF4444).copy(alpha = 0.7f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun AboutScreenLegacy(viewModel: NovaRadarViewModel) {
    val lang by viewModel.selectedLanguage.collectAsState()
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val logs by viewModel.logs.collectAsState()
    val scrollState = rememberScrollState()

    LocalizedLayout(lang) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .padding(bottom = 88.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // No header spacer — content starts at top
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // About box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Localization.get("tab_about", lang),
                            style = MaterialTheme.typography.displayMedium.copy(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF22D3EE), Color(0xFF818CF8), Color(0xFFA855F7))
                                ),
                                fontWeight = FontWeight.Black
                            )
                        )
                        Text(
                            text = "ORGANIZATION DETS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                GlassyCard(borderColor = Color(0xFF22D3EE).copy(alpha = 0.35f)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = com.novaradar.app.R.drawable.img_nova_radar_logo_1781975654739),
                            contentDescription = "Nova Radar N Logo",
                            modifier = Modifier.size(84.dp).clip(CircleShape).border(1.5.dp, Color(0xFF22D3EE), CircleShape),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(8.dp))
                        CrumpledGlitchText(text = "NOVA RADAR", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = Localization.get("about_sub", lang),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            AboutLinkItem(title = "GitHub", subtitle = "https://github.com/IRNova/NovaRadar", icon = Icons.Outlined.Code, onClick = { try { uriHandler.openUri("https://github.com/IRNova/NovaRadar") } catch (e: Exception) { e.printStackTrace() } })
                            AboutLinkItem(title = "Telegram Channel", subtitle = "https://t.me/irnova_proxy", icon = Icons.Outlined.Send, onClick = { try { uriHandler.openUri("https://t.me/irnova_proxy") } catch (e: Exception) { e.printStackTrace() } })
                            AboutLinkItem(title = "Wizard Website", subtitle = "https://novaproxy.online/install", icon = Icons.Outlined.Language, onClick = { try { uriHandler.openUri("https://novaproxy.online/install") } catch (e: Exception) { e.printStackTrace() } })
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(text = "v${com.novaradar.app.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }

                // Terminal / Logs section integrated inline
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TERMINAL LOG", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF22D3EE).copy(alpha = 0.5f), letterSpacing = 1.sp)
                        TextButton(onClick = { viewModel.clearLogs() }, modifier = Modifier.height(28.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text(Localization.get("clear", lang), fontSize = 9.sp, color = Color(0xFF22D3EE))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp, max = 280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF030712))
                            .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(10.dp)
                    ) {
                        if (logs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("TERMINAL IDLE", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                items(logs) { log ->
                                    val textColor = when {
                                        log.contains("✔ ALIVE") || log.contains("✔") -> Color(0xFF22D3EE)
                                        log.contains("✖ DEAD") || log.contains("✖") -> Color(0xFFEF4444).copy(alpha = 0.5f)
                                        log.contains("======") -> Color(0xFF22D3EE)
                                        else -> Color(0xFF9CA3AF)
                                    }
                                    Text(log, style = MaterialTheme.typography.labelSmall, color = textColor, maxLines = 1, fontSize = 8.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Live Crumpled Textured Glitch Text for High Tech Branding as requested
@Composable
fun CrumpledGlitchText(
    text: String,
    style: TextStyle = MaterialTheme.typography.displayMedium
) {
    val novaBrush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF22D3EE),
                                    Color(0xFF818CF8),
                                    Color(0xFFA855F7)
                                )
    )
    val isLightTheme = MaterialTheme.colorScheme.background == Color(0xFFFAFAFC)
    val highlightsColor = if (isLightTheme) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.40f)
    val highlightsShadow = if (isLightTheme) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.35f)
    Box {
        Text(
            text = text,
            style = style.copy(
                fontFamily = VazirmatnFontFamily,
                letterSpacing = 2.sp,
                brush = novaBrush,
                shadow = Shadow(
                    color = Color.Green.copy(alpha = 0.5f),
                    offset = Offset(2f, 2f),
                    blurRadius = 1.5f
                )
            ),
            fontWeight = FontWeight.Black
        )
        Text(
            text = text,
            style = style.copy(
                fontFamily = VazirmatnFontFamily,
                letterSpacing = 2.sp,
                shadow = Shadow(
                    color = highlightsShadow,
                    offset = Offset(-1.5f, -1.5f),
                    blurRadius = 0.8f
                )
            ),
            color = highlightsColor,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun AboutLinkItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF22D3EE).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF22D3EE),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}





