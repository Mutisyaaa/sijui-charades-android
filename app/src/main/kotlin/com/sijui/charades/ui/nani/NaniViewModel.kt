package com.sijui.charades.ui.nani

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijui.charades.data.SijuiApiClient
import com.sijui.charades.data.model.NaniDeck
import com.sijui.charades.data.model.NaniRoom
import com.sijui.charades.hardware.SoundAndHaptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class NaniUiState(
    val decks: List<NaniDeck> = emptyList(),
    val selectedDeckId: String = "all",
    val isLoading: Boolean = false,
    val error: String? = null,
    val roomCode: String? = null,
    val playerId: String? = null,
    val room: NaniRoom? = null,
    val countdownSecondsLeft: Int = 5,
)

class NaniViewModel(
    val api: SijuiApiClient,
    private val sound: SoundAndHaptics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NaniUiState())
    val uiState: StateFlow<NaniUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var countdownJob: Job? = null
    private var lastStatus: String? = null

    init {
        loadDecks()
    }

    fun loadDecks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { api.fetchNaniDecks() }
                .onSuccess { decks ->
                    _uiState.update { state ->
                        val nextSelected = if (decks.any { it.id == state.selectedDeckId }) {
                            state.selectedDeckId
                        } else {
                            decks.firstOrNull()?.id ?: "all"
                        }
                        state.copy(decks = decks, selectedDeckId = nextSelected, isLoading = false)
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Could not load photo packs: ${err.message}",
                        )
                    }
                }
        }
    }

    fun selectDeck(deckId: String) {
        _uiState.update { it.copy(selectedDeckId = deckId) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun createRoom(hostName: String) {
        val cleanName = hostName.trim().ifBlank { "Host" }
        val deckId = _uiState.value.selectedDeckId.ifBlank { "all" }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { api.createNaniRoom(cleanName, deckId) }
                .onSuccess { res ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            roomCode = res.roomCode,
                            playerId = res.playerId,
                            room = res.room,
                        )
                    }
                    handleRoomTransition(res.room)
                    startPolling()
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to create room") }
                }
        }
    }

    fun joinRoom(roomCode: String, playerName: String) {
        val cleanCode = roomCode.trim().uppercase()
        val cleanName = playerName.trim()
        if (cleanName.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your name.") }
            return
        }
        if (cleanCode.length < 4) {
            _uiState.update { it.copy(error = "Enter the 4-letter room code.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { api.joinNaniRoom(cleanCode, cleanName) }
                .onSuccess { res ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            roomCode = res.roomCode,
                            playerId = res.playerId,
                            room = res.room,
                        )
                    }
                    handleRoomTransition(res.room)
                    startPolling()
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message ?: "Could not join room") }
                }
        }
    }

    fun startRound() {
        val code = _uiState.value.roomCode ?: return
        val pid = _uiState.value.playerId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { api.startNaniRound(code, pid, _uiState.value.selectedDeckId) }
                .onSuccess { room ->
                    _uiState.update { it.copy(isLoading = false, room = room) }
                    handleRoomTransition(room)
                    viewModelScope.launch {
                        api.logGameEvent(eventType = "nani_round", deckName = room.deckName)
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message ?: "Could not start round") }
                }
        }
    }

    fun submitGuessed() {
        val code = _uiState.value.roomCode ?: return
        val pid = _uiState.value.playerId ?: return
        sound.playCorrect()
        viewModelScope.launch {
            runCatching { api.submitNaniGuess(code, pid) }
                .onSuccess { room ->
                    _uiState.update { it.copy(room = room) }
                    handleRoomTransition(room)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(error = err.message) }
                }
        }
    }

    fun restartRound() {
        val code = _uiState.value.roomCode ?: return
        val pid = _uiState.value.playerId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { api.restartNaniRound(code, pid) }
                .onSuccess { room ->
                    _uiState.update { it.copy(isLoading = false, room = room) }
                    handleRoomTransition(room)
                    viewModelScope.launch {
                        api.logGameEvent(eventType = "nani_round", deckName = room.deckName)
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message) }
                }
        }
    }

    fun leaveRoom() {
        val code = _uiState.value.roomCode
        val pid = _uiState.value.playerId
        stopPolling()
        countdownJob?.cancel()
        lastStatus = null
        _uiState.update {
            it.copy(roomCode = null, playerId = null, room = null, error = null)
        }
        if (code != null && pid != null) {
            viewModelScope.launch { api.leaveNaniRoom(code, pid) }
        }
    }

    private fun startPolling() {
        stopPolling()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val code = _uiState.value.roomCode ?: break
                val pid = _uiState.value.playerId ?: break
                runCatching { api.pollNaniRoom(code, pid) }
                    .onSuccess { room ->
                        _uiState.update { it.copy(room = room) }
                        handleRoomTransition(room)
                    }
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun handleRoomTransition(room: NaniRoom) {
        val prev = lastStatus
        lastStatus = room.status

        if (room.status == "countdown" && prev != "countdown") {
            sound.playTick(urgent = true)
            startCountdownTicker(room.countdownEndsAt)
        } else if (room.status != "countdown") {
            countdownJob?.cancel()
        }

        if (room.status == "ended" && prev != "ended") {
            val myPlayer = room.players.firstOrNull { it.id == _uiState.value.playerId }
            if (myPlayer?.isLastPlace == true) {
                sound.playWahWahRoast()
            } else {
                sound.playCelebration()
            }
        }
    }

    private fun startCountdownTicker(endsAtMs: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                val left = (((endsAtMs - System.currentTimeMillis()) + 999L) / 1000L)
                    .toInt()
                    .coerceIn(0, 5)
                _uiState.update { it.copy(countdownSecondsLeft = left) }
                if (left <= 0) break
                sound.playTick(urgent = left <= 2)
                delay(500L)
            }
        }
    }

    override fun onCleared() {
        stopPolling()
        countdownJob?.cancel()
        super.onCleared()
    }
}
