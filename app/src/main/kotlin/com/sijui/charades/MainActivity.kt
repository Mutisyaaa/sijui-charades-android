package com.sijui.charades

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sijui.charades.data.SijuiApiClient
import com.sijui.charades.hardware.SoundAndHaptics
import com.sijui.charades.ui.charades.CharadesRootScreen
import com.sijui.charades.ui.charades.CharadesViewModel
import com.sijui.charades.ui.home.HomeScreen
import com.sijui.charades.ui.nani.NaniRootScreen
import com.sijui.charades.ui.nani.NaniViewModel
import com.sijui.charades.ui.theme.SijuiTheme

enum class SijuiDestination {
    HOME,
    CHARADES,
    NANI,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent {
            SijuiTheme {
                SijuiApp()
            }
        }
    }
}

@Composable
fun SijuiApp() {
    val context = LocalContext.current
    val api = remember { SijuiApiClient(context) }
    val sound = remember { SoundAndHaptics(context) }

    val charadesVm = remember { CharadesViewModel(api, sound) }
    val naniVm = remember { NaniViewModel(api, sound) }

    var destination by remember { mutableStateOf(SijuiDestination.HOME) }
    var soundEnabled by remember { mutableStateOf(sound.soundEnabled) }

    BackHandler(enabled = destination != SijuiDestination.HOME) {
        when (destination) {
            SijuiDestination.CHARADES -> {
                charadesVm.backToDeckPicker()
                destination = SijuiDestination.HOME
            }
            SijuiDestination.NANI -> {
                naniVm.leaveRoom()
                destination = SijuiDestination.HOME
            }
            SijuiDestination.HOME -> Unit
        }
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            (fadeIn(tween(240)) + scaleIn(initialScale = 0.96f, animationSpec = tween(240))) togetherWith
                fadeOut(tween(180))
        },
        label = "sijui_nav",
    ) { dest ->
        when (dest) {
            SijuiDestination.HOME -> HomeScreen(
                api = api,
                soundEnabled = soundEnabled,
                onToggleSound = {
                    soundEnabled = !soundEnabled
                    sound.soundEnabled = soundEnabled
                    if (soundEnabled) sound.playTick(urgent = false)
                },
                onOpenCharades = { destination = SijuiDestination.CHARADES },
                onOpenNani = {
                    naniVm.loadDecks()
                    destination = SijuiDestination.NANI
                },
            )
            SijuiDestination.CHARADES -> CharadesRootScreen(
                viewModel = charadesVm,
                api = api,
                onExit = { destination = SijuiDestination.HOME },
            )
            SijuiDestination.NANI -> NaniRootScreen(
                viewModel = naniVm,
                onExit = {
                    naniVm.leaveRoom()
                    destination = SijuiDestination.HOME
                },
            )
        }
    }
}
