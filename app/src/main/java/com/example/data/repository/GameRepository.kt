package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.MessageType
import com.example.data.model.Player
import com.example.data.model.Reaction
import com.example.data.model.Room
import com.example.data.model.RoomStatus
import com.example.data.model.Stroke
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GameRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    suspend fun createRoom(hostNickname: String, matchDurationMinutes: Int): Result<String> {
        return try {
            val uid = requireUserId()
            val roomId = UUID.randomUUID().toString().replace("-", "").take(16)
            val roomCode = generateRoomCode()
            val roomRef = db.collection("rooms").document(roomId)

            val roomData = hashMapOf<String, Any>(
                "roomId" to roomId,
                "roomCode" to roomCode,
                "hostId" to uid,
                "status" to RoomStatus.LOBBY.name,
                "matchDurationMinutes" to matchDurationMinutes,
                "currentRound" to 1,
                "totalRounds" to 3,
                "currentDrawerId" to "",
                "currentDrawerNickname" to "",
                "currentWordLength" to 0,
                "revealedHint" to "",
                "secretWord" to "",
                "roundTimeRemaining" to 60,
                "roundDurationSeconds" to 60,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val hostPlayerData = hashMapOf<String, Any>(
                "userId" to uid,
                "nickname" to hostNickname.ifBlank { "Host" },
                "avatarColor" to "#00E5FF",
                "score" to 0,
                "isHost" to true,
                "isSpectator" to false,
                "hasGuessedThisRound" to false,
                "joinedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.runBatch { batch ->
                batch.set(roomRef, roomData)
                batch.set(roomRef.collection("players").document(uid), hostPlayerData)
            }.await()

            Result.success(roomId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "rooms")
            Result.failure(e)
        }
    }

    suspend fun joinRoomByCode(roomCode: String, nickname: String): Result<Pair<Room, Player>> {
        return try {
            val uid = requireUserId()
            val cleanCode = roomCode.trim().uppercase()
            val snapshot = db.collection("rooms")
                .whereEqualTo("roomCode", cleanCode)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return Result.failure(IllegalArgumentException("No room found with code $cleanCode"))
            }

            val roomDoc = snapshot.documents.first()
            val room = roomDoc.toObject(Room::class.java)?.copy(id = roomDoc.id)
                ?: return Result.failure(IllegalStateException("Failed to parse room"))

            // Spec Rule: Late joiners enter as spectators until next round. Rejoin resets score to ZERO.
            val isMidGame = room.status == RoomStatus.DRAWING.name || room.status == RoomStatus.SELECTING_WORD.name
            val playerRef = roomDoc.reference.collection("players").document(uid)

            val avatarColors = listOf("#00E5FF", "#FF3366", "#FFD600", "#00E676", "#B388FF", "#FF9100")
            val chosenColor = avatarColors.random()

            val playerData = hashMapOf<String, Any>(
                "userId" to uid,
                "nickname" to nickname.ifBlank { "Player_${uid.takeLast(4)}" },
                "avatarColor" to chosenColor,
                "score" to 0, // Reset to zero per specification
                "isHost" to (room.hostId == uid),
                "isSpectator" to isMidGame,
                "hasGuessedThisRound" to false,
                "joinedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            playerRef.set(playerData).await()

            // Broadcast system notification
            sendMessage(
                roomId = room.id,
                senderNickname = "System",
                content = "${nickname.ifBlank { "A player" }} joined the game!",
                type = MessageType.SYSTEM
            )

            val player = Player(
                id = uid,
                userId = uid,
                nickname = nickname.ifBlank { "Player_${uid.takeLast(4)}" },
                avatarColor = chosenColor,
                score = 0,
                isHost = (room.hostId == uid),
                isSpectator = isMidGame,
                hasGuessedThisRound = false
            )

            Result.success(room to player)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "rooms/join")
            Result.failure(e)
        }
    }

    fun observeRoom(roomId: String): Flow<Room?> {
        return db.collection("rooms").document(roomId)
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(Room::class.java)?.copy(id = snap.id) else null
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, "rooms/$roomId")
                emit(null)
            }
    }

    fun observePlayers(roomId: String): Flow<List<Player>> {
        return db.collection("rooms").document(roomId)
            .collection("players")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(Player::class.java)?.copy(id = doc.id)
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "rooms/$roomId/players")
                emit(emptyList())
            }
    }

    fun observeStrokes(roomId: String): Flow<List<Stroke>> {
        return db.collection("rooms").document(roomId)
            .collection("strokes")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(Stroke::class.java)?.copy(id = doc.id)
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "rooms/$roomId/strokes")
                emit(emptyList())
            }
    }

    fun observeMessages(roomId: String): Flow<List<ChatMessage>> {
        return db.collection("rooms").document(roomId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(100)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "rooms/$roomId/messages")
                emit(emptyList())
            }
    }

    fun observeReactions(roomId: String): Flow<List<Reaction>> {
        return db.collection("rooms").document(roomId)
            .collection("reactions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(Reaction::class.java)?.copy(id = doc.id)
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, "rooms/$roomId/reactions")
                emit(emptyList())
            }
    }

    suspend fun sendStroke(roomId: String, stroke: Stroke): Result<Unit> {
        return try {
            val strokeId = stroke.strokeId.ifBlank { UUID.randomUUID().toString() }
            val strokeRef = db.collection("rooms").document(roomId)
                .collection("strokes").document(strokeId)

            val payload = hashMapOf<String, Any>(
                "strokeId" to strokeId,
                "drawerId" to stroke.drawerId,
                "color" to stroke.color,
                "brushWidth" to stroke.brushWidth,
                "isEraser" to stroke.isEraser,
                "pointsJson" to stroke.pointsJson,
                "createdAt" to FieldValue.serverTimestamp()
            )
            strokeRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "rooms/$roomId/strokes")
            Result.failure(e)
        }
    }

    suspend fun clearCanvas(roomId: String): Result<Unit> {
        return try {
            val strokesCollection = db.collection("rooms").document(roomId).collection("strokes")
            val snapshot = strokesCollection.get().await()
            val batch = db.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "rooms/$roomId/strokes")
            Result.failure(e)
        }
    }

    suspend fun undoLastStroke(roomId: String, drawerId: String): Result<Unit> {
        return try {
            val snapshot = db.collection("rooms").document(roomId)
                .collection("strokes")
                .whereEqualTo("drawerId", drawerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                snapshot.documents.first().reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "rooms/$roomId/strokes/undo")
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        roomId: String,
        senderNickname: String,
        content: String,
        type: MessageType
    ): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: "sys_user"
            val messageId = UUID.randomUUID().toString()
            val messageRef = db.collection("rooms").document(roomId)
                .collection("messages").document(messageId)

            val payload = hashMapOf<String, Any>(
                "messageId" to messageId,
                "senderId" to uid,
                "senderNickname" to senderNickname,
                "content" to content,
                "type" to type.name,
                "createdAt" to FieldValue.serverTimestamp()
            )
            messageRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "rooms/$roomId/messages")
            Result.failure(e)
        }
    }

    suspend fun sendReaction(roomId: String, senderNickname: String, emoji: String): Result<Unit> {
        return try {
            val reactionId = UUID.randomUUID().toString()
            val reactionRef = db.collection("rooms").document(roomId)
                .collection("reactions").document(reactionId)

            val payload = hashMapOf<String, Any>(
                "reactionId" to reactionId,
                "senderNickname" to senderNickname,
                "emoji" to emoji,
                "createdAt" to FieldValue.serverTimestamp()
            )
            reactionRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "rooms/$roomId/reactions")
            Result.failure(e)
        }
    }

    suspend fun updateRoom(roomId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            val roomRef = db.collection("rooms").document(roomId)
            val filteredUpdates = updates.filterValues { it != null } + mapOf(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            roomRef.update(filteredUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "rooms/$roomId")
            Result.failure(e)
        }
    }

    suspend fun updatePlayer(roomId: String, playerId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            val playerRef = db.collection("rooms").document(roomId).collection("players").document(playerId)
            val filteredUpdates = updates.filterValues { it != null } + mapOf(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            playerRef.update(filteredUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "rooms/$roomId/players/$playerId")
            Result.failure(e)
        }
    }

    suspend fun leaveRoom(roomId: String, playerId: String): Result<Unit> {
        return try {
            val roomRef = db.collection("rooms").document(roomId)
            val playerRef = roomRef.collection("players").document(playerId)
            playerRef.delete().await()

            // If host left, check if any remaining players can take over host
            val remainingPlayers = roomRef.collection("players").limit(1).get().await()
            if (remainingPlayers.isEmpty) {
                // Last player left; optionally mark finished
                roomRef.update("status", RoomStatus.GAME_OVER.name).await()
            } else {
                val newHost = remainingPlayers.documents.first()
                roomRef.update(
                    mapOf(
                        "hostId" to newHost.id,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                newHost.reference.update(
                    mapOf(
                        "isHost" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "rooms/$roomId/players/$playerId")
            Result.failure(e)
        }
    }

    suspend fun kickPlayer(roomId: String, playerId: String): Result<Unit> {
        return try {
            val playerRef = db.collection("rooms").document(roomId).collection("players").document(playerId)
            playerRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "rooms/$roomId/players/$playerId/kick")
            Result.failure(e)
        }
    }
}
