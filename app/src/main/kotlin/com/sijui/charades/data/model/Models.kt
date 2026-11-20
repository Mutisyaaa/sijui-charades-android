package com.sijui.charades.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

@Serializable
data class CharadesCard(
    val id: String = "",
    val text: String,
    val category: String = "General",
    val difficulty: String = "medium",
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class CharadesDeck(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: String = "🎭",
    val theme: String = "classic",
    @SerialName("cover_url") val coverUrl: String? = null,
    val cards: List<CharadesCard> = emptyList(),
)

@Serializable
data class NaniDeck(
    val id: String,
    val name: String,
    val description: String = "",
    val icon: String = "📸",
    val count: Int = 0,
    val categoryName: String? = null,
)

@Serializable
data class NaniCard(
    val id: String = "",
    val name: String = "Character",
    val category: String = "General",
    val hint: String = "",
    val avatar: String = "📸",
    val image: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
) {
    val resolvedImage: String?
        get() = image?.takeIf { it.isNotBlank() } ?: imageUrl?.takeIf { it.isNotBlank() }
}

@Serializable
data class NaniPlayer(
    val id: String,
    val name: String,
    val isHost: Boolean = false,
    val guessed: Boolean = false,
    val rank: JsonPrimitive? = null,
    val guessedAt: Long? = null,
    val card: NaniCard? = null,
) {
    val isLastPlace: Boolean
        get() = rank?.isString == true && rank.content.equals("last", ignoreCase = true)

    val rankNumber: Int?
        get() = rank?.intOrNull
}

@Serializable
data class NaniRoom(
    val code: String,
    val hostId: String = "",
    val deckId: String = "all",
    val deckName: String = "Nani Huyu?",
    val status: String = "lobby",
    val countdownEndsAt: Long = 0L,
    val startedAt: Long = 0L,
    val nextRank: Int = 1,
    val isHost: Boolean = false,
    val myPlayerId: String? = null,
    val myCard: NaniCard? = null,
    val players: List<NaniPlayer> = emptyList(),
)

@Serializable
data class CreateOrJoinRoomResponse(
    val roomCode: String,
    val playerId: String,
    val room: NaniRoom,
)

@Serializable
data class AuthUser(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val email: String,
    val role: String = "player",
)

@Serializable
data class AuthResponse(
    val user: AuthUser,
)

@Serializable
data class NaniCategoryCount(
    val name: String,
    val count: Int = 0,
)

@Serializable
data class ApiErrorResponse(
    val error: String? = null,
)

@Serializable
data class BulkCardsResponse(
    val count: Int = 0,
)
