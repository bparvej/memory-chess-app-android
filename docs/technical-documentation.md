# Memory Chess - Technical Documentation

This document explains the Android app architecture, state flow, and the most important gameplay systems so another AI agent or developer can understand the project quickly.

## 1. Project overview

Memory Chess is a mobile board game built with Android Java and AndroidX. The game uses a circular peg board, a dice-like target color, and a memory-based match mechanic.

The app supports multiple modes:

- Solo Time Attack
- Pass & Play (2 players)
- Pass & Play (3 players)
- Vs AI Bot

Key implementation files:

- [app/src/main/java/com/bparvej/memorychess/ui/GameActivity.java](../app/src/main/java/com/bparvej/memorychess/ui/GameActivity.java)
- [app/src/main/java/com/bparvej/memorychess/viewmodel/GameViewModel.java](../app/src/main/java/com/bparvej/memorychess/viewmodel/GameViewModel.java)
- [app/src/main/java/com/bparvej/memorychess/logic/BoardManager.java](../app/src/main/java/com/bparvej/memorychess/logic/BoardManager.java)
- [app/src/main/java/com/bparvej/memorychess/logic/BotMemoryEngine.java](../app/src/main/java/com/bparvej/memorychess/logic/BotMemoryEngine.java)
- [app/src/main/java/com/bparvej/memorychess/model/Peg.java](../app/src/main/java/com/bparvej/memorychess/model/Peg.java)

## 2. High-level architecture

The app follows a simple MVVM pattern:

- UI layer: Android Activities and custom View classes
- State layer: GameViewModel with LiveData
- Domain logic: BoardManager and BotMemoryEngine
- Data model: Peg, PegColor, GameMode, BotDifficulty

### Core responsibilities

#### UI

- [app/src/main/java/com/bparvej/memorychess/ui/GameActivity.java](../app/src/main/java/com/bparvej/memorychess/ui/GameActivity.java)
  - binds the screen
  - listens for board interactions
  - observes LiveData from the ViewModel
  - opens mode selection and game-over dialogs
  - calls roll and selection actions

- [app/src/main/java/com/bparvej/memorychess/ui/BoardView.java](../app/src/main/java/com/bparvej/memorychess/ui/BoardView.java)
  - renders the circular board
  - draws pegs in ring positions
  - handles reveal/hide animations
  - shows hints and dice visuals

#### ViewModel

- [app/src/main/java/com/bparvej/memorychess/viewmodel/GameViewModel.java](../app/src/main/java/com/bparvej/memorychess/viewmodel/GameViewModel.java)
  - authoritative game state
  - manages score, turn order, target color, timer, and game-over status
  - handles rule checks and state transitions
  - schedules bot turns
  - manages undo snapshot

#### Logic layer

- [app/src/main/java/com/bparvej/memorychess/logic/BoardManager.java](../app/src/main/java/com/bparvej/memorychess/logic/BoardManager.java)
  - creates the shuffled board
  - stores all Peg objects
  - exposes unrevealed pegs and empty-board checks
  - chooses a random hidden peg color as the target color

- [app/src/main/java/com/bparvej/memorychess/logic/BotMemoryEngine.java](../app/src/main/java/com/bparvej/memorychess/logic/BotMemoryEngine.java)
  - stores remembered peg colors
  - marks revealed pegs as remembered based on retention probability
  - chooses a peg for the bot based on memory or randomness

## 3. Model layer

### Peg

[app/src/main/java/com/bparvej/memorychess/model/Peg.java](../app/src/main/java/com/bparvej/memorychess/model/Peg.java)

Each peg has:

- unique id
- color from PegColor
- position radius ratio and angle
- state: HIDDEN, REVEALED, REMOVED
- animation progress and highlight state

Important state helpers:

- isHidden()
- isRevealed()
- isRemoved()

### PegColor

[app/src/main/java/com/bparvej/memorychess/model/PegColor.java](../app/src/main/java/com/bparvej/memorychess/model/PegColor.java)

The app defines six colors:

- Red
- Blue
- Green
- Yellow
- White
- Black

The board is populated with four copies of each color, giving 24 total pegs in the board.

### GameMode

[app/src/main/java/com/bparvej/memorychess/model/GameMode.java](../app/src/main/java/com/bparvej/memorychess/model/GameMode.java)

Supported game modes are:

- SOLO_TIME_ATTACK
- PASS_AND_PLAY_2P
- PASS_AND_PLAY_3P
- VS_BOT

## 4. Board generation

The board is generated in [BoardManager.java](../app/src/main/java/com/bparvej/memorychess/logic/BoardManager.java).

Rules:

- six colors
- four copies of each color
- total board size = 24 pegs
- outer ring = 16 pegs
- inner ring = 8 pegs
- pegs are shuffled randomly before placement

Board creation flow:

1. Create a color list with each color repeated four times.
2. Shuffle it with a Random instance.
3. Place the outer ring pegs using a fixed angle step.
4. Place the inner ring pegs with an offset angle to make the layout feel circular and asymmetric.
5. Store each Peg instance in the board list.

This means the board is not a standard chessboard; it is a memory board with a radial layout.

## 5. Runtime state model

The most important state is managed by GameViewModel and exposed through LiveData:

- scorePlayer1, scorePlayer2, scorePlayer3
- activePlayer
- currentTargetColor
- isDiceRolled
- isGameOver
- gameOverTitle / gameOverMessage
- timeRemaining
- bot thinking flag
- pegsLiveData

