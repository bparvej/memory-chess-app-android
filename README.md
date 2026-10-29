# Memory Chess

Memory Chess is an Android game built with Java and AndroidX. It combines a circular peg board, memory-based color matching, and multiple game modes including solo play, local multiplayer, and a bot opponent.

## Features

- Solo Time Attack mode
- Pass-and-play local multiplayer for 2 players
- Pass-and-play local multiplayer for 3 players
- Vs AI bot mode
- Animated peg reveal and hide behavior
- Score tracking and win detection
- Undo support for the previous move
- Countdown timer for solo mode
- Ad integration and sound hooks

## Project structure

```text
memory-chess-app-android/
├── app/
│   ├── src/main/java/com/bparvej/memorychess/
│   │   ├── ads/
│   │   ├── audio/
│   │   ├── logic/
│   │   ├── model/
│   │   ├── ui/
│   │   └── viewmodel/
│   └── src/main/res/
├── docs/
│   ├── technical-documentation.md
│   └── game-logic.md
├── build.gradle
├── settings.gradle
├── gradle.properties
├── local.properties
└── README.md
```

## Tech stack

- Java
- Android SDK
- AndroidX Lifecycle
- ViewModel + LiveData
- Material Components
- Google Mobile Ads SDK

## Game overview

The game is not a traditional chess game. The board is a memory-based circular board with colored pegs. A target color is chosen from a random unrevealed peg, and the player must match that color by selecting the correct peg before the turn passes.

Successful matches score points, while wrong matches advance the turn or reduce time depending on the game mode.

## Important code entry points

- App UI: [app/src/main/java/com/bparvej/memorychess/ui/GameActivity.java](app/src/main/java/com/bparvej/memorychess/ui/GameActivity.java)
- Game rules and state: [app/src/main/java/com/bparvej/memorychess/viewmodel/GameViewModel.java](app/src/main/java/com/bparvej/memorychess/viewmodel/GameViewModel.java)
- Board setup: [app/src/main/java/com/bparvej/memorychess/logic/BoardManager.java](app/src/main/java/com/bparvej/memorychess/logic/BoardManager.java)
- Bot logic: [app/src/main/java/com/bparvej/memorychess/logic/BotMemoryEngine.java](app/src/main/java/com/bparvej/memorychess/logic/BotMemoryEngine.java)
- Peg model: [app/src/main/java/com/bparvej/memorychess/model/Peg.java](app/src/main/java/com/bparvej/memorychess/model/Peg.java)

## Documentation

- [docs/technical-documentation.md](docs/technical-documentation.md) – architecture and project understanding
- [docs/game-logic.md](docs/game-logic.md) – rules and gameplay logic written in plain language

## Running the project

1. Open the project in Android Studio.
2. Let Gradle sync dependencies.
3. Select an Android emulator or connected device.
4. Run the app.

### Build commands

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## Notes for AI or future contributors

The most important logic lives in the ViewModel and logic package. If you need to understand behavior, start here before reading UI rendering code.

## License

This project is currently a personal Android game project without a formal public license declared.
