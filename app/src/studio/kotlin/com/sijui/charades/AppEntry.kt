package com.sijui.charades

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.sijui.charades.data.SijuiApiClient
import com.sijui.charades.ui.studio.StudioScreen
import com.sijui.charades.ui.studio.StudioViewModel

@Composable
fun SijuiAppEntry() {
    val context = LocalContext.current
    val api = remember { SijuiApiClient(context) }
    val studioVm = remember { StudioViewModel(api) }
    StudioScreen(viewModel = studioVm)
}
