package com.sijui.charades.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sijui.charades.R
import com.sijui.charades.data.SijuiApiClient
import com.sijui.charades.ui.design.AuroraBackground
import com.sijui.charades.ui.design.FloatingEmoji
import com.sijui.charades.ui.design.GlassCard
import com.sijui.charades.ui.design.KenyaBeamBorder
import com.sijui.charades.ui.design.SectionLabel
import com.sijui.charades.ui.design.ShukaStrip
import com.sijui.charades.ui.design.Sj
import com.sijui.charades.ui.design.SjGradients
import com.sijui.charades.ui.design.SjPill
import com.sijui.charades.ui.design.bounceClick
import com.sijui.charades.ui.design.sjTextFieldColors
import com.sijui.charades.ui.design.staggerIn
import com.sijui.charades.ui.theme.ManlineDisplayFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    api: SijuiApiClient,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onOpenCharades: () -> Unit,
    onOpenNani: () -> Unit,
) {
    var serverOnline by remember { mutableStateOf<Boolean?>(null) }
    var showServerDialog by remember { mutableStateOf(false) }
    var showHowTo by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf(api.baseUrl) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        serverOnline = null
        serverOnline = api.pingHealth()
    }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // Top bar: avatar greeting + server status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggerIn(0),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.sijui_logo),
                    contentDescription = "Sijui logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(2.dp, Sj.Gold, CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Habari! 👋", color = Sj.TextMid, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Tucheze leo?", color = Sj.TextHi, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                }
                ServerStatusPill(online = serverOnline, onClick = { showServerDialog = true })
            }

            Spacer(Modifier.height(30.dp))

            // Hero wordmark
            Column(modifier = Modifier.staggerIn(1)) {
                Text(
                    text = "SIJUI",
                    style = TextStyle(
                        brush = Brush.linearGradient(listOf(Sj.Gold, Sj.Coral, Sj.Orchid)),
                        fontFamily = ManlineDisplayFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 84.sp,
                        lineHeight = 84.sp,
                        letterSpacing = (-1).sp,
                    ),
                )
                Text(
                    "The ultimate Kenyan party game.",
                    color = Sj.TextMid,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(14.dp))
                ShukaStrip(
                    modifier = Modifier
                        .width(176.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    height = 11.dp,
                )
            }

            Spacer(Modifier.height(30.dp))
            SectionLabel("Chagua mchezo  •  Pick a game", modifier = Modifier.staggerIn(2))
            Spacer(Modifier.height(14.dp))

            GameModeCard(
                kicker = "Heads Up! style",
                title = "Charades",
                description = "Phone on forehead. Friends act it out. Tilt to score.",
                emoji = "🎭",
                tags = listOf("OFFLINE", "TEAMS"),
                colors = SjGradients.Sunset,
                onClick = onOpenCharades,
                modifier = Modifier.staggerIn(3),
            )

            Spacer(Modifier.height(18.dp))

            GameModeCard(
                kicker = "Who am I?",
                title = "Nani Huyu?",
                description = "Everyone gets a secret face. Ask yes/no questions. Race to the podium.",
                emoji = "📸",
                tags = listOf("MULTIPLAYER", "LIVE"),
                colors = SjGradients.Violet,
                onClick = onOpenNani,
                modifier = Modifier.staggerIn(4),
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggerIn(5),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MiniTile(
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    iconTint = Sj.Gold,
                    title = "How to play",
                    subtitle = "Rules in 20 sec",
                    onClick = { showHowTo = true },
                    modifier = Modifier.weight(1f),
                )
                MiniTile(
                    icon = if (soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                    iconTint = if (soundEnabled) Sj.Savanna else Sj.TextLo,
                    title = if (soundEnabled) "Sound on" else "Muted",
                    subtitle = if (soundEnabled) "Effects & banter" else "Tap to unmute",
                    onClick = onToggleSound,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(26.dp))
            Text(
                "Made in Nairobi 🇰🇪",
                color = Sj.TextLo,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .navigationBarsPadding(),
            )
            Spacer(Modifier.height(18.dp))
        }
    }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            containerColor = Sj.Night3,
            titleContentColor = Sj.TextHi,
            textContentColor = Sj.TextMid,
            title = { Text("Game server", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column {
                    Text(
                        "Your Render URL, or your PC on Wi-Fi (e.g. http://192.168.1.50:3000).",
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Server URL") },
                        singleLine = true,
                        colors = sjTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    api.baseUrl = urlInput
                    showServerDialog = false
                    refreshTrigger += 1
                }) { Text("Save & connect", color = Sj.Gold, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) { Text("Cancel", color = Sj.TextMid) }
            },
        )
    }

    if (showHowTo) {
        ModalBottomSheet(
            onDismissRequest = { showHowTo = false },
            containerColor = Sj.Night2,
            contentColor = Sj.TextHi,
        ) {
            HowToPlaySheet()
        }
    }
}

