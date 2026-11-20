package com.sijui.charades.ui.charades

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sijui.charades.data.SijuiApiClient
import com.sijui.charades.data.model.CharadesDeck
import com.sijui.charades.hardware.ForeheadModeEffect
import com.sijui.charades.hardware.TiltAction
import com.sijui.charades.hardware.TiltDetector
import com.sijui.charades.ui.design.AnimatedCounter
import com.sijui.charades.ui.design.AuroraBackground
import com.sijui.charades.ui.design.BigCountdown
import com.sijui.charades.ui.design.ConfettiBurst
import com.sijui.charades.ui.design.GhostButton
import com.sijui.charades.ui.design.GlassCard
import com.sijui.charades.ui.design.GradientButton
import com.sijui.charades.ui.design.KenyaBeamBorder
import com.sijui.charades.ui.design.ScreenHeader
import com.sijui.charades.ui.design.SectionLabel
import com.sijui.charades.ui.design.SegmentedToggle
import com.sijui.charades.ui.design.SelectChip
import com.sijui.charades.ui.design.Sj
import com.sijui.charades.ui.design.SjGradients
import com.sijui.charades.ui.design.SjPill
import com.sijui.charades.ui.design.TimerRing
import com.sijui.charades.ui.design.bounceClick
import com.sijui.charades.ui.design.deckGradient
import com.sijui.charades.ui.design.sjTextFieldColors
import com.sijui.charades.ui.design.staggerIn
import com.sijui.charades.ui.theme.ManlineDisplayFont

@Composable
fun CharadesRootScreen(
    viewModel: CharadesViewModel,
    api: SijuiApiClient,
    onExit: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    when (state.phase) {
        CharadesPhase.DECK_PICKER -> DeckPickerScreen(
            state = state,
            api = api,
            onBack = onExit,
            onRefresh = viewModel::refreshDecks,
            onModeChange = viewModel::setMode,
            onRoundSecondsChange = viewModel::setRoundSeconds,
            onTeamNamesChange = viewModel::setTeamNames,
            onSelectDeck = viewModel::startDeck,
        )
        CharadesPhase.COUNTDOWN -> CountdownScreen(state = state, onCancel = viewModel::backToDeckPicker)
        CharadesPhase.PLAYING -> ForeheadPlayScreen(
            state = state,
            onCorrect = { viewModel.recordAnswer(true) },
            onPass = { viewModel.recordAnswer(false) },
            onEndEarly = viewModel::finishRound,
        )
        CharadesPhase.SUMMARY -> SummaryScreen(
            state = state,
            onNext = viewModel::nextTeamTurnOrReplay,
            onChangeDeck = viewModel::backToDeckPicker,
        )
    }
}

/* ============================================================
   Deck picker
   ============================================================ */

@Composable
private fun DeckPickerScreen(
    state: CharadesUiState,
    api: SijuiApiClient,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onModeChange: (CharadesMode) -> Unit,
    onRoundSecondsChange: (Int) -> Unit,
    onTeamNamesChange: (String, String) -> Unit,
    onSelectDeck: (CharadesDeck) -> Unit,
) {
    var teamA by remember { mutableStateOf(state.teamAName) }
    var teamB by remember { mutableStateOf(state.teamBName) }

    AuroraBackground(colors = listOf(Sj.Coral, Sj.Gold, Sj.Orchid)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ScreenHeader(
                    title = "Charades",
                    subtitle = "Heads up!",
                    onBack = onBack,
                    trailing = {
                        SjPill(
                            text = if (state.isLoadingDecks) "Syncing…" else if (state.isOfflineFallback) "↻ Offline" else "● Synced",
                            background = if (state.isOfflineFallback) Sj.GlassStrong else Sj.Savanna.copy(alpha = 0.25f),
                            contentColor = if (state.isOfflineFallback) Sj.TextMid else Sj.Savanna,
                            onClick = onRefresh,
                        )
                    },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.staggerIn(0)) {
                    SegmentedToggle(
                        options = listOf("⚡ Quick Play", "🏆 Team Battle"),
                        selectedIndex = if (state.mode == CharadesMode.QUICK_PLAY) 0 else 1,
                        onSelect = { onModeChange(if (it == 0) CharadesMode.QUICK_PLAY else CharadesMode.TEAM_BATTLE) },
                        colors = SjGradients.Sunset,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("⏱", fontSize = 18.sp)
                        Spacer(Modifier.width(2.dp))
                        listOf(45, 60, 90).forEach { sec ->
                            SelectChip(
                                text = "${sec}s",
                                selected = state.roundSeconds == sec,
                                onClick = { onRoundSecondsChange(sec) },
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = state.mode == CharadesMode.TEAM_BATTLE,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        GlassCard(
                            modifier = Modifier.padding(top = 14.dp),
                            padding = PaddingValues(14.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = teamA,
                                    onValueChange = { teamA = it; onTeamNamesChange(teamA, teamB) },
                                    label = { Text("Team 1") },
                                    singleLine = true,
                                    colors = sjTextFieldColors(),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f),
                                )
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(SjGradients.Sunset)),
                                    contentAlignment = Alignment.Center,
                                ) { Text("VS", color = Sj.Night, fontWeight = FontWeight.Black, fontSize = 11.sp) }
                                OutlinedTextField(
                                    value = teamB,
                                    onValueChange = { teamB = it; onTeamNamesChange(teamA, teamB) },
                                    label = { Text("Team 2") },
                                    singleLine = true,
                                    colors = sjTextFieldColors(),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    SectionLabel("Chagua deck  •  ${state.decks.size} decks")
                }
            }

            itemsIndexed(state.decks, key = { _, d -> d.id }) { index, deck ->
                DeckTile(
                    deck = deck,
                    api = api,
                    onClick = { onSelectDeck(deck) },
                    modifier = Modifier.staggerIn(index + 1),
                )
            }
        }
    }
}

