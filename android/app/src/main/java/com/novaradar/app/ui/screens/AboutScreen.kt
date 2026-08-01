package com.novaradar.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaradar.app.ui.components.Wc
import com.novaradar.app.ui.components.WidgetCard
import com.novaradar.app.ui.localization.Localization
import com.novaradar.app.ui.viewmodel.AppTheme
import com.novaradar.app.ui.viewmodel.NovaRadarViewModel

@Composable
fun AboutScreen(viewModel: NovaRadarViewModel) {
    val lang by viewModel.selectedLanguage.collectAsState()
    val theme by viewModel.selectedTheme.collectAsState()
    val isLight = theme == AppTheme.PRISM_LIGHT
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val txtPrimary = if (isLight) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val txtSecondary = if (isLight) Color(0xFF475569) else Color(0xFFE2E8F0)

    Column(Modifier.fillMaxSize().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        WidgetCard(isLightTheme = isLight, modifier = Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Image(
                    painter = painterResource(id = com.novaradar.app.R.drawable.img_nova_radar_logo_1781975654739),
                    contentDescription = "Logo",
                    modifier = Modifier.size(80.dp).clip(CircleShape).border(2.dp, Wc.primary.copy(alpha = 0.5f), CircleShape),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(12.dp))
                Text("NOVA RADAR", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Wc.primary)
                Text(Localization.get("about_sub", lang), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = txtSecondary)
                Spacer(Modifier.height(8.dp))
                Text("v${com.novaradar.app.BuildConfig.VERSION_NAME}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = txtSecondary.copy(alpha = 0.7f))
            }
        }

        WidgetCard(isLightTheme = isLight, modifier = Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LINKS", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Wc.primary, letterSpacing = 2.sp)
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    AboutLink("GitHub", "github.com/IRNova/NovaRadar", Icons.Outlined.Code, isLight, txtPrimary, txtSecondary) { try { uriHandler.openUri("https://github.com/IRNova/NovaRadar") } catch (_: Exception) {} }
                    AboutLink("Telegram", "t.me/irnova_proxy", Icons.Outlined.Send, isLight, txtPrimary, txtSecondary) { try { uriHandler.openUri("https://t.me/irnova_proxy") } catch (_: Exception) {} }
                    AboutLink("Website", "novaproxy.online/install", Icons.Outlined.Language, isLight, txtPrimary, txtSecondary) { try { uriHandler.openUri("https://novaproxy.online/install") } catch (_: Exception) {} }
                }
            }
        }
    }
}

@Composable
private fun AboutLink(title: String, subtitle: String, icon: ImageVector, isLight: Boolean, txtPrimary: Color, txtSecondary: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isLight) Color(0xFFF1F5F9) else Color(0xFF151B2D).copy(alpha = 0.6f)).border(0.5.dp, if (isLight) Color(0xFFCBD5E1).copy(alpha = 0.5f) else Color(0xFF2D3A5C).copy(alpha = 0.5f), RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(Wc.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Wc.primary, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Column { Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = txtPrimary); Spacer(Modifier.height(2.dp)); Text(subtitle, fontSize = 10.sp, color = txtSecondary) }
    }
}