@Composable
private fun ServerStatusPill(online: Boolean?, onClick: () -> Unit) {
    val dotColor = when (online) {
        true -> Sj.Savanna
        false -> Sj.Ember
        null -> Sj.Gold
    }
    val label = when (online) {
        true -> "Online"
        false -> "Offline"
        null -> "Waking"
    }
    val pulse = rememberInfiniteTransition(label = "status")
    val a by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "status_a",
    )
    Row(
        modifier = Modifier
            .bounceClick(onClick = onClick)
            .clip(RoundedCornerShape(percent = 50))
            .background(Sj.GlassStrong)
            .border(1.dp, Sj.GlassBorder, RoundedCornerShape(percent = 50))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .graphicsLayer { alpha = if (online == null) a else 1f }
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = Sj.TextHi, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun GameModeCard(
    kicker: String,
    title: String,
    description: String,
    emoji: String,
    tags: List<String>,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(32.dp)
    val phase = remember(title) { if (title.contains("Nani", ignoreCase = true)) 0.5f else 0f }

    KenyaBeamBorder(
        cornerRadius = 32.dp,
        borderWidth = 3.5.dp,
        phaseOffset = phase,
        modifier = modifier
            .fillMaxWidth()
            .height(236.dp)
            .bounceClick(onClick = onClick)
            .shadow(26.dp, shape, ambientColor = colors.last(), spotColor = colors.last()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors, Offset.Zero, Offset.Infinite)),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width * 0.86f, size.height * 0.26f)
                drawCircle(Color.White.copy(alpha = 0.10f), radius = size.height * 0.72f, center = c)
                drawCircle(Color.White.copy(alpha = 0.10f), radius = size.height * 0.48f, center = c)
                drawCircle(Color.White.copy(alpha = 0.14f), radius = size.height * 0.28f, center = c)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 22.dp, bottom = 22.dp, end = 96.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { tag ->
                        SjPill(text = tag, background = Color.Black.copy(alpha = 0.22f), contentColor = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    kicker.uppercase(),
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                )
                Text(
                    title,
                    color = Color.White,
                    fontFamily = ManlineDisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 38.sp,
                    lineHeight = 40.sp,
                )
                Text(
                    description,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .size(54.dp)
                    .shadow(14.dp, RoundedCornerShape(18.dp), ambientColor = Sj.Night, spotColor = Sj.Night)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Sj.Night.copy(alpha = 0.88f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play $title",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

@Composable
private fun MiniTile(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier,
        corner = 26.dp,
        padding = PaddingValues(18.dp),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(iconTint.copy(alpha = 0.16f))
                .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(title, color = Sj.TextHi, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        Text(subtitle, color = Sj.TextLo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun HowToPlaySheet() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(bottom = 32.dp),
    ) {
        Text("How to play", fontFamily = ManlineDisplayFont, fontSize = 30.sp, color = Sj.TextHi)
        Spacer(Modifier.height(18.dp))
        HowToBlock(
            emoji = "🎭",
            title = "Charades",
            colors = SjGradients.Sunset,
            steps = listOf(
                "Pick a deck & hold the phone on your forehead, screen facing out.",
                "Friends act, mime or describe the word — no saying it!",
                "Tilt DOWN when you get it ✅  •  Tilt UP to pass ⏭️",
                "10+ points = celebration. Under 10 = you get roasted 😂",
            ),
        )
        Spacer(Modifier.height(14.dp))
        HowToBlock(
            emoji = "📸",
            title = "Nani Huyu?",
            colors = SjGradients.Violet,
            steps = listOf(
                "One person hosts and shares the 4-letter room code.",
                "Everyone joins on their own phone and gets a secret face.",
                "Hold it to your forehead — you see everyone but yourself.",
                "Ask yes/no questions. Tap “I guessed it!” to claim your spot.",
            ),
        )
    }
}

@Composable
private fun HowToBlock(emoji: String, title: String, colors: List<Color>, steps: List<String>) {
    GlassCard(corner = 22.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(colors)),
                contentAlignment = Alignment.Center,
            ) { Text(emoji, fontSize = 20.sp) }
            Spacer(Modifier.width(12.dp))
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Sj.TextHi)
        }
        Spacer(Modifier.height(12.dp))
        steps.forEachIndexed { i, step ->
            Row(modifier = Modifier.padding(vertical = 5.dp)) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.first()),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${i + 1}", color = Sj.Night, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(10.dp))
                Text(step, color = Sj.TextMid, fontSize = 14.sp, lineHeight = 19.sp)
            }
        }
    }
}
