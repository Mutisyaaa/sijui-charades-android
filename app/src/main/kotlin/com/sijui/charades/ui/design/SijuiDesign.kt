package com.sijui.charades.ui.design

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sijui.charades.data.model.AppConfig
import com.sijui.charades.ui.theme.ManlineDisplayFont
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/* ============================================================
   Sijui "Nairobi Nights" design tokens
   ============================================================ */

object Sj {
    val Night = Color(0xFF0A0812)
    val Night2 = Color(0xFF15101F)
    val Night3 = Color(0xFF211A2E)
    val Gold = Color(0xFFFFC23D)
    val Ember = Color(0xFFFF4D3D)
    val Savanna = Color(0xFF1ED77A)
    val Lagoon = Color(0xFF3D7BFF)
    val Orchid = Color(0xFFB45CFF)
    val Coral = Color(0xFFFF7A59)
    val TextHi = Color(0xFFFFF8EC)
    val TextMid = Color(0xB3FFF8EC)
    val TextLo = Color(0x66FFF8EC)
    val Glass = Color(0x14FFFFFF)
    val GlassStrong = Color(0x24FFFFFF)
    val GlassBorder = Color(0x26FFFFFF)
}

object SjGradients {
    val Sunset = listOf(Color(0xFFFFC23D), Color(0xFFFF5E3A))
    val Violet = listOf(Color(0xFF6A5BFF), Color(0xFFC05CFF))
    val Mint = listOf(Color(0xFF2EE6A8), Color(0xFF0E8F6E))
    val Lagoon = listOf(Color(0xFF4FA3FF), Color(0xFF2A44D9))
    val Rose = listOf(Color(0xFFFF6B8B), Color(0xFFC9184A))
    val Orchid = listOf(Color(0xFFE879F9), Color(0xFF8B2CC9))
    val Win = listOf(Color(0xFF2EE68A), Color(0xFF0F9D58))
    val Lose = listOf(Color(0xFFFF6A5B), Color(0xFFC0262D))
    val Gold = listOf(Color(0xFFFFE27A), Color(0xFFF5A623))
    val Silver = listOf(Color(0xFFE3E8F0), Color(0xFF8A93A6))
    val Bronze = listOf(Color(0xFFF0B27A), Color(0xFFA0522D))
}

fun deckGradient(theme: String?): List<Color> = when (theme?.lowercase()) {
    "sunset" -> SjGradients.Sunset
    "mint" -> SjGradients.Mint
    "berry" -> SjGradients.Orchid
    "red" -> SjGradients.Rose
    "classic" -> SjGradients.Lagoon
    else -> SjGradients.Violet
}

val LocalAppConfig = compositionLocalOf { AppConfig() }

fun parseHexColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return runCatching {
        Color(android.graphics.Color.parseColor(hex.trim()))
    }.getOrDefault(fallback)
}

/* ============================================================
   Backgrounds & decoration
   ============================================================ */

/** Deep night background with slowly drifting aurora glow blobs driven by live AppConfig. */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    colors: List<Color>? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val config = LocalAppConfig.current
    val bgColor = remember(config.backgroundColor) { parseHexColor(config.backgroundColor, Sj.Night) }
    val resolvedColors = colors ?: remember(config.auroraColor1, config.auroraColor2, config.auroraColor3) {
        listOf(
            parseHexColor(config.auroraColor1, Sj.Ember),
            parseHexColor(config.auroraColor2, Sj.Orchid),
            parseHexColor(config.auroraColor3, Sj.Lagoon),
        )
    }

    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart),
        label = "aurora_phase",
    )
    val c0 = resolvedColors.getOrElse(0) { Sj.Ember }
    val c1 = resolvedColors.getOrElse(1) { Sj.Orchid }
    val c2 = resolvedColors.getOrElse(2) { Sj.Lagoon }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .drawBehind {
                val w = size.width
                val h = size.height
                val a = phase * 2f * PI.toFloat()
                fun blob(color: Color, cx: Float, cy: Float, r: Float) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.42f), color.copy(alpha = 0.0f)),
                            center = Offset(cx, cy),
                            radius = r,
                        ),
                        radius = r,
                        center = Offset(cx, cy),
                    )
                }
                blob(c0, w * (0.15f + 0.15f * cos(a)), h * (0.10f + 0.05f * sin(a)), w * 0.85f)
                blob(c1, w * (0.95f + 0.08f * sin(a)), h * (0.48f + 0.07f * cos(a)), w * 0.75f)
                blob(c2, w * (0.25f + 0.12f * sin(a + 1.3f)), h * (0.95f + 0.04f * cos(a)), w * 0.9f)
            },
    ) {
        CompositionLocalProvider(LocalContentColor provides Sj.TextHi) {
            content()
        }
    }
}

