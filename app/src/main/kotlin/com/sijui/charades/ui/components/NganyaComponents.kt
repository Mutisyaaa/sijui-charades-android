package com.sijui.charades.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sijui.charades.ui.theme.SijuiInk
import com.sijui.charades.ui.theme.SijuiPaper
import com.sijui.charades.ui.theme.SijuiWhite

/**
 * Cream paper background with subtle dot-grid texture matching Sijui's Nganya Sticker theme.
 */
@Composable
fun PaperDotBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SijuiPaper),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 22.dp.toPx()
            val radius = 1.3.dp.toPx()
            val dotColor = SijuiInk.copy(alpha = 0.09f)
            var y = spacing / 2f
            while (y < size.height) {
                var x = spacing / 2f
                while (x < size.width) {
                    drawCircle(color = dotColor, radius = radius, center = Offset(x, y))
                    x += spacing
                }
                y += spacing
            }
        }
        content()
    }
}

/**
 * Neo-brutalist sticker card with thick ink outline and hard offset drop shadow.
 */
@Composable
fun NganyaCard(
    modifier: Modifier = Modifier,
    containerColor: Color = SijuiWhite,
    shadowOffset: Dp = 5.dp,
    cornerRadius: Dp = 18.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffset = if (isPressed && onClick != null) shadowOffset * 0.65f else 0.dp

    Box(modifier = modifier.padding(bottom = shadowOffset, end = 3.dp)) {
        // Hard ink shadow layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = shadowOffset)
                .clip(shape)
                .background(SijuiInk),
        )
        // Foreground card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = if (isPressed && onClick != null) 2.dp else 0.dp, y = pressOffset)
                .clip(shape)
                .background(containerColor)
                .border(3.dp, SijuiInk, shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(contentPadding),
            content = content,
        )
    }
}

/**
 * Chunky neo-brutalist button with tactile press shift.
 */
@Composable
fun NganyaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = SijuiWhite,
    contentColor: Color = SijuiInk,
    enabled: Boolean = true,
    leadingEmoji: String? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shadowY = 4.dp
    val shiftY = if (isPressed && enabled) shadowY else 0.dp

    Box(modifier = modifier.padding(bottom = shadowY)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = shadowY)
                .clip(shape)
                .background(if (enabled) SijuiInk else SijuiInk.copy(alpha = 0.35f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .offset(y = shiftY)
                .clip(shape)
                .background(if (enabled) containerColor else containerColor.copy(alpha = 0.55f))
                .border(3.dp, if (enabled) SijuiInk else SijuiInk.copy(alpha = 0.4f), shape)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!leadingEmoji.isNullOrBlank()) {
                Text(text = leadingEmoji, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun NganyaTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            Box(modifier = Modifier.padding(bottom = 3.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .offset(y = 3.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SijuiInk),
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SijuiWhite)
                        .border(3.dp, SijuiInk, RoundedCornerShape(14.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "←",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = SijuiInk,
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = SijuiInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun NganyaPill(
    text: String,
    containerColor: Color,
    contentColor: Color = SijuiInk,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(2.dp, SijuiInk),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}
