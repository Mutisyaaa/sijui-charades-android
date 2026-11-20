package com.sijui.charades.ui.nani

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sijui.charades.data.SijuiApiClient
import com.sijui.charades.data.model.NaniCard
import com.sijui.charades.data.model.NaniDeck
import com.sijui.charades.data.model.NaniPlayer
import com.sijui.charades.data.model.NaniRoom
import com.sijui.charades.hardware.ForeheadModeEffect
import com.sijui.charades.hardware.ImageCodec
import com.sijui.charades.ui.design.AuroraBackground
import com.sijui.charades.ui.design.AvatarBubble
import com.sijui.charades.ui.design.BigCountdown
import com.sijui.charades.ui.design.ConfettiBurst
import com.sijui.charades.ui.design.GhostButton
import com.sijui.charades.ui.design.GlassCard
import com.sijui.charades.ui.design.GradientButton
import com.sijui.charades.ui.design.ScreenHeader
import com.sijui.charades.ui.design.SectionLabel
import com.sijui.charades.ui.design.SegmentedToggle
import com.sijui.charades.ui.design.Sj
import com.sijui.charades.ui.design.SjGradients
import com.sijui.charades.ui.design.SjPill
import com.sijui.charades.ui.design.bounceClick
import com.sijui.charades.ui.design.sjTextFieldColors
import com.sijui.charades.ui.design.staggerIn
import com.sijui.charades.ui.theme.ManlineDisplayFont

private val NaniAurora = listOf(Sj.Orchid, Sj.Lagoon, Sj.Ember)

@Composable
fun NaniRootScreen(
    viewModel: NaniViewModel,
    onExit: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val room = state.room

    if (room == null) {
        EntryScreen(
            state = state,
            onBack = onExit,
            onRefreshDecks = viewModel::loadDecks,
            onSelectDeck = viewModel::selectDeck,
            onCreateRoom = viewModel::createRoom,
            onJoinRoom = viewModel::joinRoom,
        )
        return
    }

    when (room.status) {
        "countdown" -> BigCountdown(
            number = state.countdownSecondsLeft.coerceAtLeast(1),
            colors = SjGradients.Violet,
            headline = "SIMU KICHWANI!",
            subline = "Hold it to your forehead — screen facing out. No peeking 👀",
        ).also { ForeheadModeEffect(lockLandscape = false, keepScreenOn = true, immersive = true) }
        "playing" -> {
            val me = room.players.firstOrNull { it.id == state.playerId }
            if (me?.guessed == true) {
                RevealedScreen(room = room, me = me, api = viewModel.api, onLeave = viewModel::leaveRoom)
            } else {
                ForeheadCardScreen(
                    card = me?.card ?: room.myCard,
                    room = room,
                    api = viewModel.api,
                    onGuessed = viewModel::submitGuessed,
                    onLeave = viewModel::leaveRoom,
                )
            }
        }
        "ended" -> PodiumScreen(
            room = room,
            myPlayerId = state.playerId,
            isLoading = state.isLoading,
            onPlayAgain = viewModel::restartRound,
            onLeave = viewModel::leaveRoom,
        )
        else -> LobbyScreen(
            room = room,
            myPlayerId = state.playerId,
            error = state.error,
            isLoading = state.isLoading,
            onStart = viewModel::startRound,
            onLeave = viewModel::leaveRoom,
        )
    }
}

/* ============================================================
   Entry: Host / Join
   ============================================================ */

