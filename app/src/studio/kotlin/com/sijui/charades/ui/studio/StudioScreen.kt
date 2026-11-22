package com.sijui.charades.ui.studio

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sijui.charades.data.model.AnalyticsSummary
import com.sijui.charades.data.model.AppConfig
import com.sijui.charades.ui.design.parseHexColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sijui.charades.R
import com.sijui.charades.data.AppUpdateInfo
import com.sijui.charades.data.model.CharadesDeck
import com.sijui.charades.update.InAppUpdateBanner
import com.sijui.charades.hardware.ImageCodec
import com.sijui.charades.ui.design.AuroraBackground
import com.sijui.charades.ui.design.GhostButton
import com.sijui.charades.ui.design.GlassCard
import com.sijui.charades.ui.design.GradientButton
import com.sijui.charades.ui.design.SectionLabel
import com.sijui.charades.ui.design.SegmentedToggle
import com.sijui.charades.ui.design.SelectChip
import com.sijui.charades.ui.design.ShukaStrip
import com.sijui.charades.ui.design.Sj
import com.sijui.charades.ui.design.SjGradients
import com.sijui.charades.ui.design.SjPill
import com.sijui.charades.ui.design.sjTextFieldColors
import com.sijui.charades.ui.nani.NaniCharacterPhoto
import com.sijui.charades.ui.theme.ManlineDisplayFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var email by remember { mutableStateOf("k3v1n.goof@gmail.com") }
    var password by remember { mutableStateOf("") }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Decks, 1 = Photo Studio

    // New Deck form
    var deckName by remember { mutableStateOf("") }
    var deckDesc by remember { mutableStateOf("") }
    var deckCoverUri by remember { mutableStateOf<Uri?>(null) }
    var bulkWords by remember { mutableStateOf("") }

    // Add words to existing deck dialog
    var targetDeckForWords by remember { mutableStateOf<CharadesDeck?>(null) }
    var extraBulkWords by remember { mutableStateOf("") }

    // Photo Studio form
    var photoCategory by remember { mutableStateOf("Friends & Family") }
    var characterName by remember { mutableStateOf("") }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Server URL dialog
    var showServerDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf(viewModel.api.baseUrl) }
    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        val currentVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
        availableUpdate = viewModel.api.checkForAppUpdate(currentVersion)
    }

    val deckCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) deckCoverUri = uri }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) pickedPhotoUri = uri }

    AuroraBackground(colors = listOf(Sj.Lagoon, Sj.Orchid, Sj.Gold)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(10.dp))

            // Top Brand Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.sijui_logo),
                    contentDescription = "Sijui Studio",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(2.dp, Sj.Gold, CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SIJUI STUDIO",
                        style = TextStyle(
                            brush = Brush.linearGradient(listOf(Sj.Gold, Sj.Coral, Sj.Orchid)),
                            fontFamily = ManlineDisplayFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                        ),
                    )
                    Text(
                        text = state.user?.let { "Signed in as ${it.displayName}" } ?: "Admin Companion",
                        color = Sj.TextMid,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (state.user != null) {
                    SjPill(
                        text = "Logout",
                        background = Sj.Ember.copy(alpha = 0.25f),
                        contentColor = Sj.Ember,
                        onClick = viewModel::logout,
                    )
                } else {
                    SjPill(
                        text = when (state.serverOnline) {
                            true -> "● Online"
                            false -> "● Offline"
                            null -> "● Waking"
                        },
                        background = Sj.GlassStrong,
                        contentColor = when (state.serverOnline) {
                            true -> Sj.Savanna
                            false -> Sj.Ember
                            null -> Sj.Gold
                        },
                        onClick = { showServerDialog = true },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            ShukaStrip(modifier = Modifier.width(130.dp).clip(RoundedCornerShape(3.dp)), height = 8.dp)
            Spacer(Modifier.height(14.dp))

            availableUpdate?.let { update ->
                InAppUpdateBanner(update = update, api = viewModel.api)
                Spacer(Modifier.height(12.dp))
            }

            if (!state.error.isNullOrBlank()) {
                BannerBox(emoji = "⚠️", text = state.error!!, color = Sj.Ember)
                Spacer(Modifier.height(10.dp))
            }
            if (!state.statusMessage.isNullOrBlank()) {
                BannerBox(emoji = "✅", text = state.statusMessage!!, color = Sj.Savanna)
                Spacer(Modifier.height(10.dp))
            }

            if (state.user?.role != "admin") {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    corner = 28.dp,
                    padding = PaddingValues(22.dp),
                ) {
                    Text(
                        "Admin Sign In",
                        color = Sj.TextHi,
                        fontFamily = ManlineDisplayFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Create Charades decks with cover art, bulk-paste words, and upload Photo Studio pictures straight from your phone.",
                        color = Sj.TextMid,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Admin email") },
                        singleLine = true,
                        colors = sjTextFieldColors(),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = sjTextFieldColors(),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(18.dp))
                    GradientButton(
                        text = if (state.isLoading) "Signing in…" else "Sign in",
                        emoji = "🔐",
                        colors = SjGradients.Sunset,
                        enabled = !state.isLoading,
                        onClick = { viewModel.login(email, password) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                return@Column
            }

            // Authenticated Studio Tabs
            SegmentedToggle(
                options = listOf("🎭 Decks", "📸 Photos", "🎨 Theme", "📊 Stats"),
                selectedIndex = activeTab,
                onSelect = { activeTab = it },
                colors = when (activeTab) {
                    0 -> SjGradients.Sunset
                    1 -> SjGradients.Violet
                    2 -> SjGradients.Mint
                    else -> SjGradients.Lagoon
                },
            )

            Spacer(Modifier.height(14.dp))

            if (activeTab == 0) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 28.dp),
                ) {
                    item {
                        GlassCard(corner = 26.dp) {
                            Text(
                                "New Charades Deck",
                                color = Sj.TextHi,
                                fontFamily = ManlineDisplayFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = deckName,
                                onValueChange = { deckName = it },
                                label = { Text("Deck name (e.g. Nairobi Life)") },
                                singleLine = true,
                                colors = sjTextFieldColors(),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = deckDesc,
                                onValueChange = { deckDesc = it },
                                label = { Text("Short description (optional)") },
                                singleLine = true,
                                colors = sjTextFieldColors(),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(10.dp))
                            GhostButton(
                                text = if (deckCoverUri != null) "Cover photo selected ✓" else "Pick deck cover image",
                                emoji = "🖼️",
                                height = 50.dp,
                                onClick = {
                                    deckCoverPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = bulkWords,
                                onValueChange = { bulkWords = it },
                                label = { Text("Bulk paste words (one per line)") },
                                minLines = 4,
                                colors = sjTextFieldColors(),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(14.dp))
                            GradientButton(
                                text = if (state.isLoading) "Publishing…" else "Create deck",
                                emoji = "✨",
                                colors = SjGradients.Win,
                                enabled = !state.isLoading,
                                onClick = {
                                    viewModel.createDeckWithWords(context, deckName, deckDesc, deckCoverUri, bulkWords)
                                    deckName = ""
                                    deckDesc = ""
                                    deckCoverUri = null
                                    bulkWords = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel("Live server decks", modifier = Modifier.weight(1f))
                            SjPill("↻ Refresh", onClick = viewModel::loadStudioData)
                        }
                    }

                    items(state.decks, key = { it.id }) { deck ->
                        DeckAdminRow(
                            deck = deck,
                            onAddWords = {
                                targetDeckForWords = deck
                                extraBulkWords = ""
                            },
                            onDelete = { viewModel.deleteDeck(deck.id, deck.name) },
                        )
                    }
                }
            } else if (activeTab == 1) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 28.dp),
                ) {
                    item {
                        GlassCard(corner = 26.dp) {
                            Text(
                                "Upload to Photo Studio",
                                color = Sj.TextHi,
                                fontFamily = ManlineDisplayFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                            )
                            Spacer(Modifier.height(10.dp))
                            val quickCats = remember(state.categories) {
                                (state.categories.map { it.name } + listOf("Friends & Family", "Kenyan Celebs", "Footballers", "Memes"))
                                    .distinct()
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(quickCats) { cat ->
                                    SelectChip(
                                        text = cat,
                                        selected = photoCategory.equals(cat, ignoreCase = true),
                                        onClick = { photoCategory = cat },
                                        colors = SjGradients.Violet,
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = photoCategory,
                                onValueChange = { photoCategory = it },
                                label = { Text("Category") },
                                singleLine = true,
                                colors = sjTextFieldColors(),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = characterName,
                                onValueChange = { characterName = it },
                                label = { Text("Character name") },
                                singleLine = true,
                                colors = sjTextFieldColors(),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            GhostButton(
                                text = if (pickedPhotoUri != null) "Photo ready ✓ (Change)" else "Pick photo from gallery",
                                emoji = "🖼️",
                                height = 50.dp,
                                onClick = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(10.dp))
                            GradientButton(
                                text = if (state.isLoading) "Uploading…" else "Upload photo",
                                emoji = "☁️",
                                colors = SjGradients.Violet,
                                contentColor = Color.White,
                                enabled = !state.isLoading && pickedPhotoUri != null,
                                onClick = {
                                    pickedPhotoUri?.let { uri ->
                                        viewModel.uploadPhotoCharacter(context, uri, photoCategory, characterName)
                                        characterName = ""
                                        pickedPhotoUri = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel("Uploaded photos", modifier = Modifier.weight(1f))
                            SjPill("↻ Refresh", onClick = viewModel::loadStudioData)
                        }
                    }

                    items(state.characters, key = { it.id }) { char ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Sj.Glass)
                                .border(1.dp, Sj.GlassBorder, RoundedCornerShape(20.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Sj.Night3),
                            ) {
                                NaniCharacterPhoto(card = char, api = viewModel.api, modifier = Modifier.fillMaxSize())
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(char.name, color = Sj.TextHi, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                Text(char.category, color = Sj.TextLo, fontSize = 13.sp)
                            }
                            SjPill(
                                text = "Delete",
                                background = Sj.Ember.copy(alpha = 0.25f),
                                contentColor = Sj.Ember,
                                onClick = { viewModel.deleteCharacter(char.id) },
                            )
                        }
                    }
                }
            } else if (activeTab == 2) {
                ThemeStudioTab(
                    initialConfig = state.appConfig,
                    isLoading = state.isLoading,
                    onSave = viewModel::saveAppConfig,
                    modifier = Modifier.weight(1f),
                )
            } else {
                StatsStudioTab(
                    stats = state.analytics,
                    isLoading = state.isLoading,
                    onRefresh = viewModel::loadStudioData,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    targetDeckForWords?.let { deck ->
        AlertDialog(
            onDismissRequest = { targetDeckForWords = null },
            containerColor = Sj.Night3,
            titleContentColor = Sj.TextHi,
            textContentColor = Sj.TextMid,
            title = { Text("Add words to ${deck.name}", fontWeight = FontWeight.ExtraBold) },
            text = {
                OutlinedTextField(
                    value = extraBulkWords,
                    onValueChange = { extraBulkWords = it },
                    label = { Text("One word per line") },
                    minLines = 5,
                    colors = sjTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.bulkAddToExistingDeck(deck.id, deck.name, extraBulkWords)
                    targetDeckForWords = null
                }) { Text("Add words", color = Sj.Gold, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { targetDeckForWords = null }) { Text("Cancel", color = Sj.TextMid) }
            },
        )
    }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            containerColor = Sj.Night3,
            titleContentColor = Sj.TextHi,
            textContentColor = Sj.TextMid,
            title = { Text("Backend Server URL", fontWeight = FontWeight.ExtraBold) },
            text = {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Server URL") },
                    singleLine = true,
                    colors = sjTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.api.baseUrl = urlInput
                    showServerDialog = false
                    viewModel.pingAndCheckSession()
                }) { Text("Save", color = Sj.Gold, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) { Text("Cancel", color = Sj.TextMid) }
            },
        )
    }
}

@Composable
private fun DeckAdminRow(
    deck: CharadesDeck,
    onAddWords: () -> Unit,
    onDelete: () -> Unit,
) {
    val coverBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = deck.coverUrl) {
        val raw = deck.coverUrl
        value = if (!raw.isNullOrBlank() && raw.startsWith("data:")) {
            withContext(Dispatchers.Default) { ImageCodec.decodeDataUrl(raw)?.asImageBitmap() }
        } else {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Sj.Glass)
            .border(1.dp, Sj.GlassBorder, RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Sj.Night3),
            contentAlignment = Alignment.Center,
        ) {
            if (coverBitmap != null) {
                Image(
                    bitmap = coverBitmap!!,
                    contentDescription = deck.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text("🎭", fontSize = 22.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(deck.name, color = Sj.TextHi, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text("${deck.cards.size} cards", color = Sj.TextLo, fontSize = 12.sp)
        }
        SjPill(
            text = "+ Words",
            background = Sj.Gold.copy(alpha = 0.25f),
            contentColor = Sj.Gold,
            onClick = onAddWords,
        )
        Spacer(Modifier.width(6.dp))
        SjPill(
            text = "✕",
            background = Sj.Ember.copy(alpha = 0.25f),
            contentColor = Sj.Ember,
            onClick = onDelete,
        )
    }
}

@Composable
private fun BannerBox(emoji: String, text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(text, color = Sj.TextHi, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

private data class ThemePreset(
    val id: String,
    val label: String,
    val bg: String,
    val a1: String,
    val a2: String,
    val a3: String,
    val accent: String,
)

private val STUDIO_PRESETS = listOf(
    ThemePreset("nairobi_nights", "Nairobi Nights", "#0A0812", "#FF4D3D", "#B45CFF", "#3D7BFF", "#FFC23D"),
    ThemePreset("savanna_emerald", "Savanna Green", "#071410", "#1ED77A", "#0E8F6E", "#FFC23D", "#2EE6A8"),
    ThemePreset("rift_sunset", "Rift Sunset", "#16090B", "#FF5E3A", "#FFC23D", "#C9184A", "#FFB81C"),
    ThemePreset("shuka_crimson", "Shuka Red", "#14070A", "#E5322D", "#1FA45B", "#FFC23D", "#FF6B6B"),
    ThemePreset("lamu_ocean", "Lamu Ocean", "#07101E", "#2255D9", "#00C2FF", "#6A5BFF", "#4FA3FF"),
)

@Composable
private fun ThemeStudioTab(
    initialConfig: AppConfig,
    isLoading: Boolean,
    onSave: (AppConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf(initialConfig) }
    LaunchedEffect(initialConfig) { draft = initialConfig }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            GlassCard(corner = 26.dp) {
                Text(
                    "Theme Presets",
                    color = Sj.TextHi,
                    fontFamily = ManlineDisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(STUDIO_PRESETS, key = { it.id }) { preset ->
                        SelectChip(
                            text = preset.label,
                            selected = draft.themePreset == preset.id,
                            colors = listOf(
                                parseHexColor(preset.accent, Sj.Gold),
                                parseHexColor(preset.a1, Sj.Coral),
                            ),
                            onClick = {
                                draft = draft.copy(
                                    themePreset = preset.id,
                                    backgroundColor = preset.bg,
                                    auroraColor1 = preset.a1,
                                    auroraColor2 = preset.a2,
                                    auroraColor3 = preset.a3,
                                    accentColor = preset.accent,
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                // Mini live color preview strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(parseHexColor(draft.backgroundColor, Sj.Night))
                        .border(1.5.dp, Sj.GlassBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(draft.backgroundColor, draft.accentColor, draft.auroraColor1, draft.auroraColor2, draft.auroraColor3).forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex, Sj.Gold))
                                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Live Preview", color = Sj.TextMid, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.backgroundColor,
                        onValueChange = { draft = draft.copy(themePreset = "custom", backgroundColor = it.take(7)) },
                        label = { Text("Bg Hex") },
                        singleLine = true,
                        colors = sjTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = draft.accentColor,
                        onValueChange = { draft = draft.copy(themePreset = "custom", accentColor = it.take(7)) },
                        label = { Text("Accent Hex") },
                        singleLine = true,
                        colors = sjTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.auroraColor1,
                        onValueChange = { draft = draft.copy(themePreset = "custom", auroraColor1 = it.take(7)) },
                        label = { Text("Glow 1") },
                        singleLine = true,
                        colors = sjTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = draft.auroraColor2,
                        onValueChange = { draft = draft.copy(themePreset = "custom", auroraColor2 = it.take(7)) },
                        label = { Text("Glow 2") },
                        singleLine = true,
                        colors = sjTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            GlassCard(corner = 26.dp) {
                Text(
                    "Layout & Visual Effects",
                    color = Sj.TextHi,
                    fontFamily = ManlineDisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                )
                Spacer(Modifier.height(12.dp))
                SectionLabel("Charades deck columns")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "1 Col (Wide)", 2 to "2 Col", 3 to "3 Col").forEach { (cols, label) ->
                        SelectChip(
                            text = label,
                            selected = draft.deckColumns == cols,
                            onClick = { draft = draft.copy(deckColumns = cols) },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                SectionLabel("Default round timer")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(45, 60, 90).forEach { sec ->
                        SelectChip(
                            text = "${sec}s",
                            selected = draft.defaultRoundSeconds == sec,
                            onClick = { draft = draft.copy(defaultRoundSeconds = sec) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Kenya Flag rotating card beam", color = Sj.TextHi, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = draft.showKenyaBeam,
                        onCheckedChange = { draft = draft.copy(showKenyaBeam = it) },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Maasai shuka strip on Home", color = Sj.TextHi, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = draft.showShukaStrip,
                        onCheckedChange = { draft = draft.copy(showShukaStrip = it) },
                    )
                }
            }
        }

        item {
            GlassCard(corner = 26.dp) {
                Text(
                    "Live Home Copy & Banner",
                    color = Sj.TextHi,
                    fontFamily = ManlineDisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = draft.homeGreeting,
                    onValueChange = { draft = draft.copy(homeGreeting = it) },
                    label = { Text("Home greeting") },
                    singleLine = true,
                    colors = sjTextFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = draft.homeTagline,
                    onValueChange = { draft = draft.copy(homeTagline = it) },
                    label = { Text("Home tagline") },
                    singleLine = true,
                    colors = sjTextFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = draft.announcementBanner,
                    onValueChange = { draft = draft.copy(announcementBanner = it) },
                    label = { Text("Announcement banner (leave blank to hide)") },
                    singleLine = true,
                    colors = sjTextFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                GradientButton(
                    text = if (isLoading) "Pushing live…" else "Save & push live",
                    emoji = "🚀",
                    colors = SjGradients.Win,
                    enabled = !isLoading,
                    onClick = { onSave(draft) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StatsStudioTab(
    stats: AnalyticsSummary,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SectionLabel("Live Party Analytics")
                SjPill(
                    text = if (isLoading) "Refreshing…" else "↻ Refresh",
                    background = Sj.GlassStrong,
                    onClick = onRefresh,
                )
            }
        }

        // Live Nani Huyu? rooms right now
        item {
            GlassCard(
                corner = 26.dp,
                tint = Sj.Savanna.copy(alpha = 0.14f),
                borderColor = Sj.Savanna.copy(alpha = 0.45f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("● LIVE RIGHT NOW", color = Sj.Savanna, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${stats.activeNaniRooms} Nani Rooms  •  ${stats.activeNaniPlayers} Players",
                            color = Sj.TextHi,
                            fontFamily = ManlineDisplayFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                        )
                    }
                }
            }
        }

        // Round stats 2x2 grid
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricBox("🎭", "${stats.totalCharadesRounds}", "Charades Rounds", Modifier.weight(1f))
                MetricBox("📸", "${stats.totalNaniRounds}", "Nani Rounds", Modifier.weight(1f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricBox("🔥", "${stats.roundsToday}", "Last 24 Hours", Modifier.weight(1f))
                MetricBox("🏆", "${stats.highestScore}", "Highest Score", Modifier.weight(1f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricBox("🎯", "${stats.avgScore}", "Avg Score", Modifier.weight(1f))
                MetricBox("🃏", "${stats.totalDecks} / ${stats.totalCards}", "Decks / Cards", Modifier.weight(1f))
            }
        }

        // Top played decks
        item {
            GlassCard(corner = 26.dp) {
                Text(
                    "🔥 Most Played Decks",
                    color = Sj.TextHi,
                    fontFamily = ManlineDisplayFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                )
                Spacer(Modifier.height(12.dp))
                if (stats.topDecks.isEmpty()) {
                    Text(
                        "No rounds logged yet — play a round of Sijui! to see your squad's favorite decks appear here.",
                        color = Sj.TextMid,
                        fontSize = 14.sp,
                    )
                } else {
                    val maxPlays = stats.topDecks.maxOf { it.plays }.coerceAtLeast(1)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        stats.topDecks.forEachIndexed { idx, deck ->
                            val frac = (deck.plays.toFloat() / maxPlays.toFloat()).coerceIn(0.12f, 1f)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        "#${idx + 1}  ${deck.name}",
                                        color = Sj.TextHi,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                    )
                                    Text(
                                        "${deck.plays} plays",
                                        color = Sj.Gold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                    )
                                }
                                Spacer(Modifier.height(5.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(Sj.GlassStrong),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(frac)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(99.dp))
                                            .background(Brush.horizontalGradient(SjGradients.Sunset)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, corner = 22.dp, padding = PaddingValues(16.dp)) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            color = Sj.TextHi,
            fontFamily = ManlineDisplayFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
        )
        Text(
            label.uppercase(),
            color = Sj.TextLo,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
        )
    }
}
