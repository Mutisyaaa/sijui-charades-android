package com.sijui.charades.ui.charades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sijui.charades.data.DefaultDecks
import com.sijui.charades.data.SijuiApiClient
import com.sijui.charades.data.model.CharadesCard
import com.sijui.charades.data.model.CharadesDeck
import com.sijui.charades.hardware.SoundAndHaptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CharadesMode {
    QUICK_PLAY,
    TEAM_BATTLE,
}

enum class CharadesPhase {
    DECK_PICKER,
    COUNTDOWN,
    PLAYING,
    SUMMARY,
}

enum class FlashFeedback {
    NONE,
    CORRECT,
    PASS,
}

data class RoundWordResult(
    val word: String,
    val guessed: Boolean,
)

data class CharadesUiState(
    val decks: List<CharadesDeck> = DefaultDecks.withRandomMix(DefaultDecks.all),
    val isLoadingDecks: Boolean = false,
    val isOfflineFallback: Boolean = true,
    val mode: CharadesMode = CharadesMode.QUICK_PLAY,
    val roundSeconds: Int = 60,
    val teamAName: String = "Team A",
    val teamBName: String = "Team B",
    val teamAScore: Int = 0,
    val teamBScore: Int = 0,
    val currentTeamIndex: Int = 0, // 0 = Team A, 1 = Team B
    val totalRoundsPerTeam: Int = 2,
    val completedTurns: Int = 0,
    val selectedDeck: CharadesDeck? = null,
    val phase: CharadesPhase = CharadesPhase.DECK_PICKER,
    val countdownValue: Int = 3,
    val secondsLeft: Int = 60,
    val currentCard: CharadesCard? = null,
    val roundResults: List<RoundWordResult> = emptyList(),
    val flash: FlashFeedback = FlashFeedback.NONE,
) {
    val roundScore: Int get() = roundResults.count { it.guessed }
    val activeTeamName: String get() = if (currentTeamIndex == 0) teamAName else teamBName
    val isTeamBattleFinished: Boolean
        get() = mode == CharadesMode.TEAM_BATTLE && completedTurns >= totalRoundsPerTeam * 2
}