The GameActivity observes these values and updates the UI accordingly.

Key idea:

The ViewModel is the real source of truth. The board view only renders state; it does not decide rules.

## 6. Full game flow

### A. Game start

When the user chooses a mode:

- GameActivity shows a mode dialog
- GameViewModel.startGame(mode, difficulty) is triggered
- BoardManager.initBoard() resets the board
- score, turn, target color, and timer are reset
- solo timer starts for time-attack mode

### B. Dice roll

The player rolls the dice by calling GameViewModel.rollDice().

This logic:

- blocks if the game is over
- blocks if a dice result was already assigned
- calls BoardManager.rollDice()
- picks one random peg from the unrevealed set
- sets the currentTargetColor to that peg's color
- marks isDiceRolled = true

Important behavior:

The target color is not chosen randomly from all colors in the enum. It is chosen from the color of a random unrevealed peg still on the board. This keeps the target aligned with the current board state.

### C. Peg selection

When the user taps a peg:

- GameActivity calls processPegSelection(peg, isMatch)
- GameViewModel saves a MoveSnapshot before mutating state
- BotMemoryEngine.onPegRevealed(peg) records the revealed peg in memory
- rule result is checked:
  - match => handleMatchSuccess()
  - miss => handleMatchMiss()

### D. Match success

If the selected peg color matches the target color:

- current player gets one point in most modes
- in solo mode, player gets +10 and +3 seconds bonus
- peg state becomes REMOVED
- isDiceRolled resets to false
- board refreshes
- if board is now empty, win condition is checked
- in vs-bot mode, the bot may schedule a delayed turn when bot is active

### E. Match miss

If the selected peg does not match:

- solo mode subtracts 5 seconds
- pass-and-play 3P advances to next player
- standard 2-player mode switches to the other player
- dice resets to false
- if bot turn is active, bot action is scheduled

### F. Win conditions

The end-of-game check is in checkWinCondition().

Solo mode:

- board cleared => victory message
- timer ends => TIME'S UP

Vs bot:

- compare Player 1 score vs bot score
- tie is allowed

Pass & Play 2P:

- highest score wins

Pass & Play 3P:

- highest score wins among P1, P2, P3

## 7. Undo system

Undo is implemented by MoveSnapshot in GameViewModel.

Before each selection, GameViewModel.saveSnapshot() stores:

- current score values
- active player
- remaining time
- target color
- dice flag
- peg state before action
- peg reference

When undo is triggered:

- restore scores and turn state
- restore timer for solo mode
- restore peg state
- clear snapshot
- show toast message

This makes undo a clean state rollback rather than a custom animation stack.

## 8. Bot logic

The bot is driven by BotMemoryEngine.

### Memory behavior

When a peg is revealed, BotMemoryEngine.onPegRevealed() may store its color in a map keyed by peg ID:

- retention chance depends on BotDifficulty
- easier difficulty => higher chance of forgetting or lower retention probability
- difficult mode => more reliable memory retention

If the peg is later removed, its memory entry is cleared.

### Decision logic

In selectPeg(targetColor, unrevealedPegs):

1. Search visible/relevant unrevealed pegs that are already remembered.
2. If a remembered peg matches the current target color, choose it.
3. Otherwise, choose a random unrevealed peg that has not been remembered yet.
4. If none are available, fall back to a random unrevealed peg.

This gives the bot a memory-based strategy that is not perfect but feels intentional.

## 9. Difficulty model

BotDifficulty is defined in the model package. It likely governs memory retention or decision confidence. The actual retention probability is read via difficulty.getRetentionProbability() inside BotMemoryEngine.

The difficulty should be treated as the bot's memory quality setting rather than a full decision engine.

## 10. Dependency and build setup

Project root:

- [build.gradle](../build.gradle)
- [settings.gradle](../settings.gradle)
- [app/build.gradle](../app/build.gradle)

The app uses:

- Android Gradle Plugin 8.3.2
- compileSdk 36
- minSdk 24
- AppCompat, Material, ConstraintLayout
- Lifecycle ViewModel / LiveData
- Google Mobile Ads SDK

Build commands:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## 11. Recommended AI reading order

If another AI needs to understand the project quickly, read these in order:

1. [GameViewModel.java](../app/src/main/java/com/bparvej/memorychess/viewmodel/GameViewModel.java)
2. [BoardManager.java](../app/src/main/java/com/bparvej/memorychess/logic/BoardManager.java)
3. [BotMemoryEngine.java](../app/src/main/java/com/bparvej/memorychess/logic/BotMemoryEngine.java)
4. [GameActivity.java](../app/src/main/java/com/bparvej/memorychess/ui/GameActivity.java)
5. [BoardView.java](../app/src/main/java/com/bparvej/memorychess/ui/BoardView.java)
6. [Peg.java](../app/src/main/java/com/bparvej/memorychess/model/Peg.java)

## 12. Practical notes for future changes

- Treat GameViewModel as the state machine.
- Do not move rule logic into the View layer.
- Use BoardManager for board generation and data queries.
- Use BotMemoryEngine for bot decisions and memory behavior.
- Keep all game-state mutations inside ViewModel so UI and bot actions stay synchronized.

## 13. Summary

This project is a compact, rule-driven Android game with:

- a circular board
- color-matching memory gameplay
- turn-based rules
- solo timer mode
- multiplayer rotation
- AI that remembers prior peg colors

The code is small and readable, but the critical logic is concentrated in GameViewModel and the logic package. That is the correct entry point for understanding game behavior.
