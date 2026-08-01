package com.novaradar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novaradar.app.ui.localization.Localization
import com.novaradar.app.ui.screens.*
import com.novaradar.app.ui.theme.*
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

            LaunchedEffect(Unit) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
            }

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

                val meshColors = if (theme == AppTheme.PRISM_DARK) {
                    listOf(Color(0xFF060B18), Color(0xFF0A1428), Color(0xFF040812))
                } else {
                    listOf(Color(0xFFF1F5F9), Color(0xFFE8EDF4), Color(0xFFF8FAFC))
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(colors = meshColors))
                ) {
                    MainAppLayout(viewModel)
                }
            }
        }
    }
}

data class NavTab(
    val key: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isCenter: Boolean = false
)

@Composable
fun MainAppLayout(viewModel: NovaRadarViewModel) {
    val lang by viewModel.selectedLanguage.collectAsState()
    val theme by viewModel.selectedTheme.collectAsState()
    val isDark = theme == AppTheme.PRISM_DARK
    val isScanning by viewModel.isScanning.collectAsState()
    val pagerState = rememberPagerState(initialPage = 2, pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    val isLight = !isDark

    val tabs = listOf(
        NavTab("tab_settings", Icons.Filled.Settings, Icons.Outlined.Settings),
        NavTab("tab_import", Icons.Filled.Download, Icons.Outlined.Download),
        NavTab("tab_radar", Icons.Filled.Radar, Icons.Outlined.Radar, isCenter = true),
        NavTab("tab_installer", Icons.Filled.Bolt, Icons.Outlined.Bolt),
        NavTab("tab_about", Icons.Filled.Info, Icons.Outlined.Info)
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Box(Modifier.weight(1f)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                            .padding(top = 6.dp)
                    ) {
                        when (page) {
                            0 -> SettingsScreen(viewModel)
                            1 -> ImportScreen(viewModel)
                            2 -> RadarScreen(viewModel)
                            3 -> EasyInstallerScreen(viewModel)
                            4 -> AboutScreen(viewModel)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .padding(horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .align(Alignment.BottomCenter)
                        .shadow(20.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (isDark) Color(0xFF0A0E1A).copy(alpha = 0.97f)
                            else Color(0xFFF8FAFC).copy(alpha = 0.97f)
                        )
                        .border(
                            0.5.dp,
                            if (isDark) Color(0xFF1E3A5F).copy(alpha = 0.3f)
                            else Color(0xFFCBD5E1).copy(alpha = 0.3f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(bottom = 4.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(68.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val selected = pagerState.currentPage == index

                            if (tab.isCenter) {
                                Spacer(Modifier.width(64.dp))
                            }

                            if (!tab.isCenter) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                        .padding(vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (selected) {
                                                    if (isDark) DarkPrimary.copy(alpha = 0.12f)
                                                    else LightPrimary.copy(alpha = 0.1f)
                                                } else Color.Transparent
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Icon(
                                            if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = Localization.get(tab.key, lang),
                                            tint = when {
                                                selected -> if (isDark) DarkPrimary else LightPrimary
                                                else -> if (isDark) Color.White.copy(alpha = 0.45f)
                                                else Color(0xFF1E293B).copy(alpha = 0.4f)
                                            },
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        Localization.get(tab.key, lang),
                                        fontSize = 8.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            selected -> if (isDark) DarkPrimary else LightPrimary
                                            else -> if (isDark) Color.White.copy(alpha = 0.4f)
                                            else Color(0xFF1E293B).copy(alpha = 0.35f)
                                        }
                                    )
                                }
                            }

                            if (tab.isCenter) {
                                Spacer(Modifier.width(64.dp))
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 0.dp)
                        .size(60.dp)
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                if (isScanning) listOf(Color(0xFFBE123C), Color(0xFF9F1239))
                                else listOf(Color(0xFF0D7DB3), Color(0xFF065A8C))
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (isScanning) viewModel.stopScan()
                            else {
                                viewModel.startScan()
                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        val pulse by rememberInfiniteTransition().animateFloat(
                            initialValue = 1f, targetValue = 1.4f,
                            animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                            label = "pulse"
                        )
                        Box(
                            modifier = Modifier
                                .size(60.dp * pulse)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                    }
                    Icon(
                        if (isScanning) Icons.Default.Stop else Icons.Default.Radar,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