@Composable
private fun DeckTile(
    deck: CharadesDeck,
    api: SijuiApiClient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = deckGradient(deck.theme)
    val shape = RoundedCornerShape(28.dp)
    val phase = remember(deck.id) { ((deck.id.hashCode() and 0x7fffffff) % 100) / 100f }
    val coverUrl = remember(deck.coverUrl) { api.resolveImageUrl(deck.coverUrl) }

    KenyaBeamBorder(
        cornerRadius = 28.dp,
        borderWidth = 3.dp,
        phaseOffset = phase,
        modifier = modifier
            .aspectRatio(0.88f)
            .bounceClick(onClick = onClick)
            .shadow(16.dp, shape, ambientColor = colors.last(), spotColor = colors.last()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors, Offset.Zero, Offset.Infinite)),
        ) {
            if (!coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = deck.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.18f),
                                    Color.Black.copy(alpha = 0.72f),
                                ),
                            ),
                        ),
                )
            }

            val longestWord = remember(deck.name) {
                deck.name.split(" ", "/", "-").maxOfOrNull { it.length } ?: 0
            }
            val titleSize = when {
                longestWord >= 12 -> 19.sp
                longestWord >= 10 -> 21.sp
                else -> 25.sp
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f),
                            ),
                        ),
                    ),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(
                    text = deck.name,
                    color = Color.White,
                    fontFamily = ManlineDisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = titleSize,
                    lineHeight = titleSize * 1.12f,
                    textAlign = TextAlign.Start,
                    maxLines = 3,
                )
            }
        }
    }
}

/* ============================================================
   Countdown
   ============================================================ */

@Composable
private fun CountdownScreen(state: CharadesUiState, onCancel: () -> Unit) {
    ForeheadModeEffect(lockLandscape = true, keepScreenOn = true, immersive = true)
    val colors = deckGradient(state.selectedDeck?.theme)
    Box(modifier = Modifier.fillMaxSize()) {
        BigCountdown(
            number = state.countdownValue,
            colors = colors,
            headline = if (state.mode == CharadesMode.TEAM_BATTLE) "${state.activeTeamName.uppercase()} — WEKA SIMU KICHWANI!" else "WEKA SIMU KICHWANI!",
            subline = "Phone on your forehead, screen facing your friends",
            footer = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SjPill("⬇️  Tilt down = Correct", background = Color.Black.copy(alpha = 0.22f), contentColor = Color.White)
                    SjPill("⬆️  Tilt up = Pass", background = Color.Black.copy(alpha = 0.22f), contentColor = Color.White)
                }
            },
        )
        SjPill(
            text = "✕  Cancel",
            background = Color.Black.copy(alpha = 0.25f),
            contentColor = Color.White,
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp),
        )
    }
}

/* ============================================================
   Forehead play (landscape)
   ============================================================ */

