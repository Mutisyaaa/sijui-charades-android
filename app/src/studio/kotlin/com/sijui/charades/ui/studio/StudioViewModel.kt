package com.sijui.charades.ui.studio

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijui.charades.data.SijuiApiClient
import com.sijui.charades.data.model.AuthUser
import com.sijui.charades.data.model.CharadesDeck
import com.sijui.charades.data.model.NaniCard
import com.sijui.charades.data.model.NaniCategoryCount
import com.sijui.charades.hardware.ImageCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StudioUiState(
    val user: AuthUser? = null,
    val serverOnline: Boolean? = null,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val error: String? = null,
    val decks: List<CharadesDeck> = emptyList(),
    val categories: List<NaniCategoryCount> = emptyList(),
    val characters: List<NaniCard> = emptyList(),
)

class StudioViewModel(
    val api: SijuiApiClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    init {
        pingAndCheckSession()
    }

    fun pingAndCheckSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(serverOnline = null, isLoading = true, error = null) }
            val online = api.pingHealth()
            val me = if (online) api.currentUser() else null
            _uiState.update { it.copy(serverOnline = online, user = me, isLoading = false) }
            if (me?.role == "admin") {
                loadStudioData()
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, statusMessage = null) }
            runCatching { api.login(email, password) }
                .onSuccess { user ->
                    _uiState.update { it.copy(user = user, isLoading = false) }
                    if (user.role == "admin") {
                        loadStudioData()
                    } else {
                        _uiState.update { it.copy(error = "Signed in as ${user.displayName}, but admin role is required.") }
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message ?: "Login failed") }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            api.logout()
            _uiState.update { StudioUiState(serverOnline = it.serverOnline) }
        }
    }

    fun loadStudioData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val decks = runCatching { api.fetchAdminDecks() }.getOrDefault(emptyList())
            val cats = runCatching { api.fetchNaniCategories() }.getOrDefault(emptyList())
            val chars = runCatching { api.fetchCustomNaniCharacters() }.getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    decks = decks,
                    categories = cats,
                    characters = chars,
                )
            }
        }
    }

    fun createDeckWithWords(
        context: Context,
        name: String,
        description: String,
        coverUri: Uri?,
        bulkWords: String,
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            _uiState.update { it.copy(error = "Enter a deck name.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, statusMessage = null) }
            val coverDataUrl = if (coverUri != null) {
                withContext(Dispatchers.IO) { ImageCodec.uriToDataUrl(context, coverUri, maxDim = 720) }
            } else {
                null
            }
            runCatching {
                val deck = api.createAdminDeck(
                    name = cleanName,
                    description = description.trim(),
                    icon = "🎭",
                    theme = "classic",
                    coverUrl = coverDataUrl,
                )
                val words = bulkWords.lines().map { it.trim() }.filter { it.isNotBlank() }
                val added = if (words.isNotEmpty()) api.bulkAddCards(deck.id, words) else 0
                deck.name to added
            }.onSuccess { (deckName, count) ->
                _uiState.update {
                    it.copy(isLoading = false, statusMessage = "Created '$deckName' with $count words!")
                }
                loadStudioData()
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to create deck") }
            }
        }
    }

    fun bulkAddToExistingDeck(deckId: String, deckName: String, bulkWords: String) {
        val words = bulkWords.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (words.isEmpty()) {
            _uiState.update { it.copy(error = "Paste at least one word.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, statusMessage = null) }
            runCatching { api.bulkAddCards(deckId, words) }
                .onSuccess { count ->
                    _uiState.update { it.copy(isLoading = false, statusMessage = "Added $count words to '$deckName'!") }
                    loadStudioData()
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to add words") }
                }
        }
    }

    fun deleteDeck(deckId: String, deckName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, statusMessage = null) }
            runCatching { api.deleteAdminDeck(deckId) }
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, statusMessage = "Deleted '$deckName'.") }
                    loadStudioData()
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message) }
                }
        }
    }

    fun uploadPhotoCharacter(context: Context, uri: Uri, category: String, characterName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, statusMessage = null) }
            val dataUrl = withContext(Dispatchers.IO) { ImageCodec.uriToDataUrl(context, uri, maxDim = 800) }
            if (dataUrl == null) {
                _uiState.update { it.copy(isLoading = false, error = "Could not read selected image.") }
                return@launch
            }
            val cleanCat = category.trim().ifBlank { "Friends & Family" }
            val cleanName = characterName.trim().ifBlank { "Character" }
            runCatching { api.uploadNaniCharacter(cleanCat, cleanName, dataUrl) }
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, statusMessage = "Uploaded '$cleanName' to $cleanCat!")
                    }
                    loadStudioData()
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message ?: "Upload failed") }
                }
        }
    }

    fun deleteCharacter(id: String) {
        viewModelScope.launch {
            runCatching { api.deleteNaniCharacter(id) }
                .onSuccess {
                    _uiState.update { it.copy(statusMessage = "Photo deleted.") }
                    loadStudioData()
                }
                .onFailure { err -> _uiState.update { it.copy(error = err.message) } }
        }
    }
}