/** Maasai-shuka inspired beadwork strip. */
@Composable
fun ShukaStrip(modifier: Modifier = Modifier, height: Dp = 10.dp) {
    val palette = listOf(Sj.Ember, Sj.Gold, Sj.Savanna, Sj.TextHi, Sj.Lagoon, Sj.Ember, Sj.Night)
    Canvas(modifier = modifier.height(height)) {
        val s = size.height
        var x = 0f
        var i = 0
        while (x < size.width) {
            val up = Path().apply {
                moveTo(x, s)
                lineTo(x + s / 2f, 0f)
                lineTo(x + s, s)
                close()
            }
            drawPath(up, palette[i % palette.size])
            val down = Path().apply {
                moveTo(x + s / 2f, 0f)
                lineTo(x + s, s)
                lineTo(x + s * 1.5f, 0f)
                close()
            }
            drawPath(down, palette[(i + 3) % palette.size].copy(alpha = 0.55f))
            x += s
            i++
        }
    }
}

@Composable
fun PulsingRings(modifier: Modifier = Modifier, color: Color = Color.White) {
    val transition = rememberInfiniteTransition(label = "rings")
    val p by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "rings_p",
    )
    Canvas(modifier = modifier) {
        val maxR = size.minDimension / 2f
        for (i in 0 until 3) {
            val f = (p + i / 3f) % 1f
            drawCircle(
                color = color.copy(alpha = (1f - f) * 0.35f),
                radius = maxR * (0.3f + 0.7f * f),
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }
}

/**
 * Rotating glowing Kenya Flag light beam sweeping around the card border,
 * matching the webapp's signature `kenyaBeamSpin` conic-gradient lighting.
 */
@Composable
fun KenyaBeamBorder(
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    borderWidth: Dp = 3.5.dp,
    phaseOffset: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "kenya_beam")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "kenya_beam_angle",
    )

    val config = LocalAppConfig.current
    val outerShape = RoundedCornerShape(cornerRadius)
    val innerShape = RoundedCornerShape((cornerRadius - borderWidth).coerceAtLeast(8.dp))

    if (!config.showKenyaBeam) {
        Box(
            modifier = modifier
                .clip(outerShape)
                .border(1.5.dp, Sj.GlassBorder, outerShape),
            content = content,
        )
        return
    }

    val beamBrush = remember {
        Brush.sweepGradient(
            0.00f to Color.Transparent,
            0.52f to Color.Transparent,
            0.58f to Color(0xFF000000),
            0.63f to Color(0xFFFFFFFF),
            0.67f to Color(0xFFC1272D),
            0.78f to Color(0xFFC1272D),
            0.82f to Color(0xFFFFFFFF),
            0.86f to Color(0xFF008A00),
            0.93f to Color(0xFF008A00),
            0.97f to Color.Transparent,
            1.00f to Color.Transparent,
        )
    }

    Box(
        modifier = modifier
            .clip(outerShape)
            .background(Color(0xFF140F0D))
            .drawBehind {
                val diag = kotlin.math.hypot(size.width.toDouble(), size.height.toDouble()).toFloat()
                val topLeft = Offset((size.width - diag) / 2f, (size.height - diag) / 2f)
                val rot = (angle + phaseOffset * 360f) % 360f
                // Subtle static dark glass rim so the unlit side still looks sleek
                drawRect(color = Color.White.copy(alpha = 0.10f))
                rotate(degrees = rot, pivot = center) {
                    drawRect(
                        brush = beamBrush,
                        topLeft = topLeft,
                        size = Size(diag, diag),
                    )
                }
            }
            .padding(borderWidth),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(innerShape),
            content = content,
        )
    }
}

@Composable
fun FloatingEmoji(
    emoji: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    amplitude: Dp = 8.dp,
    periodMs: Int = 2600,
) {
    val transition = rememberInfiniteTransition(label = "float")
    val p by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float_p",
    )
    Text(
        text = emoji,
        fontSize = fontSize,
        modifier = modifier.graphicsLayer {
            translationY = p * amplitude.toPx()
            rotationZ = p * 6f
        },
    )
}

/* ============================================================
   Motion helpers
   ============================================================ */

fun Modifier.bounceClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 600f),
        label = "bounce",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

/** Staggered spring-in entrance for lists of hero elements. */
fun Modifier.staggerIn(index: Int): Modifier = composed {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 85L)
        anim.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 210f))
    }
    this.graphicsLayer {
        alpha = anim.value.coerceIn(0f, 1f)
        translationY = (1f - anim.value) * 56.dp.toPx()
    }
}

