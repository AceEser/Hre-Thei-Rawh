package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.MessageType
import com.example.data.model.Player
import com.example.data.model.Reaction
import com.example.data.model.Room
import com.example.data.model.RoomStatus
import com.example.data.model.Stroke
import com.example.data.model.WordChoice
import com.example.data.model.WordDictionary
import com.example.data.repository.GameRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenState {
    HOME,
    LOBBY,
    GAMEPLAY,
    GAME_OVER
}

class GameViewModel(
    application: Application,
    private val repository: GameRepository = GameRepository(application)
) : AndroidViewModel(application) {

    private val auth = Firebase.auth
    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    private val _nickname = MutableStateFlow(
        auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Player_${(1000..9999).random()}"
    )
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    private val _activeScreen = MutableStateFlow(ScreenState.HOME)
    val activeScreen: StateFlow<ScreenState> = _activeScreen.asStateFlow()

    private val _currentRoomId = MutableStateFlow<String?>(null)
    val currentRoomId: StateFlow<String?> = _currentRoomId.asStateFlow()

    private val _currentRoom = MutableStateFlow<Room?>(null)
    val currentRoom: StateFlow<Room?> = _currentRoom.asStateFlow()

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _reactions = MutableStateFlow<List<Reaction>>(emptyList())
    val reactions: StateFlow<List<Reaction>> = _reactions.asStateFlow()

    private val _wordChoices = MutableStateFlow<List<WordChoice>>(emptyList())
    val wordChoices: StateFlow<List<WordChoice>> = _wordChoices.asStateFlow()

    private val _localTimer = MutableStateFlow(60)
    val localTimer: StateFlow<Int> = _localTimer.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var roomJob: Job? = null
    private var playersJob: Job? = null
    private var strokesJob: Job? = null
    private var messagesJob: Job? = null
    private var reactionsJob: Job? = null
    private var timerJob: Job? = null

    fun setNickname(name: String) {
        if (name.isNotBlank()) {
            _nickname.value = name.trim()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun createRoom(matchDurationMinutes: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.createRoom(_nickname.value, matchDurationMinutes)
            _isLoading.value = false
            result.onSuccess { roomId ->
                _currentRoomId.value = roomId
                observeRoomData(roomId)
                _activeScreen.value = ScreenState.LOBBY
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Failed to create room"
            }
        }
    }

    fun joinRoom(roomCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.joinRoomByCode(roomCode, _nickname.value)
            _isLoading.value = false
            result.onSuccess { (room, _) ->
                _currentRoomId.value = room.roomId
                observeRoomData(room.roomId)
                _activeScreen.value = if (room.status == RoomStatus.LOBBY.name) ScreenState.LOBBY else ScreenState.GAMEPLAY
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Failed to join room"
            }
        }
    }

    private fun observeRoomData(roomId: String) {
        roomJob?.cancel()
        playersJob?.cancel()
        strokesJob?.cancel()
        messagesJob?.cancel()
        reactionsJob?.cancel()

        roomJob = viewModelScope.launch {
            repository.observeRoom(roomId).collect { room ->
                _currentRoom.value = room
                if (room != null) {
                    when (room.status) {
                        RoomStatus.LOBBY.name -> {
                            if (_activeScreen.value != ScreenState.LOBBY) {
                                _activeScreen.value = ScreenState.LOBBY
                            }
                        }
                        RoomStatus.GAME_OVER.name -> {
                            _activeScreen.value = ScreenState.GAME_OVER
                        }
                        else -> {
                            if (_activeScreen.value != ScreenState.GAMEPLAY) {
                                _activeScreen.value = ScreenState.GAMEPLAY
                            }
                        }
                    }

                    // Host runs turn timers
                    if (room.hostId == currentUserId && room.status == RoomStatus.DRAWING.name) {
                        startHostTimerLoop(room)
                    }
                }
            }
        }

        playersJob = viewModelScope.launch {
            repository.observePlayers(roomId).collect { list ->
                _players.value = list
            }
        }

        strokesJob = viewModelScope.launch {
            repository.observeStrokes(roomId).collect { list ->
                _strokes.value = list
            }
        }

        messagesJob = viewModelScope.launch {
            repository.observeMessages(roomId).collect { list ->
                _messages.value = list
            }
        }

        reactionsJob = viewModelScope.launch {
            repository.observeReactions(roomId).collect { list ->
                _reactions.value = list
            }
        }
    }

    fun startGame() {
        val room = _currentRoom.value ?: return
        if (room.hostId != currentUserId) return

        val playerList = _players.value
        if (playerList.isEmpty()) return

        val firstDrawer = playerList.random()

        viewModelScope.launch {
            _wordChoices.value = WordDictionary.getRandomWordChoices()
            repository.updateRoom(
                roomId = room.roomId,
                updates = mapOf(
                    "status" to RoomStatus.SELECTING_WORD.name,
                    "currentDrawerId" to firstDrawer.userId,
                    "currentDrawerNickname" to firstDrawer.nickname,
                    "secretWord" to "",
                    "revealedHint" to "",
                    "currentWordLength" to 0
                )
            )
            repository.sendMessage(
                roomId = room.roomId,
                senderNickname = "System",
                content = "${firstDrawer.nickname} is choosing a word to draw!",
                type = MessageType.SYSTEM
            )
        }
    }

    fun selectWord(word: String) {
        val room = _currentRoom.value ?: return
        val cleanWord = word.trim()

        viewModelScope.launch {
            repository.clearCanvas(room.roomId)

            // Reset player guessed states for the new round
            _players.value.forEach { player ->
                repository.updatePlayer(
                    roomId = room.roomId,
                    playerId = player.userId,
                    updates = mapOf(
                        "hasGuessedThisRound" to false,
                        "isSpectator" to false
                    )
                )
            }

            val maskedInitial = WordDictionary.generateMaskedHint(cleanWord, 0f)

            repository.updateRoom(
                roomId = room.roomId,
                updates = mapOf(
                    "status" to RoomStatus.DRAWING.name,
                    "secretWord" to cleanWord,
                    "currentWordLength" to cleanWord.length,
                    "revealedHint" to maskedInitial,
                    "roundTimeRemaining" to 60,
                    "roundDurationSeconds" to 60
                )
            )

            repository.sendMessage(
                roomId = room.roomId,
                senderNickname = "System",
                content = "${room.currentDrawerNickname} has started drawing! Guess the word (${cleanWord.length} letters)!",
                type = MessageType.SYSTEM
            )
        }
    }

    private fun startHostTimerLoop(room: Room) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = room.roundTimeRemaining
            while (remaining > 0) {
                delay(1000)
                remaining--
                _localTimer.value = remaining

                // Progressive hint reveal at ~65% and ~35% remaining
                val elapsedFrac = (60 - remaining) / 60f
                val updatedHint = WordDictionary.generateMaskedHint(room.secretWord, elapsedFrac)

                repository.updateRoom(
                    roomId = room.roomId,
                    updates = mapOf(
                        "roundTimeRemaining" to remaining,
                        "revealedHint" to updatedHint
                    )
                )

                // Check if all active non-drawers have guessed correctly
                val nonDrawers = _players.value.filter { it.userId != room.currentDrawerId && !it.isSpectator }
                if (nonDrawers.isNotEmpty() && nonDrawers.all { it.hasGuessedThisRound }) {
                    break
                }
            }

            // End round
            endRound(room)
        }
    }

    private suspend fun endRound(room: Room) {
        repository.sendMessage(
            roomId = room.roomId,
            senderNickname = "System",
            content = "Time's up! The secret word was: \"${room.secretWord}\"",
            type = MessageType.SYSTEM
        )

        repository.updateRoom(
            roomId = room.roomId,
            updates = mapOf(
                "status" to RoomStatus.ROUND_END.name,
                "revealedHint" to room.secretWord.uppercase()
            )
        )

        delay(4000)

        // Rotate to next drawer or end game
        val currentPlayers = _players.value
        val currentIndex = currentPlayers.indexOfFirst { it.userId == room.currentDrawerId }
        val nextIndex = (currentIndex + 1) % currentPlayers.size.coerceAtLeast(1)
        val nextDrawer = currentPlayers.getOrNull(nextIndex) ?: currentPlayers.firstOrNull()

        val nextRound = room.currentRound + 1
        if (nextRound > room.totalRounds * currentPlayers.size) {
            // Match completed!
            repository.updateRoom(
                roomId = room.roomId,
                updates = mapOf("status" to RoomStatus.GAME_OVER.name)
            )
        } else if (nextDrawer != null) {
            _wordChoices.value = WordDictionary.getRandomWordChoices()
            repository.updateRoom(
                roomId = room.roomId,
                updates = mapOf(
                    "status" to RoomStatus.SELECTING_WORD.name,
                    "currentDrawerId" to nextDrawer.userId,
                    "currentDrawerNickname" to nextDrawer.nickname,
                    "currentRound" to nextRound,
                    "secretWord" to "",
                    "revealedHint" to ""
                )
            )
        }
    }

    fun submitGuess(guess: String) {
        val room = _currentRoom.value ?: return
        val currentUid = currentUserId
        if (room.currentDrawerId == currentUid) return // Drawer cannot guess

        val me = _players.value.find { it.userId == currentUid } ?: return
        if (me.hasGuessedThisRound) return // Duplicate scoring prevention

        val cleanGuess = guess.trim()
        val isCorrect = cleanGuess.equals(room.secretWord.trim(), ignoreCase = true)

        viewModelScope.launch {
            if (isCorrect) {
                // Award points: faster guesses earn more points (up to 350 pts)
                val remaining = room.roundTimeRemaining.coerceAtLeast(1)
                val earnedPoints = 100 + (remaining * 4)

                // Update guesser score
                repository.updatePlayer(
                    roomId = room.roomId,
                    playerId = currentUid,
                    updates = mapOf(
                        "score" to (me.score + earnedPoints),
                        "hasGuessedThisRound" to true
                    )
                )

                // Award drawer points for successful explanation
                val drawer = _players.value.find { it.userId == room.currentDrawerId }
                if (drawer != null) {
                    repository.updatePlayer(
                        roomId = room.roomId,
                        playerId = drawer.userId,
                        updates = mapOf(
                            "score" to (drawer.score + 50)
                        )
                    )
                }

                // Announce correct guess
                repository.sendMessage(
                    roomId = room.roomId,
                    senderNickname = me.nickname,
                    content = "guessed the word!",
                    type = MessageType.CORRECT_GUESS
                )
            } else {
                // Broadcast regular chat guess
                repository.sendMessage(
                    roomId = room.roomId,
                    senderNickname = me.nickname,
                    content = cleanGuess,
                    type = MessageType.CHAT
                )
            }
        }
    }

    fun sendSticker(sticker: String) {
        val room = _currentRoom.value ?: return
        val me = _players.value.find { it.userId == currentUserId }
        val senderName = me?.nickname ?: _nickname.value

        viewModelScope.launch {
            repository.sendMessage(
                roomId = room.roomId,
                senderNickname = senderName,
                content = sticker,
                type = MessageType.STICKER
            )
            repository.sendReaction(room.roomId, senderName, sticker)
        }
    }

    fun sendReaction(emoji: String) {
        val room = _currentRoom.value ?: return
        val me = _players.value.find { it.userId == currentUserId }
        val senderName = me?.nickname ?: _nickname.value

        viewModelScope.launch {
            repository.sendReaction(room.roomId, senderName, emoji)
        }
    }

    fun sendStroke(stroke: Stroke) {
        val room = _currentRoom.value ?: return
        if (room.currentDrawerId != currentUserId) return

        viewModelScope.launch {
            repository.sendStroke(room.roomId, stroke)
        }
    }

    fun undoStroke() {
        val room = _currentRoom.value ?: return
        if (room.currentDrawerId != currentUserId) return

        viewModelScope.launch {
            repository.undoLastStroke(room.roomId, currentUserId)
        }
    }

    fun clearCanvas() {
        val room = _currentRoom.value ?: return
        if (room.currentDrawerId != currentUserId) return

        viewModelScope.launch {
            repository.clearCanvas(room.roomId)
        }
    }

    fun kickPlayer(playerId: String) {
        val room = _currentRoom.value ?: return
        if (room.hostId != currentUserId) return

        viewModelScope.launch {
            repository.kickPlayer(room.roomId, playerId)
        }
    }

    fun leaveRoom() {
        val room = _currentRoom.value ?: return
        viewModelScope.launch {
            repository.leaveRoom(room.roomId, currentUserId)
            resetLocalSession()
        }
    }

    fun playAgain() {
        val room = _currentRoom.value ?: return
        if (room.hostId != currentUserId) return

        viewModelScope.launch {
            repository.updateRoom(
                roomId = room.roomId,
                updates = mapOf(
                    "status" to RoomStatus.LOBBY.name,
                    "currentRound" to 1,
                    "secretWord" to "",
                    "revealedHint" to "",
                    "currentDrawerId" to "",
                    "currentDrawerNickname" to ""
                )
            )
            _players.value.forEach { player ->
                repository.updatePlayer(
                    roomId = room.roomId,
                    playerId = player.userId,
                    updates = mapOf(
                        "score" to 0,
                        "hasGuessedThisRound" to false,
                        "isSpectator" to false
                    )
                )
            }
            _activeScreen.value = ScreenState.LOBBY
        }
    }

    fun returnHome() {
        resetLocalSession()
    }

    private fun resetLocalSession() {
        roomJob?.cancel()
        playersJob?.cancel()
        strokesJob?.cancel()
        messagesJob?.cancel()
        reactionsJob?.cancel()
        timerJob?.cancel()

        _currentRoomId.value = null
        _currentRoom.value = null
        _players.value = emptyList()
        _strokes.value = emptyList()
        _messages.value = emptyList()
        _reactions.value = emptyList()
        _activeScreen.value = ScreenState.HOME
    }
}
