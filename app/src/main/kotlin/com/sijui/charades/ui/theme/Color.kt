package com.sijui.charades.ui.theme

import androidx.compose.ui.graphics.Color

// Sijui Charades — "Nganya Sticker" Neo-Brutalist Palette
val SijuiInk = Color(0xFF17120F)
val SijuiPaper = Color(0xFFF7EFDC)
val SijuiPaperAlt = Color(0xFFEDE0C2)
val SijuiWhite = Color(0xFFFFFFFF)

val SijuiAmber = Color(0xFFFFB81C)
val SijuiAmberDeep = Color(0xFFE39A00)
val SijuiRed = Color(0xFFE5322D)
val SijuiRedDeep = Color(0xFFB3201C)
val SijuiBlue = Color(0xFF2255D9)
val SijuiBlueDeep = Color(0xFF1639A0)
val SijuiGreen = Color(0xFF1FA45B)
val SijuiGreenDeep = Color(0xFF137A42)
val SijuiBerry = Color(0xFF9B2C7B)
val SijuiInkMuted = Color(0x9E17120F)

fun deckThemeColor(theme: String?): Color = when (theme?.lowercase()) {
    "sunset" -> SijuiAmber
    "mint" -> SijuiGreen
    "berry" -> SijuiBerry
    "red" -> SijuiRed
    "blue" -> SijuiBlue
    else -> SijuiBlue
}