/* ============================================================
   Surfaces & controls
   ============================================================ */

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    padding: PaddingValues = PaddingValues(18.dp),
    tint: Color = Sj.Glass,
    borderColor: Color = Sj.GlassBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier)
            .clip(shape)
            .background(tint)
            .border(1.dp, borderColor, shape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = SjGradients.Sunset,
    contentColor: Color = Sj.Night,
    enabled: Boolean = true,
    emoji: String? = null,
    height: Dp = 60.dp,
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .height(height)
            .bounceClick(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(
                elevation = if (enabled) 18.dp else 0.dp,
                shape = shape,
                ambientColor = colors.last(),
                spotColor = colors.last(),
            )
            .clip(shape)
            .background(Brush.horizontalGradient(colors))
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!emoji.isNullOrBlank()) {
                Text(emoji, fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text.uppercase(),
                color = contentColor,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                letterSpacing = 1.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    height: Dp = 60.dp,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .height(height)
            .bounceClick(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(Sj.Glass)
            .border(1.5.dp, Sj.GlassBorder, shape)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!emoji.isNullOrBlank()) {
                Text(emoji, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                color = Sj.TextHi,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun SjPill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = Sj.GlassStrong,
    contentColor: Color = Sj.TextHi,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier)
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.6.sp,
            maxLines = 1,
        )
    }
}

@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = SjGradients.Sunset,
) {
    val shape = RoundedCornerShape(percent = 50)
    val textColor by animateColorAsState(if (selected) Sj.Night else Sj.TextMid, label = "chip_text")
    Box(
        modifier = modifier
            .bounceClick(onClick = onClick)
            .clip(shape)
            .background(
                if (selected) Brush.horizontalGradient(colors) else Brush.horizontalGradient(listOf(Sj.Glass, Sj.Glass)),
            )
            .border(1.dp, if (selected) Color.Transparent else Sj.GlassBorder, shape)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
    }
}

@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Color> = SjGradients.Sunset,
) {
    val shape = RoundedCornerShape(percent = 50)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(shape)
            .background(Sj.Glass)
            .border(1.dp, Sj.GlassBorder, shape)
            .padding(5.dp),
    ) {
        val segW = maxWidth / options.size
        val offsetX by animateDpAsState(
            targetValue = segW * selectedIndex,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
            label = "seg_offset",
        )
        Box(
            modifier = Modifier
                .offset(x = offsetX)
                .width(segW)
                .fillMaxHeight()
                .shadow(10.dp, shape, ambientColor = colors.last(), spotColor = colors.last())
                .clip(shape)
                .background(Brush.horizontalGradient(colors)),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                val selected = i == selectedIndex
                val textColor by animateColorAsState(
                    if (selected) Sj.Night else Sj.TextMid,
                    label = "seg_text_$i",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(i) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = Sj.TextHi,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(size)
            .bounceClick(onClick = onClick)
            .clip(shape)
            .background(Sj.GlassStrong)
            .border(1.dp, Sj.GlassBorder, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(
                    subtitle.uppercase(),
                    color = Sj.TextLo,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                )
            }
            Text(
                title,
                color = Sj.TextHi,
                fontFamily = ManlineDisplayFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                lineHeight = 28.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = Sj.TextLo,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.sp,
        modifier = modifier,
    )
}

@Composable
fun sjTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Sj.TextHi,
    unfocusedTextColor = Sj.TextHi,
    focusedContainerColor = Color(0x0FFFFFFF),
    unfocusedContainerColor = Color(0x0AFFFFFF),
    focusedBorderColor = Sj.Gold,
    unfocusedBorderColor = Sj.GlassBorder,
    focusedLabelColor = Sj.Gold,
    unfocusedLabelColor = Sj.TextLo,
    focusedPlaceholderColor = Sj.TextLo,
    unfocusedPlaceholderColor = Sj.TextLo,
    cursorColor = Sj.Gold,
)

/* ============================================================
   Game widgets
   ============================================================ */

@Composable
fun TimerRing(
    secondsLeft: Int,
    total: Int,
    modifier: Modifier = Modifier,
    diameter: Dp = 84.dp,
) {
    val progress by animateFloatAsState(
        targetValue = if (total > 0) secondsLeft / total.toFloat() else 0f,
        animationSpec = tween(950, easing = LinearEasing),
        label = "timer_ring",
    )
    val urgent = secondsLeft <= 10
    val ringColor by animateColorAsState(if (urgent) Sj.Gold else Color.White, label = "ring_color")
    val pulse = rememberInfiniteTransition(label = "timer_pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (urgent) 1.08f else 1f,
        animationSpec = infiniteRepeatable(tween(480), RepeatMode.Reverse),
        label = "timer_pulse_s",
    )
    Box(
        modifier = modifier
            .size(diameter)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2f + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(
                color = Color.White.copy(alpha = 0.18f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke),
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            "$secondsLeft",
            color = ringColor,
            fontWeight = FontWeight.Black,
            fontSize = (diameter.value * 0.32f).sp,
        )
    }
}

private data class ConfettiParticle(
    val x0: Float,
    val vx: Float,
    val vy: Float,
    val rot: Float,
    val spin: Float,
    val w: Float,
    val h: Float,
    val color: Color,
)

@Composable
fun ConfettiBurst(modifier: Modifier = Modifier, count: Int = 110, trigger: Any = Unit) {
    val palette = listOf(Sj.Gold, Sj.Ember, Sj.Savanna, Sj.Lagoon, Sj.Orchid, Sj.TextHi, Sj.Coral)
    val particles = remember(trigger) {
        List(count) {
            ConfettiParticle(
                x0 = Random.nextFloat(),
                vx = (Random.nextFloat() - 0.5f) * 0.5f,
                vy = Random.nextFloat() * 0.35f - 0.25f,
                rot = Random.nextFloat() * 360f,
                spin = Random.nextFloat() * 2f - 1f,
                w = 10f + Random.nextFloat() * 14f,
                h = 6f + Random.nextFloat() * 10f,
                color = palette[Random.nextInt(palette.size)],
            )
        }
    }
    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.animateTo(1f, tween(3600, easing = LinearOutSlowInEasing))
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val fade = if (t < 0.7f) 1f else ((1f - t) / 0.3f).coerceIn(0f, 1f)
        particles.forEach { p ->
            val x = size.width * p.x0 + p.vx * size.width * t
            val y = -30f + (p.vy * t + 1.3f * t * t) * size.height
            rotate(degrees = p.rot + 720f * t * p.spin, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = fade),
                    topLeft = Offset(x - p.w / 2f, y - p.h / 2f),
                    size = Size(p.w, p.h),
                )
            }
        }
    }
}