@Composable
private fun ForeheadPlayScreen(
    state: CharadesUiState,
    onCorrect: () -> Unit,
    onPass: () -> Unit,
    onEndEarly: () -> Unit,
) {
    ForeheadModeEffect(lockLandscape = true, keepScreenOn = true, immersive = true)

    val context = LocalContext.current
    DisposableEffect(context) {
        val detector = TiltDetector(context) { action ->
            when (action) {
                TiltAction.CORRECT -> onCorrect()
                TiltAction.PASS -> onPass()
            }
        }
        detector.start()
        onDispose { detector.stop() }
    }

    val base = deckGradient(state.selectedDeck?.theme)
    val top by animateColorAsState(
        targetValue = when (state.flash) {
            FlashFeedback.CORRECT -> SjGradients.Win[0]
            FlashFeedback.PASS -> SjGradients.Lose[0]
            FlashFeedback.NONE -> base[0]
        },
        animationSpec = tween(220),
        label = "bg_top",
    )
    val bottom by animateColorAsState(
        targetValue = when (state.flash) {
            FlashFeedback.CORRECT -> SjGradients.Win[1]
            FlashFeedback.PASS -> SjGradients.Lose[1]
            FlashFeedback.NONE -> base[1]
        },
        animationSpec = tween(220),
        label = "bg_bottom",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(top, bottom), Offset.Zero, Offset.Infinite)),
    ) {
        // Tap fallback zones: left = pass, right = correct
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPass,
                    ),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCorrect,
                    ),
            )
        }

        // HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 28.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("✅", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                AnimatedContent(
                    targetState = state.roundScore,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                    },
                    label = "score",
                ) { s ->
                    Text("$s", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            TimerRing(secondsLeft = state.secondsLeft, total = state.roundSeconds, diameter = 76.dp)
            Spacer(Modifier.weight(1f))
            SjPill(
                text = "END ROUND",
                background = Color.Black.copy(alpha = 0.22f),
                contentColor = Color.White,
                onClick = onEndEarly,
            )
        }

        // The word
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp)
                .padding(top = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = state.flash to (state.currentCard?.text.orEmpty()),
                transitionSpec = {
                    (scaleIn(initialScale = 0.6f, animationSpec = tween(260)) + fadeIn(tween(200))) togetherWith
                        (scaleOut(targetScale = 1.25f, animationSpec = tween(180)) + fadeOut(tween(160)))
                },
                label = "word",
            ) { (flash, word) ->
                when (flash) {
                    FlashFeedback.CORRECT -> FlashLabel(big = "SAWA!", small = "+1 point")
                    FlashFeedback.PASS -> FlashLabel(big = "PASS", small = "next one…")
                    FlashFeedback.NONE -> {
                        val size = when {
                            word.length > 22 -> 46.sp
                            word.length > 14 -> 58.sp
                            word.length > 8 -> 74.sp
                            else -> 92.sp
                        }
                        Text(
                            text = word,
                            color = Color.White,
                            fontFamily = ManlineDisplayFont,
                            fontSize = size,
                            lineHeight = size * 1.05f,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // Edge hints
        Text(
            "‹ PASS",
            color = Color.White.copy(alpha = 0.45f),
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 2.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp),
        )
        Text(
            "GOT IT ›",
            color = Color.White.copy(alpha = 0.45f),
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 2.sp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp),
        )
    }
}

@Composable
private fun FlashLabel(big: String, small: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(big, color = Color.White, fontFamily = ManlineDisplayFont, fontSize = 104.sp, lineHeight = 106.sp)
        Text(small.uppercase(), color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Black, letterSpacing = 3.sp)
    }
}

/* ============================================================
   Summary
   ============================================================ */

