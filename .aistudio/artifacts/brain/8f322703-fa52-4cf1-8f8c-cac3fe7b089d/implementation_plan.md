# Hre Thei Rawh – Multiplayer Drawing & Guessing Game

Hre Thei Rawh is a vibrant, real-time multiplayer drawing and guessing Android application built with Jetpack Compose and Firebase Firestore, bringing the social excitement of Skribbl-style party games with room codes, live canvas synchronization, progressive hint guessing, floating sticker reactions, and animated podium celebrations.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and design choices were clarified and confirmed in Phase 1:
>
> - **Game Session Hosting**: Real-time online multiplayer rooms synchronized via Firebase Firestore with real-time snapshot streams for strokes, chat, scores, and turns.
> - **Word Library**: Built-in tiered English dictionary (Easy, Medium, Hard) plus full support for custom secret word entry and validation.
> - **Visual Direction**: Vibrant arcade party aesthetic with energetic colors, playful doodle accents, floating emoji reactions, and fluid animations.
> - **Authentication Flow**: Google Sign-In via Jetpack Credential Manager as required by the backend security model, keeping player identity secure while allowing custom room nicknames.

---

## 1. Overview & Core Concept

- **What It Does**: Hre Thei Rawh allows players to create private or public game rooms, invite friends via 6-character room codes, take turns drawing words on a synchronized canvas, guess answers in real-time chat, send animated floating reactions and stickers, and climb the podium leaderboard.
- **Target Audience / Persona**: Groups of friends, families, students, and casual party gamers playing on mobile phones and tablets.
- **Key Value**: Zero-friction party gaming on Android—players can quickly spin up a room, customize match length (5 to 30 minutes), draw with intuitive finger tools, and enjoy low-latency guess validation with progressive letter hints.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Home & Identity**:
   - Player launches the app, authenticates smoothly via Google Sign-In, and enters their in-game display Nickname and avatar style.
   - Choose to **Create Room** (configuring match duration: 5, 10, 15, 20, or 30 mins) or **Join Room** (entering a 6-character code).
2. **Lobby & Match Configuration**:
   - Host sees connected players in real-time cards, selected game duration, and a "Start Game" action.
   - Non-hosts see lobby participant updates and waiting indicators.
   - Host can kick disruptive players or transfer host status if leaving.
3. **Word Selection (Drawing Player)**:
   - Selected drawer receives 3 randomized word choices spanning Easy, Medium, and Hard, plus a "Use My Own Word" option with validation.
   - Non-drawers see a suspenseful "Drawer is choosing a word..." banner.
4. **Active Round – Drawing & Guessing**:
   - **Drawer View**: Full drawing tools (brush size, vibrant color palette, eraser, undo, clear), timer countdown, secret word displayed clearly, and live guess chat.
   - **Guesser View**: Synchronized canvas rendering, masked word (`_ _ _ _`), progressive letter hints at timed intervals, text chat input, and reaction toolbar.
   - **Real-Time Guessing**: Typing the exact word triggers instant positive sound/vaptic cues, awards score based on elapsed time, awards drawer bonus points, and hides the guess from other players to preserve fair play.
   - **Floating Reactions**: Tapping emoji stickers (🔥, 😂, 👏, 🤔, ❤️, 🎉) launches floating physics-based particle animations across all players' screens.
5. **Round Summary & Next Turn**:
   - Reveals the answer, displays round points gained, highlights fastest guesser, and transitions to the next drawer.
6. **Game Over & Podium Celebration**:
   - Displays a 3D-styled 1st, 2nd, and 3rd place podium with gold/silver/bronze trophies, animated confetti particles, full rankings, and options to Play Again or Return Home.

### Visual Identity & Theme

- **Aesthetic Direction**: Vibrant Arcade Party—playful, energetic, friendly, and modern.
- **Color Palette**:
  - Dominant Background: Deep Indigo / Midnight Violet (`#120D26` / `#1E153D`) providing rich contrast for vivid drawings.
  - Primary / Accent: Electric Cyan (`#00E5FF`) and Neon Coral (`#FF3366`) for primary interactive buttons and round timers.
  - Secondary Accents: Sunshine Yellow (`#FFD600`), Vibrant Mint (`#00E676`), and Lilac (`#B388FF`).
  - Drawing Palette: 16 curated high-contrast arcade colors for expressive sketching.
- **Typography & Hierarchy**:
  - Display / Headings: Rounded, punchy typography with bold weights for countdown timers, podium ranks, and logo branding.
  - Body / Chat: Crisp, readable sans-serif with distinct message bubbles (system announcements, guesses, and sticker notifications).
- **Interactive Feedback & Motion**:
  - Spring-animated reaction emojis floating up the canvas.
  - Confetti particle bursts on correct guesses and podium screen.
  - Pulsing amber alert when the round timer dips below 10 seconds.
  - Smooth Bezier curve interpolation on canvas strokes for butter-smooth drawing.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Real-Time Sync Strategy via Firestore Document Streams**
  - *Chosen Approach*: Maintain a active room document for match phase, timer state, and current word hint, with subcollections for `strokes` (batched point paths) and `messages` (guesses and sticker events).
  - *Why*: Provides synchronized multiplayer across different physical devices without requiring a custom dedicated WebSocket server, maintaining full persistence, reconnect support, and clean state recovery.
  - *Alternatives Considered*: Direct WebSockets would require deploying and hosting a separate server process; Firestore provides zero-maintenance, built-in real-time listener flows on Android.

- **Decision 2: Secret Word Fair Play & Security Rules**
  - *Chosen Approach*: The full secret word is only visible to the drawing player; guessers receive the masked string (`_ a _ _ e`) and progressive letter reveals calculated by time milestones. Guess validation checks against the secret word and awards points without broadcasting the solution to remaining guessers.
  - *Why*: Prevents cheat tools or network snooping from spoiling the round.

