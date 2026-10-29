# Memory Chess Game Logic

This file explains the actual gameplay rules and logic as implemented in the code. It is intended to be easier to follow than the raw Java class files.

## 1. Board layout

The board is a circular arrangement of pegs.

The code creates:

- 16 pegs on the outer ring
- 8 pegs on the inner ring
- total = 24 pegs

The board is created in [BoardManager.java](../app/src/main/java/com/bparvej/memorychess/logic/BoardManager.java).

Each peg has:

- a unique ID
- a color
- a state: HIDDEN, REVEALED, or REMOVED
- a ring radius ratio and angle value for drawing

## 2. Color distribution

The color set is defined in [PegColor.java](../app/src/main/java/com/bparvej/memorychess/model/PegColor.java).

A full board contains:

- 4 red pegs
- 4 blue pegs
- 4 green pegs
- 4 yellow pegs
- 4 white pegs
- 4 black pegs

That creates exactly 24 pegs total.

The board is shuffled before placement, so the player never sees a stable pattern.

## 3. Turn flow

The current turn and score state live inside [GameViewModel.java](../app/src/main/java/com/bparvej/memorychess/viewmodel/GameViewModel.java).

The game state includes:

- activePlayer
- currentTargetColor
- isDiceRolled
- score for each player
- game-over status

### Step 1: roll the dice

The user rolls and the app chooses a target color by selecting a random unrevealed peg and reading its color.

Implementation logic:

- BoardManager.rollDice() scans all hidden pegs
- randomly picks one hidden peg
- returns that peg's color as the target color

This means the selected target always corresponds to a peg that is still on the board.

### Step 2: pick a peg

The player taps a peg on the board.

The app calls:

- checkIsMatch(peg)
- processPegSelection(peg, isMatch)

Then:

- the reveal state is processed
- the peg is checked against the current target color
- the board state is updated

## 4. Match rules

### Match success

A match occurs when:

```java
peg.getColor() == currentTargetColor
```

When a match happens:

- the peg becomes REMOVED
- the active player gains a point
- the dice is reset to false
- the board is refreshed

### Match miss

If the peg does not match:

- the turn passes to the next player (or rotates based on game mode)
- dice state resets
- the board stays active for the next turn

## 5. Score logic by mode

### Solo Time Attack

The system awards:

- +10 points for each successful match
- +3 seconds bonus after a correct match
- -5 seconds after a wrong match

The countdown timer is controlled in GameViewModel.startSoloTimer().

If the timer reaches 0, the game ends immediately.

If the player clears the board before the timer ends, the game ends with a final score summary.

### Multiplayer pass-and-play 2-player

- Player 1 and Player 2 alternate turns
- each correct match gives +1 point to the current player
- highest score wins at the end

### Multiplayer pass-and-play 3-player

- Player 1, Player 2, and Player 3 alternate turns
- each successful match gives +1 to the active player
- the winner is determined by highest score among the three players

### Vs AI Bot

- Player 1 plays the human
- Player 2 is the bot
- the bot receives a delayed turn after human actions
- score is compared between Player 1 and the bot at the end

## 6. Turn rotation rules

The logic is handled in GameViewModel.handleMatchMiss().

#### Standard 2-player flow

```java
int nextPlayer = (activePlayer == 1) ? 2 : 1;
activePlayer.setValue(nextPlayer);
```

#### 3-player flow

```java
int curr = activePlayer.getValue();
int next = (curr % 3) + 1;
activePlayer.setValue(next);
```

The active player always changes after a miss, not after a successful match.

## 7. Board clearing and win detection

The board is considered empty when all pegs are REMOVED.

This check is performed by:

- BoardManager.isBoardEmpty()

When the board is empty:

- GameViewModel.checkWinCondition() runs
- the final score is calculated
- winner message is shown

Win conditions by mode:

- Solo: highest score is the final score; board clear or time out ends the match
- Vs Bot: compare human score vs bot score
- Pass & Play 2P: compare P1 vs P2
- Pass & Play 3P: compare P1 vs P2 vs P3

## 8. Undo logic

Undo is implemented using a MoveSnapshot object in GameViewModel.

Before a selection is processed, the code stores:

- score values
- active player
- current target color
- dice state
- peg state
- remaining time

Then if undo is triggered:

- scores are restored
- active player is restored
- target color is restored
- dice is restored
- time is restored for solo mode
- peg state is restored
- the snapshot is cleared

This is useful because the project supports a state rollback after a move, while keeping the game logic central in the ViewModel.

## 9. Bot intelligence

The bot uses BotMemoryEngine.

### Memory storage

Whenever a peg is revealed, the engine stores the color in a map keyed by peg ID.

The retention probability depends on the bot difficulty:

- easier difficulty = less reliable memory
- harder difficulty = stronger memory retention

The memory is deleted when:

- the peg is removed
- the game restarts

### Bot move selection

The bot selects a peg from the unrevealed list in this order:

1. If a remembered peg matches the current target color, choose it.
2. Otherwise, pick a random unrevealed peg that has not been remembered yet.
3. If there are no remembered choices available, pick a random unrevealed peg.

This is intentionally simple but effective enough to feel like a memory-based AI.

## 10. Game start and reset flow

When a new game starts:

1. timer is cancelled
2. last undo snapshot is cleared
3. game mode is set
4. bot difficulty is selected
5. BoardManager.initBoard() recreates the board
6. scores reset to zero
7. active player reset to Player 1
8. target color reset to null
9. dice reset to false
10. game-over flags reset

In solo mode only, a countdown timer begins immediately.

## 11. Why the logic is in ViewModel

The app keeps gameplay logic outside the UI for a very important reason:

- the UI is only responsible for rendering and input
- the ViewModel handles rules, scoring, turn changes, timer logic, and bot scheduling
- this prevents UI actions from silently breaking state consistency

If another AI needs to reason about the game mechanics, it should begin in [GameViewModel.java](../app/src/main/java/com/bparvej/memorychess/viewmodel/GameViewModel.java).

## 12. Summary of actual rules

The game rule loop is:

1. choose target color from a random unrevealed peg
2. reveal and pick a peg
3. if color matches target => success
4. if color does not match => turn passes
5. correct match removes the peg and scores points
6. clear board or timer ends the game
7. bot uses memory to pick likely matching pegs

This makes the game a memory-based matching challenge rather than a traditional chess game, even though the app name and UI are inspired by a chess-like board layout.