@Composable
private fun SummaryScreen(
    state: CharadesUiState,
    onNext: () -> Unit,
    onChangeDeck: () -> Unit,
) {
    val won = state.roundScore >= 10
    val passed = state.roundResults.count { !it.guessed }
    val total = state.roundResults.size
    val accuracy = if (total > 0) (state.roundScore * 100) / total else 0
    val banter = when {
        state.roundScore >= 15 -> "Wewe ni legend! 🔥"
        state.roundScore >= 10 -> "Sawa kabisa! Big brain energy 🏆"
        state.roundScore >= 5 -> "Umejaribu… kidogo 😅"
        else -> "Kwani ulikwama wapi?! 😂"
    }
    val medalColors = if (won) SjGradients.Win else SjGradients.Lose

    AuroraBackground(colors = if (won) listOf(Sj.Savanna, Sj.Gold, Sj.Lagoon) else listOf(Sj.Ember, Sj.Orchid, Sj.Coral)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ScreenHeader(
                    title = if (state.mode == CharadesMode.TEAM_BATTLE) "${state.activeTeamName}'s round" else "Round over",
                    subtitle = state.selectedDeck?.name ?: "Charades",
                    onBack = onChangeDeck,
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggerIn(0),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(196.dp)
                            .shadow(36.dp, CircleShape, ambientColor = medalColors.last(), spotColor = medalColors.last())
                            .clip(CircleShape)
                            .background(Brush.linearGradient(medalColors, Offset.Zero, Offset.Infinite))
                            .border(6.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedCounter(target = state.roundScore, fontSize = 84.sp, color = Color.White)
                            Text("POINTS", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        banter,
                        color = Sj.TextHi,
                        fontFamily = ManlineDisplayFont,
                        fontSize = 26.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.staggerIn(1),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatTile("✅", "${state.roundScore}", "Got it", Modifier.weight(1f))
                    StatTile("⏭️", "$passed", "Passed", Modifier.weight(1f))
                    StatTile("🎯", "$accuracy%", "Accuracy", Modifier.weight(1f))
                }
            }

            if (state.mode == CharadesMode.TEAM_BATTLE) {
                item { TeamScoreboard(state = state, modifier = Modifier.staggerIn(2)) }
            }

            if (state.roundResults.isNotEmpty()) {
                item { SectionLabel("Maneno ya round hii  •  This round", modifier = Modifier.padding(top = 6.dp)) }
                itemsIndexed(state.roundResults) { i, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggerIn(3 + i.coerceAtMost(8))
                            .clip(RoundedCornerShape(18.dp))
                            .background(Sj.Glass)
                            .border(1.dp, Sj.GlassBorder, RoundedCornerShape(18.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (item.guessed) Sj.Savanna else Sj.Ember.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (item.guessed) "✓" else "✕", color = Sj.Night, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            item.word,
                            color = if (item.guessed) Sj.TextHi else Sj.TextLo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Sticky actions
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Sj.Night.copy(alpha = 0.95f))))
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GhostButton(text = "Decks", emoji = "🃏", onClick = onChangeDeck, modifier = Modifier.weight(1f))
            val label = when {
                state.mode == CharadesMode.TEAM_BATTLE && !state.isTeamBattleFinished -> "Next team"
                state.mode == CharadesMode.TEAM_BATTLE -> "New match"
                else -> "Play again"
            }
            GradientButton(text = label, emoji = "🔥", onClick = onNext, modifier = Modifier.weight(1.5f))
        }

        if (won) ConfettiBurst(trigger = state.roundResults.size)
    }
}

@Composable
private fun StatTile(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, corner = 20.dp, padding = PaddingValues(vertical = 14.dp, horizontal = 10.dp)) {
        Text(emoji, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(4.dp))
        Text(value, color = Sj.TextHi, fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(label.uppercase(), color = Sj.TextLo, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun TeamScoreboard(state: CharadesUiState, modifier: Modifier = Modifier) {
    val aLeads = state.teamAScore > state.teamBScore
    val bLeads = state.teamBScore > state.teamAScore
    GlassCard(modifier = modifier, corner = 26.dp) {
        Text(
            if (state.isTeamBattleFinished) "🏆  FINAL SCORE" else "⚔️  TEAM BATTLE  •  Turn ${state.completedTurns}/${state.totalRoundsPerTeam * 2}",
            color = Sj.TextMid,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamSide(state.teamAName, state.teamAScore, aLeads, SjGradients.Lagoon, Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(SjGradients.Sunset)),
                contentAlignment = Alignment.Center,
            ) { Text("VS", color = Sj.Night, fontWeight = FontWeight.Black, fontSize = 13.sp) }
            TeamSide(state.teamBName, state.teamBScore, bLeads, SjGradients.Rose, Modifier.weight(1f))
        }
        if (state.isTeamBattleFinished) {
            Spacer(Modifier.height(14.dp))
            val result = when {
                aLeads -> "${state.teamAName} wins! 🎉"
                bLeads -> "${state.teamBName} wins! 🎉"
                else -> "It's a draw! 🤝"
            }
            Text(result, color = Sj.Gold, fontFamily = ManlineDisplayFont, fontSize = 24.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun TeamSide(name: String, score: Int, leading: Boolean, colors: List<Color>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors.map { it.copy(alpha = if (leading) 0.9f else 0.35f) }))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (leading) "👑" else " ", fontSize = 16.sp)
        Text(name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1)
        AnimatedCounter(target = score, fontSize = 40.sp, color = Color.White)
    }
}
