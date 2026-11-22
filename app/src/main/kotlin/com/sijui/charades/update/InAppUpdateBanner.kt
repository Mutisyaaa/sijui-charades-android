package com.sijui.charades.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sijui.charades.data.AppUpdateInfo
import com.sijui.charades.data.SijuiApiClient
import com.sijui.charades.ui.design.GlassCard
import com.sijui.charades.ui.design.Sj
import com.sijui.charades.ui.design.SjPill
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun InAppUpdateBanner(
    update: AppUpdateInfo,
    api: SijuiApiClient,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val progress by InAppUpdateManager.state.collectAsState()

    LaunchedEffect(Unit) {
        InAppUpdateManager.cleanupOldApks(context)
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        scope.launch {
            InAppUpdateManager.resumeInstallIfPermitted(context)
        }
    }

    val isClickable = progress is UpdateProgressState.Idle ||
        progress is UpdateProgressState.NeedsInstallPermission ||
        progress is UpdateProgressState.Failed

    GlassCard(
        corner = 20.dp,
        padding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
        tint = when (progress) {
            is UpdateProgressState.Failed -> Sj.Ember.copy(alpha = 0.16f)
            else -> Sj.Savanna.copy(alpha = 0.18f)
        },
        borderColor = when (progress) {
            is UpdateProgressState.Failed -> Sj.Ember.copy(alpha = 0.55f)
            else -> Sj.Savanna.copy(alpha = 0.55f)
        },
        onClick = if (isClickable) {
            {
                when (progress) {
                    is UpdateProgressState.Idle,
                    is UpdateProgressState.Failed,
                    -> {
                        scope.launch {
                            InAppUpdateManager.downloadAndInstall(context, api, update)
                        }
                    }
                    is UpdateProgressState.NeedsInstallPermission -> {
                        settingsLauncher.launch(InAppUpdateManager.unknownSourcesSettingsIntent(context))
                    }
                    else -> Unit
                }
            }
        } else {
            null
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        when (val st = progress) {
            is UpdateProgressState.Idle -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "✨ Update ${update.latestVersion} available",
                            color = Sj.TextHi,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                        )
                        Text(
                            "Tap to download & install right inside the app",
                            color = Sj.TextMid,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    SjPill("Update", background = Sj.Savanna, contentColor = Sj.Night)
                }
            }

            is UpdateProgressState.Downloading -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Downloading ${update.latestVersion}…",
                            color = Sj.TextHi,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                        )
                        val sizeText = if (st.totalMb > 0f) {
                            String.format(Locale.US, "%d%% • %.1f / %.1f MB", st.percent, st.downloadedMb, st.totalMb)
                        } else {
                            String.format(Locale.US, "%.1f MB", st.downloadedMb)
                        }
                        Text(
                            sizeText,
                            color = Sj.Savanna,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    if (st.totalMb > 0f) {
                        LinearProgressIndicator(
                            progress = { (st.percent / 100f).coerceIn(0f, 1f) },
                            color = Sj.Savanna,
                            trackColor = Sj.GlassBorder,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(6.dp)),
                        )
                    } else {
                        LinearProgressIndicator(
                            color = Sj.Savanna,
                            trackColor = Sj.GlassBorder,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(6.dp)),
                        )
                    }
                }
            }

            is UpdateProgressState.NeedsInstallPermission -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Allow in-app installs",
                            color = Sj.TextHi,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                        )
                        Text(
                            "Enable once so Sijui can update & reopen itself",
                            color = Sj.TextMid,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    SjPill("Allow", background = Sj.Gold, contentColor = Sj.Night)
                }
            }

            is UpdateProgressState.Installing -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        color = Sj.Savanna,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Installing ${update.latestVersion}…",
                            color = Sj.TextHi,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                        )
                        Text(
                            "Confirm the prompt — app will reopen automatically",
                            color = Sj.TextMid,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            is UpdateProgressState.Failed -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Update couldn't finish",
                            color = Sj.TextHi,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                        )
                        Text(
                            st.message,
                            color = Sj.TextMid,
                            fontSize = 12.sp,
                            maxLines = 2,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    SjPill("Retry", background = Sj.Ember, contentColor = Sj.TextHi)
                }
            }
        }
    }
}