@Composable
fun AnimatedCounter(
    target: Int,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 72.sp,
    color: Color = Sj.TextHi,
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val value by animateIntAsState(
        targetValue = if (started) target else 0,
        animationSpec = tween(1300, easing = FastOutSlowInEasing),
        label = "counter",
    )
    Text(
        text = "$value",
        modifier = modifier,
        color = color,
        fontFamily = ManlineDisplayFont,
        fontSize = fontSize,
        lineHeight = fontSize,
    )
}

@Composable
fun AvatarBubble(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    badge: String? = null,
    ring: Boolean = false,
) {
    val palette = listOf(SjGradients.Sunset, SjGradients.Violet, SjGradients.Mint, SjGradients.Lagoon, SjGradients.Rose, SjGradients.Orchid)
    val gradient = palette[(name.hashCode() and 0x7fffffff) % palette.size]
    val initials = name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .then(if (ring) Modifier.border(3.dp, Sj.Gold, CircleShape) else Modifier)
                .padding(if (ring) 4.dp else 0.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials,
                color = Sj.Night,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.34f).sp,
            )
        }
        if (badge != null) {
            Text(
                badge,
                fontSize = (size.value * 0.36f).sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp),
            )
        }
    }
}

/** Full-screen gradient countdown with pulsing rings and spring number swaps. */
@Composable
fun BigCountdown(
    number: Int,
    colors: List<Color>,
    headline: String,
    subline: String,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors, Offset.Zero, Offset.Infinite)),
        contentAlignment = Alignment.Center,
    ) {
        PulsingRings(modifier = Modifier.size(340.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                headline,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                subline,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            AnimatedContent(
                targetState = number,
                transitionSpec = {
                    (scaleIn(initialScale = 1.9f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.4f) + fadeOut())
                },
                label = "big_countdown",
            ) { n ->
                Text(
                    "$n",
                    color = Color.White,
                    fontFamily = ManlineDisplayFont,
                    fontSize = 136.sp,
                    lineHeight = 140.sp,
                )
            }
            footer()
        }
    }
}

/** Crisp white-framed scannable QR code rendered locally via ZXing. */
@Composable
fun QrCodeBox(
    data: String,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
) {
    val qrBitmap = remember(data) {
        runCatching {
            val px = 420
            val matrix = com.google.zxing.qrcode.QRCodeWriter().encode(
                data,
                com.google.zxing.BarcodeFormat.QR_CODE,
                px,
                px,
                mapOf(com.google.zxing.EncodeHintType.MARGIN to 1),
            )
            val bmp = android.graphics.Bitmap.createBitmap(px, px, android.graphics.Bitmap.Config.RGB_565)
            val dark = android.graphics.Color.parseColor("#0A0812")
            val light = android.graphics.Color.WHITE
            for (y in 0 until px) {
                for (x in 0 until px) {
                    bmp.setPixel(x, y, if (matrix.get(x, y)) dark else light)
                }
            }
            bmp.asImageBitmap()
        }.getOrNull()
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap,
                contentDescription = "Scan QR to join room",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