@Composable
private fun EntryScreen(
    state: NaniUiState,
    onBack: () -> Unit,
    onRefreshDecks: () -> Unit,
    onSelectDeck: (String) -> Unit,
    onCreateRoom: (String) -> Unit,
    onJoinRoom: (String, String) -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var playerName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }

    AuroraBackground(colors = NaniAurora) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp),
        ) {
            ScreenHeader(title = "Nani Huyu?", subtitle = "Who am I", onBack = onBack)
            Spacer(Modifier.height(6.dp))
            SegmentedToggle(
                options = listOf("👑 Host", "🎟️ Join"),
                selectedIndex = tab,
                onSelect = { tab = it },
                colors = SjGradients.Violet,
            )

            if (!state.error.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                ErrorBanner(state.error)
            }
            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { it * dir / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir / 3 } + fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "nani_tab",
            ) { t ->
                if (t == 0) {
                    HostPane(
                        state = state,
                        playerName = playerName,
                        onNameChange = { playerName = it },
                        onRefreshDecks = onRefreshDecks,
                        onSelectDeck = onSelectDeck,
                        onCreate = { onCreateRoom(playerName) },
                    )
                } else {
                    JoinPane(
                        isLoading = state.isLoading,
                        playerName = playerName,
                        onNameChange = { playerName = it },
                        code = joinCode,
                        onCodeChange = { joinCode = it },
                        onJoin = { onJoinRoom(joinCode, playerName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HostPane(
    state: NaniUiState,
    playerName: String,
    onNameChange: (String) -> Unit,
    onRefreshDecks: () -> Unit,
    onSelectDeck: (String) -> Unit,
    onCreate: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = playerName,
            onValueChange = onNameChange,
            label = { Text("Your name") },
            placeholder = { Text("e.g. Kevo") },
            singleLine = true,
            colors = sjTextFieldColors(),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Photo pack", modifier = Modifier.weight(1f))
            SjPill("↻ Refresh", onClick = onRefreshDecks)
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            if (state.isLoading && state.decks.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Sj.Orchid)
                    }
                }
            }
            if (state.decks.isEmpty() && !state.isLoading) {
                item {
                    GlassCard {
                        Text("📭  No photo packs yet", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Upload pictures of your squad in the Studio and they'll show up here.", color = Sj.TextMid, fontSize = 14.sp)
                    }
                }
            }
            items(state.decks, key = { it.id }) { deck ->
                PackRow(deck = deck, selected = deck.id == state.selectedDeckId, onClick = { onSelectDeck(deck.id) })
            }
        }
        GradientButton(
            text = if (state.isLoading) "Creating…" else "Create room",
            emoji = "🚀",
            colors = SjGradients.Violet,
            contentColor = Color.White,
            enabled = !state.isLoading,
            onClick = onCreate,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun PackRow(deck: NaniDeck, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .clip(shape)
            .background(
                if (selected) Brush.linearGradient(SjGradients.Violet.map { it.copy(alpha = 0.35f) })
                else Brush.linearGradient(listOf(Sj.Glass, Sj.Glass)),
            )
            .border(if (selected) 2.dp else 1.dp, if (selected) Sj.Orchid else Sj.GlassBorder, shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(if (selected) Brush.linearGradient(SjGradients.Violet) else Brush.linearGradient(listOf(Sj.GlassStrong, Sj.GlassStrong))),
            contentAlignment = Alignment.Center,
        ) { Text(deck.icon, fontSize = 24.sp) }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(deck.name, color = Sj.TextHi, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${deck.count} photos", color = Sj.TextLo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (selected) Sj.Orchid else Color.Transparent)
                .border(2.dp, if (selected) Sj.Orchid else Sj.GlassBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Text("✓", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
    }
}

@Composable
private fun JoinPane(
    isLoading: Boolean,
    playerName: String,
    onNameChange: (String) -> Unit,
    code: String,
    onCodeChange: (String) -> Unit,
    onJoin: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SectionLabel("Room code")
        Spacer(Modifier.height(12.dp))
        BasicTextField(
            value = code,
            onValueChange = { raw -> onCodeChange(raw.uppercase().filter { it.isLetterOrDigit() }.take(7)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
            decorationBox = { inner ->
                Box {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val slots = maxOf(4, code.length)
                        for (i in 0 until slots) {
                            val ch = code.getOrNull(i)?.toString().orEmpty()
                            val active = i == code.length
                            CodeSlot(char = ch, active = active, modifier = Modifier.weight(1f))
                        }
                    }
                    Box(Modifier.size(1.dp)) { inner() }
                }
            },
        )
        Spacer(Modifier.height(22.dp))
        OutlinedTextField(
            value = playerName,
            onValueChange = onNameChange,
            label = { Text("Your name") },
            singleLine = true,
            colors = sjTextFieldColors(),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Ask the host for the 4-letter code on their screen.",
            color = Sj.TextLo,
            fontSize = 13.sp,
        )
        Spacer(Modifier.weight(1f))
        GradientButton(
            text = if (isLoading) "Joining…" else "Join game",
            emoji = "🎉",
            colors = SjGradients.Lagoon,
            contentColor = Color.White,
            enabled = !isLoading && code.length >= 4,
            onClick = onJoin,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun CodeSlot(char: String, active: Boolean, modifier: Modifier = Modifier) {
    val blink = rememberInfiniteTransition(label = "blink")
    val a by blink.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "blink_a")
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .aspectRatio(0.82f)
            .clip(shape)
            .background(if (char.isNotEmpty()) Sj.GlassStrong else Sj.Glass)
            .border(
                width = 2.dp,
                color = when {
                    active -> Sj.Gold.copy(alpha = a)
                    char.isNotEmpty() -> Sj.Orchid
                    else -> Sj.GlassBorder
                },
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(char, color = Sj.TextHi, fontFamily = ManlineDisplayFont, fontSize = 38.sp)
    }
}

/* ============================================================
   Lobby
   ============================================================ */

@Composable
private fun LobbyScreen(
    room: NaniRoom,
    myPlayerId: String?,
    error: String?,
    isLoading: Boolean,
    onStart: () -> Unit,
    onLeave: () -> Unit,
) {
    val context = LocalContext.current
    AuroraBackground(colors = NaniAurora) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp),
        ) {
            ScreenHeader(title = "Lobby", subtitle = room.deckName, onBack = onLeave)

            // Room code hero
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggerIn(0),
                corner = 30.dp,
                padding = PaddingValues(20.dp),
            ) {
                SectionLabel("Room code", modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    room.code.forEach { ch ->
                        Box(
                            modifier = Modifier
                                .size(width = 60.dp, height = 72.dp)
                                .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = Sj.Orchid, spotColor = Sj.Orchid)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.linearGradient(SjGradients.Violet, Offset.Zero, Offset.Infinite)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(ch.toString(), color = Color.White, fontFamily = ManlineDisplayFont, fontSize = 40.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                GhostButton(
                    text = "Invite on WhatsApp",
                    emoji = "💬",
                    height = 48.dp,
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Tucheze Nani Huyu? 📸 Join my Sijui room with code ${room.code}!",
                            )
                        }
                        context.startActivity(Intent.createChooser(send, "Invite friends"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (!error.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                ErrorBanner(error)
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Wachezaji  •  ${room.players.size} in the room", modifier = Modifier.weight(1f))
                WaitingDots()
            }
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(room.players, key = { it.id }) { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (p.id == myPlayerId) Sj.GlassStrong else Sj.Glass)
                            .border(1.dp, if (p.id == myPlayerId) Sj.Orchid.copy(alpha = 0.6f) else Sj.GlassBorder, RoundedCornerShape(22.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AvatarBubble(name = p.name, size = 46.dp, badge = if (p.isHost) "👑" else null)
                        Spacer(Modifier.width(14.dp))
                        Text(p.name, color = Sj.TextHi, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        if (p.id == myPlayerId) SjPill("YOU", background = Sj.Orchid, contentColor = Color.White)
                        else if (p.isHost) SjPill("HOST", background = Sj.Gold.copy(alpha = 0.25f), contentColor = Sj.Gold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            if (room.isHost) {
                GradientButton(
                    text = if (isLoading) "Dealing photos…" else if (room.players.size < 2) "Start (waiting for friends)" else "Start round",
                    emoji = "🎬",
                    colors = SjGradients.Win,
                    contentColor = Sj.Night,
                    enabled = !isLoading,
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                    Text(
                        "⏳  Waiting for the host to start…",
                        color = Sj.TextMid,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun WaitingDots() {
    val t = rememberInfiniteTransition(label = "dots")
    val p by t.animateFloat(0f, 3f, infiniteRepeatable(tween(1200)), label = "dots_p")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until 3) {
            val on = p.toInt() == i
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (on) Sj.Orchid else Sj.GlassBorder),
            )
        }
    }
}

/* ============================================================
   Forehead card
   ============================================================ */

@Composable
private fun ForeheadCardScreen(
    card: NaniCard?,
    room: NaniRoom,
    api: SijuiApiClient,
    onGuessed: () -> Unit,
    onLeave: () -> Unit,
) {
    ForeheadModeEffect(lockLandscape = false, keepScreenOn = true, immersive = true)
    val glow = rememberInfiniteTransition(label = "glow")
    val g by glow.animateFloat(0.6f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "glow_a")
    val guessedCount = room.players.count { it.guessed }

    AuroraBackground(colors = listOf(Sj.Orchid, Sj.Gold, Sj.Lagoon)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SjPill(card?.category ?: "Nani Huyu?", background = Sj.Gold, contentColor = Sj.Night)
                Spacer(Modifier.weight(1f))
                SjPill("$guessedCount/${room.players.size} done", modifier = Modifier.padding(end = 8.dp))
                SjPill("✕", onClick = onLeave)
            }

            Spacer(Modifier.weight(1f))

            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .aspectRatio(1f)
                        .graphicsLayer { alpha = g }
                        .background(
                            Brush.radialGradient(listOf(Sj.Gold.copy(alpha = 0.55f), Color.Transparent)),
                            CircleShape,
                        ),
                )
                NaniCharacterPhoto(
                    card = card,
                    api = api,
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f)
                        .shadow(30.dp, RoundedCornerShape(36.dp), ambientColor = Sj.Gold, spotColor = Sj.Gold)
                        .clip(RoundedCornerShape(36.dp))
                        .border(5.dp, Brush.linearGradient(SjGradients.Gold), RoundedCornerShape(36.dp))
                        .background(Sj.Night3),
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                card?.name ?: "Mystery",
                color = Sj.TextHi,
                fontFamily = ManlineDisplayFont,
                fontSize = 44.sp,
                lineHeight = 46.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))

            GradientButton(
                text = "I guessed it!",
                emoji = "✅",
                colors = SjGradients.Win,
                contentColor = Sj.Night,
                onClick = onGuessed,
                height = 68.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/* ============================================================
   Revealed (waiting for others)
   ============================================================ */

@Composable
private fun RevealedScreen(
    room: NaniRoom,
    me: NaniPlayer,
    api: SijuiApiClient,
    onLeave: () -> Unit,
) {
    val rank = me.rankNumber ?: 1
    val medal = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "🎉" }
    AuroraBackground(colors = listOf(Sj.Savanna, Sj.Orchid, Sj.Gold)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp),
        ) {
            ScreenHeader(title = "Umepata!", subtitle = "You got it", onBack = onLeave)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggerIn(0),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    NaniCharacterPhoto(
                        card = me.card,
                        api = api,
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .border(4.dp, Sj.Savanna, RoundedCornerShape(32.dp))
                            .background(Sj.Night3),
                    )
                    Text(medal, fontSize = 52.sp, modifier = Modifier.padding(end = 0.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("You were", color = Sj.TextMid, fontWeight = FontWeight.Bold)
                Text(me.card?.name ?: "Unknown", color = Sj.TextHi, fontFamily = ManlineDisplayFont, fontSize = 34.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                SjPill("Finished #$rank", background = Sj.Savanna, contentColor = Sj.Night)
            }
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Still guessing…", modifier = Modifier.weight(1f))
                WaitingDots()
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(room.players, key = { it.id }) { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Sj.Glass)
                            .border(1.dp, Sj.GlassBorder, RoundedCornerShape(20.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AvatarBubble(name = p.name, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, color = Sj.TextHi, fontWeight = FontWeight.ExtraBold)
                            if (!p.guessed && p.id != me.id) {
                                Text("is ${p.card?.name ?: "…"} 🤫", color = Sj.TextLo, fontSize = 12.sp)
                            }
                        }
                        SjPill(
                            if (p.guessed) "✓ Done" else "🤔 Asking",
                            background = if (p.guessed) Sj.Savanna.copy(alpha = 0.25f) else Sj.Gold.copy(alpha = 0.2f),
                            contentColor = if (p.guessed) Sj.Savanna else Sj.Gold,
                        )
                    }
                }
            }
        }
    }
}

/* ============================================================
   Podium
   ============================================================ */

@Composable
private fun PodiumScreen(
    room: NaniRoom,
    myPlayerId: String?,
    isLoading: Boolean,
    onPlayAgain: () -> Unit,
    onLeave: () -> Unit,
) {
    val winners = remember(room.players) {
        room.players.filter { !it.isLastPlace }.sortedBy { it.rankNumber ?: 99 }
    }
    val lastPlace = remember(room.players) { room.players.firstOrNull { it.isLastPlace } }
    val top3 = winners.take(3)
    val rest = winners.drop(3)

    AuroraBackground(colors = listOf(Sj.Gold, Sj.Orchid, Sj.Ember)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { ScreenHeader(title = "Podium", subtitle = room.deckName, onBack = onLeave) }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    val order = listOf(1, 0, 2)
                    order.forEach { idx ->
                        val p = top3.getOrNull(idx)
                        Box(modifier = Modifier.weight(1f)) {
                            if (p != null) PodiumColumn(player = p, place = idx + 1, isMe = p.id == myPlayerId, modifier = Modifier.staggerIn(3 - idx))
                        }
                    }
                }
            }

            if (lastPlace != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggerIn(5)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Brush.linearGradient(SjGradients.Lose, Offset.Zero, Offset.Infinite))
                            .padding(18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🐢", fontSize = 40.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("LAST PLACE", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 2.sp)
                                Text(lastPlace.name, color = Color.White, fontFamily = ManlineDisplayFont, fontSize = 26.sp)
                                Text("Was: ${lastPlace.card?.name ?: "?"}", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("“Kwani ulikwama wapi?!” 😂", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }

            if (rest.isNotEmpty()) {
                item { SectionLabel("Wengine  •  The rest") }
                items(rest, key = { it.id }) { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Sj.Glass)
                            .border(1.dp, Sj.GlassBorder, RoundedCornerShape(20.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("#${p.rankNumber ?: "-"}", color = Sj.TextLo, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp))
                        AvatarBubble(name = p.name, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, color = Sj.TextHi, fontWeight = FontWeight.ExtraBold)
                            Text("Was ${p.card?.name ?: "?"}", color = Sj.TextLo, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Sj.Night.copy(alpha = 0.95f))))
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GhostButton(text = "Leave", onClick = onLeave, modifier = Modifier.weight(1f))
            if (room.isHost) {
                GradientButton(
                    text = if (isLoading) "Dealing…" else "Next round",
                    emoji = "🔁",
                    colors = SjGradients.Violet,
                    contentColor = Color.White,
                    enabled = !isLoading,
                    onClick = onPlayAgain,
                    modifier = Modifier.weight(1.6f),
                )
            }
        }

        ConfettiBurst(trigger = room.startedAt)
    }
}

@Composable
private fun PodiumColumn(player: NaniPlayer, place: Int, isMe: Boolean, modifier: Modifier = Modifier) {
    val (colors, height: Dp, medal) = when (place) {
        1 -> Triple(SjGradients.Gold, 150.dp, "🥇")
        2 -> Triple(SjGradients.Silver, 112.dp, "🥈")
        else -> Triple(SjGradients.Bronze, 86.dp, "🥉")
    }
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(medal, fontSize = if (place == 1) 34.sp else 26.sp)
        AvatarBubble(name = player.name, size = if (place == 1) 66.dp else 54.dp, ring = isMe)
        Spacer(Modifier.height(6.dp))
        Text(player.name, color = Sj.TextHi, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(player.card?.name ?: "", color = Sj.TextLo, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .shadow(16.dp, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp), ambientColor = colors.last(), spotColor = colors.last())
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(Brush.verticalGradient(colors)),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text("$place", color = Sj.Night.copy(alpha = 0.75f), fontFamily = ManlineDisplayFont, fontSize = 48.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Sj.Ember.copy(alpha = 0.18f))
            .border(1.dp, Sj.Ember.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⚠️", fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Text(message, color = Sj.TextHi, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/* ============================================================
   Shared photo renderer
   ============================================================ */

@Composable
fun NaniCharacterPhoto(
    card: NaniCard?,
    api: SijuiApiClient,
    modifier: Modifier = Modifier,
) {
    val raw = card?.resolvedImage
    if (raw.isNullOrBlank()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = card?.avatar ?: "📸", fontSize = 64.sp)
        }
        return
    }
    val decoded = remember(raw) { ImageCodec.decodeDataUrl(raw) }
    if (decoded != null) {
        Image(
            bitmap = decoded.asImageBitmap(),
            contentDescription = card.name,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        val url = remember(raw) { api.proxyImageUrl(raw) ?: api.resolveImageUrl(raw) }
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Sj.Gold, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
            AsyncImage(
                model = url,
                contentDescription = card.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
