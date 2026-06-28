package com.novaradar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novaradar.app.ui.localization.Localization
import com.novaradar.app.ui.screens.*
import com.novaradar.app.ui.theme.NovaRadarTheme
import com.novaradar.app.ui.viewmodel.AppLanguage
import com.novaradar.app.ui.viewmodel.AppTheme
import com.novaradar.app.ui.viewmodel.NovaRadarViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: NovaRadarViewModel = viewModel()
            val theme by viewModel.selectedTheme.collectAsState()
            val lang by viewModel.selectedLanguage.collectAsState()

            NovaRadarTheme(theme = theme) {
                val view = androidx.compose.ui.platform.LocalView.current
                val isLightTheme = theme == AppTheme.PRISM_LIGHT
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as android.app.Activity).window
                        window.statusBarColor = android.graphics.Color.TRANSPARENT
                        window.navigationBarColor = android.graphics.Color.TRANSPARENT
                        val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
                        controller.isAppearanceLightStatusBars = isLightTheme
                        controller.isAppearanceLightNavigationBars = isLightTheme
                    }
                }

                val isDark = theme == AppTheme.PRISM_DARK
                val meshColors = if (isDark) {
                    listOf(Color(0xFF0A0E1A), Color(0xFF0F1A3A), Color(0xFF060A15))
                } else {
                    listOf(Color(0xFFFFFFFF), Color(0xFFEFF6FF), Color(0xFFF8FAFC))
                }
                val meshGradient = Brush.linearGradient(colors = meshColors)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(meshGradient)
                ) {
                    MainAppLayout(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppLayout(viewModel: NovaRadarViewModel) {
    val lang by viewModel.selectedLanguage.collectAsState()
    val theme by viewModel.selectedTheme.collectAsState()
    val isLightTheme = theme == AppTheme.PRISM_LIGHT
    val isDark = !isLightTheme
    val pagerState = rememberPagerState(initialPage = 2, pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    val headerHeight = 68.dp
    val navBarHeight = 68.dp
    val fadeHeight = 100.dp

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Main content pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("main_horizontal_pager")
            ) { page ->
                when (page) {
                    0 -> EasyInstallerScreen(viewModel)
                    1 -> SettingsScreen(viewModel)
                    2 -> RadarScreen(viewModel)
                    3 -> ImportScreen(viewModel)
                    4 -> AboutScreen(viewModel)
                }
            }

            // Top fade overlay: content fades as it scrolls under the header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fadeHeight + 20.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                if (isDark) Color(0xFF0A0E1A) else Color(0xFFF5F7FA),
                                (if (isDark) Color(0xFF0A0E1A) else Color(0xFFF5F7FA)).copy(alpha = 0.95f),
                                (if (isDark) Color(0xFF0A0E1A) else Color(0xFFF5F7FA)).copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Bottom fade overlay: content fades as it scrolls under the nav bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fadeHeight + 40.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                (if (isDark) Color(0xFF0A0E1A) else Color(0xFFF5F7FA)).copy(alpha = 0.7f),
                                (if (isDark) Color(0xFF0A0E1A) else Color(0xFFF5F7FA)).copy(alpha = 0.95f),
                                if (isDark) Color(0xFF0A0E1A) else Color(0xFFF5F7FA)
                            )
                        )
                    )
            )

            // Glassmorphic Top Header
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp, start = 14.dp, end = 14.dp)
                    .height(headerHeight)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = if (isDark) Color(0xFF151B2D).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.65f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isDark) Color(0xFF4DA8FF).copy(alpha = 0.25f) else Color(0xFF2563EB).copy(alpha = 0.2f)
                ),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_nova_radar_logo_1781975654739),
                        contentDescription = "Nova Radar Logo",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "NOVA RADAR",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color(0xFF1E293B),
                                letterSpacing = 1.2.sp
                            )
                        )
                        Text(
                            text = "NOVA PROXY GROUP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (isDark) Color(0xFF4DA8FF).copy(alpha = 0.8f) else Color(0xFF2563EB).copy(alpha = 0.8f),
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            // Floating Navigation Bar with glassmorphism
            val isScanning by viewModel.isScanning.collectAsState()
            val pulseAnim by rememberInfiniteTransition().animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                label = "navPulse"
            )
            NavigationBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 12.dp, start = 14.dp, end = 14.dp)
                    .height(navBarHeight)
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        width = 1.5.dp,
                        color = if (isDark) Color(0xFF2D3A5C).copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(32.dp)
                    ),
                containerColor = if (isDark) Color(0xFF151B2D).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f),
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    NavigationItemData(key = "tab_installer", selectedIcon = Icons.Filled.Download, unselectedIcon = Icons.Outlined.Download),
                    NavigationItemData(key = "tab_settings", selectedIcon = Icons.Filled.Settings, unselectedIcon = Icons.Outlined.Settings),
                    NavigationItemData(key = "tab_radar", selectedIcon = Icons.Filled.Radar, unselectedIcon = Icons.Outlined.Radar),
                    NavigationItemData(key = "tab_import", selectedIcon = Icons.Filled.Add, unselectedIcon = Icons.Outlined.Add),
                    NavigationItemData(key = "tab_about", selectedIcon = Icons.Filled.Info, unselectedIcon = Icons.Outlined.Info)
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = pagerState.currentPage == index
                        val isRadar = index == 2

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(if (isRadar) 56.dp else 44.dp)
                                ) {
                                    if (isSelected && !isRadar) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isDark) Color.White.copy(alpha = 0.08f)
                                                    else Color(0xFF2563EB).copy(alpha = 0.08f)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isDark) Color.White.copy(alpha = 0.12f)
                                                    else Color(0xFF2563EB).copy(alpha = 0.12f),
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                    if (isRadar) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isScanning)
                                                        Brush.linearGradient(listOf(Color(0xFFBE123C), Color(0xFF9F1239)))
                                                    else
                                                        Brush.linearGradient(listOf(Color(0xFFDC2626), Color(0xFF991B1B)))
                                                )
                                                .border(2.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFF2563EB).copy(alpha = 0.2f), CircleShape)
                                                .clickable {
                                                    if (isScanning) {
                                                        viewModel.stopScan()
                                                    } else {
                                                        viewModel.startScan()
                                                        // Auto-navigate to Radar screen (Index 2)
                                                        coroutineScope.launch {
                                                            pagerState.animateScrollToPage(2)
                                                        }
                                                    }
                                                }
                                                .testTag("nav_start_button"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isScanning) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(60.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFBE123C).copy(alpha = pulseAnim * 0.25f))
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.PowerSettingsNew,
                                                contentDescription = "Start/Stop",
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = Localization.get(item.key, lang),
                                            tint = if (isSelected) {
                                                if (isDark) Color(0xFF4DA8FF) else Color(0xFF2563EB)
                                            } else {
                                                if (isDark) Color(0xFFE2E8F0).copy(alpha = 0.4f)
                                                else Color(0xFF334155).copy(alpha = 0.4f)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            },
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.testTag("nav_item_${item.key}")
                        )
                    }
                }
            }
        }
    }
}

data class NavigationItemData(
    val key: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)
