package com.sijui.charades

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sijui.charades.data.SijuiApiClient
import com.sijui.charades.hardware.SoundAndHaptics
import com.sijui.charades.ui.charades.CharadesRootScreen
import com.sijui.charades.ui.charades.CharadesViewModel
import com.sijui.charades.ui.design.LocalAppConfig
import com.sijui.charades.ui.home.HomeScreen
import com.sijui.charades.ui.nani.NaniRootScreen
import com.sijui.charades.ui.nani.NaniViewModel

private enum class PlayerDestination {
    HOME,
    CHARADES,
    NANI,
}

@Composable
fun SijuiAppEntry() {
    val context = LocalContext.current
    val api = remember { SijuiApiClient(context) }
    val sound = remember { SoundAndHaptics(context) }

    val charadesVm = remember { CharadesViewModel(api, sound) }
    val naniVm = remember { NaniViewModel(api, sound) }

    val deepLinkRoomCode = remember {
        val uri = (context as? android.app.Activity)?.intent?.data
        val fromQuery = uri?.getQueryParameter("nani")
        val fromPath = if (uri?.scheme == "sijui") uri.lastPathSegment else null
        (fromQuery ?: fromPath)?.trim()?.uppercase()?.takeIf { it.length >= 4 }
    }

    var destination by remember {
        mutableStateOf(if (deepLinkRoomCode != null) PlayerDestination.NANI else PlayerDestination.HOME)
    }
    var soundEnabled by remember { mutableStateOf(sound.soundEnabled) }
    var appConfig by remember { mutableStateOf(api.loadCachedAppConfig()) }

    LaunchedEffect(Unit) {
        charadesVm.setRoundSeconds(appConfig.defaultRoundSeconds)
        runCatching { api.fetchAppConfig() }.onSuccess { live ->
            appConfig = live
            charadesVm.setRoundSeconds(live.defaultRoundSeconds)
        }
    }

    BackHandler(enabled = destination != PlayerDestination.HOME) {
        when (destination) {
            PlayerDestination.CHARADES -> {
                charadesVm.backToDeckPicker()
                destination = PlayerDestination.HOME
            }
            PlayerDestination.NANI -> {
                naniVm.leaveRoom()
                destination = PlayerDestination.HOME
            }
            PlayerDestination.HOME -> Unit
        }
    }

    CompositionLocalProvider(LocalAppConfig provides appConfig) {
        AnimatedContent(
            targetState = destination,
            transitionSpec = {
                (fadeIn(tween(240)) + scaleIn(initialScale = 0.96f, animationSpec = tween(240))) togetherWith
                    fadeOut(tween(180))
            },
            label = "player_nav",
        ) { dest ->
            when (dest) {
                PlayerDestination.HOME -> HomeScreen(
                    api = api,
                    soundEnabled = soundEnabled,
                    onToggleSound = {
                        soundEnabled = !soundEnabled
                        sound.soundEnabled = soundEnabled
                        if (soundEnabled) sound.playTick(urgent = false)
                    },
                    onOpenCharades = {
                        charadesVm.refreshDecks()
                        destination = PlayerDestination.CHARADES
                    },
                    onOpenNani = {
                        naniVm.loadDecks()
                        destination = PlayerDestination.NANI
                    },
                )
                PlayerDestination.CHARADES -> CharadesRootScreen(
                    viewModel = charadesVm,
                    api = api,
                    onExit = { destination = PlayerDestination.HOME },
                )
                PlayerDestination.NANI -> NaniRootScreen(
                    viewModel = naniVm,
                    initialJoinCode = deepLinkRoomCode,
                    onExit = {
                        naniVm.leaveRoom()
                        destination = PlayerDestination.HOME
                    },
                )
            }
        }
    }
}