- **Decision 3: Rejoin & Spectator Handling**
  - *Chosen Approach*: When a player leaves, their presence is updated. If they rejoin mid-round, they join as a spectator with score reset to zero (per specification) and participate actively in the subsequent round.
  - *Why*: Ensures game balance and adheres strictly to the client specification.

---

## 4. Technical Architecture & Data Strategy

### System Architecture & State Flow

```
┌────────────────────────────────────────────────────────┐
│                      Hre Thei Rawh                     │
│                  Android Jetpack Compose               │
└──────────────────────────┬─────────────────────────────┘
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
   ┌───────────────────┐       ┌───────────────────┐
   │ CredentialManager │       │   GameViewModel   │
   │  Google Sign-In   │       │ (State Management)│
   └─────────┬─────────┘       └─────────┬─────────┘
             │                           │
             ▼                           ▼
   ┌───────────────────────────────────────────────┐
   │             Firebase Firestore                │
   │       (Rooms, Players, Strokes, Chat)         │
   └───────────────────────────────────────────────┘
```

### Component Hierarchy

```
MainActivity
 └── HreTheiRawhApp
      ├── AuthScreen (Google Sign-In Gate)
      └── MainGameFlow (Scaffold & Edge-to-Edge)
           ├── HomeScreen (Nickname, Create/Join Room, Mode Select)
           ├── LobbyScreen (Player Grid, Host Controls, Duration Picker)
           ├── WordSelectionDialog (Drawer: 3 Words + Custom Word Modal)
           ├── GamePlayScreen (Active Round)
           │    ├── GameTopBar (Timer, Round Counter, Masked Word Hint)
           │    ├── DrawingCanvas (Drawing or Viewing with Bezier smoothing)
           │    ├── DrawingToolbar (Brush Size, Color Palette, Undo, Clear)
           │    ├── FloatingReactionsOverlay (Particle Emoji Animations)
           │    ├── PlayersScoreRail (Avatars, Current Scores, Guessed Status)
           │    └── GuessChatSection (Message Stream, Sticker Selector, Guess Input)
           ├── RoundEndDialog (Answer Reveal, Point Breakdown, Next Drawer)
           └── GameOverScreen (Winner Podium, Confetti, Leaderboard, Play Again)
```

### Firestore Schema Blueprint

- **`/rooms/{roomId}`**:
  - `roomCode`: String (e.g., "HR7X29")
  - `hostId`: String
  - `status`: String (`"LOBBY"`, `"SELECTING_WORD"`, `"DRAWING"`, `"ROUND_END"`, `"GAME_OVER"`)
  - `matchDurationMinutes`: Int (5, 10, 15, 20, 30)
  - `matchStartTime`: Timestamp?
  - `currentRound`: Int
  - `totalRounds`: Int
  - `currentDrawerId`: String
  - `currentWordLength`: Int
  - `revealedHint`: String (e.g. `_ O _ K _ T`)
  - `roundTimeRemaining`: Int
  - `roundEndTime`: Timestamp?
- **`/rooms/{roomId}/players/{userId}`**:
  - `userId`: String
  - `nickname`: String
  - `avatarColor`: String
  - `score`: Int
  - `isHost`: Boolean
  - `hasGuessedThisRound`: Boolean
  - `isSpectator`: Boolean
  - `joinedAt`: Timestamp
- **`/rooms/{roomId}/strokes/{strokeId}`**:
  - `strokeId`: String
  - `drawerId`: String
  - `points`: List<Map<String, Float>> (normalized coordinates `x, y` for multi-screen scale independence)
  - `color`: Long
  - `brushWidth`: Float
  - `isEraser`: Boolean
  - `timestamp`: Timestamp
- **`/rooms/{roomId}/messages/{messageId}`**:
  - `senderId`: String
  - `senderNickname`: String
  - `content`: String
  - `type`: String (`"CHAT"`, `"CORRECT_GUESS"`, `"STICKER"`, `"SYSTEM"`)
  - `timestamp`: Timestamp
- **`/rooms/{roomId}/reactions/{reactionId}`**:
  - `senderNickname`: String
  - `emoji`: String
  - `timestamp`: Timestamp

---

## 5. Phased Implementation Roadmap

1. **Phase 1: Project Metadata & Firebase Provisioning**:
   - Set app name "Hre Thei Rawh" and description in `metadata.json` and `strings.xml`.
   - Provision Firebase Firestore backend using `set_up_firebase`.
   - Generate `firebase-blueprint.json` and zero-trust `firestore.rules`.
   - Validate rules locally with tests before deployment.
2. **Phase 2: Authentication & Room Service**:
   - Google Sign-In with Credential Manager.
   - Room creation, 6-character room code generation, join flow, host migration, and spectator state on mid-game join.
3. **Phase 3: Synchronized Drawing Canvas**:
   - High-performance touch canvas with normalized vector paths, stroke smoothing, color/brush picking, undo/clear.
   - Real-time Firestore stroke synchronization for watching players.
4. **Phase 4: Game Loop, Progressive Word Hints & Guessing Chat**:
   - Curated dictionary (Easy/Medium/Hard) + Custom Word validation.
   - Server-side round timer, progressive letter reveal logic, guess matching, duplicate-score prevention, and speed-based point calculation.
5. **Phase 5: Stickers, Floating Reactions & Podium Polish**:
   - Interactive sticker selector and physics-based floating emoji overlay.
   - Winner podium (Gold/Silver/Bronze medals), confetti animations, full leaderboard, and play-again rematch cycle.