class CharadesViewModel(
    private val api: SijuiApiClient,
    private val sound: SoundAndHaptics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharadesUiState())
    val uiState: StateFlow<CharadesUiState> = _uiState.asStateFlow()

    private var shuffledCards: MutableList<CharadesCard> = mutableListOf()
    private var timerJob: Job? = null
    private var flashJob: Job? = null

    init {
        val cached = api.loadCachedDecks()
        if (cached.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    decks = DefaultDecks.withRandomMix(cached),
                    isOfflineFallback = false,
                )
            }
        }
        refreshDecks()
    }

    fun refreshDecks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDecks = true) }
            var remote = runCatching { api.fetchDecks() }.getOrNull()
            // Retry once if Render free tier was waking from cold start
            if (remote.isNullOrEmpty()) {
                delay(2500L)
                remote = runCatching { api.fetchDecks() }.getOrNull()
            }
            if (!remote.isNullOrEmpty()) {
                _uiState.update {
                    it.copy(
                        decks = DefaultDecks.withRandomMix(remote),
                        isLoadingDecks = false,
                        isOfflineFallback = false,
                    )
                }
            } else {
                val cached = api.loadCachedDecks()
                _uiState.update {
                    it.copy(
                        decks = DefaultDecks.withRandomMix(if (cached.isNotEmpty()) cached else DefaultDecks.all),
                        isLoadingDecks = false,
                        isOfflineFallback = cached.isEmpty(),
                    )
                }
            }
        }
    }

    fun setMode(mode: CharadesMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                teamAScore = 0,
                teamBScore = 0,
                currentTeamIndex = 0,
                completedTurns = 0,
            )
        }
    }

    fun setRoundSeconds(seconds: Int) {
        _uiState.update { it.copy(roundSeconds = seconds) }
    }

    fun setTeamNames(teamA: String, teamB: String) {
        _uiState.update {
            it.copy(
                teamAName = teamA.ifBlank { "Team A" },
                teamBName = teamB.ifBlank { "Team B" },
            )
        }
    }

    fun startDeck(deck: CharadesDeck) {
        if (deck.cards.isEmpty()) return
        timerJob?.cancel()
        shuffledCards = deck.cards.shuffled().toMutableList()
        _uiState.update {
            it.copy(
                selectedDeck = deck,
                phase = CharadesPhase.COUNTDOWN,
                countdownValue = 3,
                secondsLeft = it.roundSeconds,
                roundResults = emptyList(),
                flash = FlashFeedback.NONE,
            )
        }
        startCountdownAndTimer()
    }

    private fun startCountdownAndTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (c in 3 downTo 1) {
                _uiState.update { it.copy(countdownValue = c) }
                sound.playTick(urgent = false)
                delay(1000L)
            }
            val first = nextCardInternal()
            _uiState.update {
                it.copy(
                    phase = CharadesPhase.PLAYING,
                    currentCard = first,
                    secondsLeft = it.roundSeconds,
                )
            }
            sound.playCorrect()

            var remaining = _uiState.value.roundSeconds
            while (remaining > 0) {
                delay(1000L)
                remaining -= 1
                _uiState.update { it.copy(secondsLeft = remaining) }
                if (remaining in 1..5) {
                    sound.playTick(urgent = true)
                }
            }
            finishRound()
        }
    }

    private fun nextCardInternal(): CharadesCard {
        if (shuffledCards.isEmpty()) {
            val deck = _uiState.value.selectedDeck
            shuffledCards = (deck?.cards ?: DefaultDecks.all.first().cards).shuffled().toMutableList()
        }
        return shuffledCards.removeAt(0)
    }

    fun recordAnswer(correct: Boolean) {
        val state = _uiState.value
        if (state.phase != CharadesPhase.PLAYING) return
        val word = state.currentCard?.text ?: return

        if (correct) sound.playCorrect() else sound.playPass()
        val next = nextCardInternal()

        _uiState.update {
            it.copy(
                roundResults = it.roundResults + RoundWordResult(word = word, guessed = correct),
                currentCard = next,
                flash = if (correct) FlashFeedback.CORRECT else FlashFeedback.PASS,
            )
        }

        flashJob?.cancel()
        flashJob = viewModelScope.launch {
            delay(420L)
            _uiState.update { it.copy(flash = FlashFeedback.NONE) }
        }
    }

    fun finishRound() {
        timerJob?.cancel()
        val state = _uiState.value
        if (state.phase != CharadesPhase.PLAYING) return

        val score = state.roundScore
        if (score >= 10) sound.playCelebration() else sound.playWahWahRoast()

        _uiState.update {
            if (it.mode == CharadesMode.TEAM_BATTLE) {
                val newTeamA = if (it.currentTeamIndex == 0) it.teamAScore + score else it.teamAScore
                val newTeamB = if (it.currentTeamIndex == 1) it.teamBScore + score else it.teamBScore
                it.copy(
                    phase = CharadesPhase.SUMMARY,
                    teamAScore = newTeamA,
                    teamBScore = newTeamB,
                    completedTurns = it.completedTurns + 1,
                    flash = FlashFeedback.NONE,
                )
            } else {
                it.copy(
                    phase = CharadesPhase.SUMMARY,
                    flash = FlashFeedback.NONE,
                )
            }
        }
    }

    fun nextTeamTurnOrReplay() {
        val state = _uiState.value
        val deck = state.selectedDeck ?: return
        if (state.mode == CharadesMode.TEAM_BATTLE) {
            if (state.isTeamBattleFinished) {
                _uiState.update {
                    it.copy(
                        teamAScore = 0,
                        teamBScore = 0,
                        currentTeamIndex = 0,
                        completedTurns = 0,
                        phase = CharadesPhase.DECK_PICKER,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(currentTeamIndex = if (it.currentTeamIndex == 0) 1 else 0)
                }
                startDeck(deck)
            }
        } else {
            startDeck(deck)
        }
    }

    fun backToDeckPicker() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                phase = CharadesPhase.DECK_PICKER,
                flash = FlashFeedback.NONE,
            )
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        flashJob?.cancel()
        super.onCleared()
    }
}
