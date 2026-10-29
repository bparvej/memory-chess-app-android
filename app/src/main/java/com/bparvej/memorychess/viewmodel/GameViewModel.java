package com.bparvej.memorychess.viewmodel;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bparvej.memorychess.logic.BoardManager;
import com.bparvej.memorychess.logic.BotMemoryEngine;
import com.bparvej.memorychess.model.BotDifficulty;
import com.bparvej.memorychess.model.GameMode;
import com.bparvej.memorychess.model.Peg;
import com.bparvej.memorychess.model.PegColor;

import java.util.List;

public class GameViewModel extends ViewModel {

    public interface BotMoveListener {
        void onBotChosePeg(Peg peg, boolean isMatch, Runnable onActionComplete);
    }

    private static class MoveSnapshot {
        final int scoreP1;
        final int scoreP2;
        final int scoreP3;
        final int activeP;
        final long time;
        final Peg peg;
        final Peg.State pegState;
        final PegColor targetColor;
        final boolean diceRolled;

        MoveSnapshot(int scoreP1, int scoreP2, int scoreP3, int activeP, long time, Peg peg, Peg.State pegState, PegColor targetColor, boolean diceRolled) {
            this.scoreP1 = scoreP1;
            this.scoreP2 = scoreP2;
            this.scoreP3 = scoreP3;
            this.activeP = activeP;
            this.time = time;
            this.peg = peg;
            this.pegState = pegState;
            this.targetColor = targetColor;
            this.diceRolled = diceRolled;
        }
    }

    private final BoardManager boardManager = new BoardManager();
    private final BotMemoryEngine botMemoryEngine = new BotMemoryEngine(BotDifficulty.EASY);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private MoveSnapshot lastMoveSnapshot = null;
    private BotMoveListener botMoveListener = null;

    private final MutableLiveData<GameMode> gameMode = new MutableLiveData<>(GameMode.VS_BOT);
    private final MutableLiveData<BotDifficulty> botDifficulty = new MutableLiveData<>(BotDifficulty.EASY);
    private final MutableLiveData<List<Peg>> pegsLiveData = new MutableLiveData<>();

    private final MutableLiveData<Integer> scorePlayer1 = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> scorePlayer2 = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> scorePlayer3 = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> activePlayer = new MutableLiveData<>(1);

    private final MutableLiveData<Long> timeRemaining = new MutableLiveData<>(90L);
    private final MutableLiveData<PegColor> currentTargetColor = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isDiceRolled = new MutableLiveData<>(false);

    private final MutableLiveData<Boolean> isGameOver = new MutableLiveData<>(false);
    private final MutableLiveData<String> gameOverTitle = new MutableLiveData<>("");
    private final MutableLiveData<String> gameOverMessage = new MutableLiveData<>("");
    private final MutableLiveData<Integer> soloHighScore = new MutableLiveData<>(0);

    private final MutableLiveData<String> toastMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isBotThinking = new MutableLiveData<>(false);

    private CountDownTimer countDownTimer;

    public GameViewModel() {
        pegsLiveData.setValue(boardManager.getPegs());
    }

    public void setBotMoveListener(BotMoveListener listener) {
        this.botMoveListener = listener;
    }

    public LiveData<GameMode> getGameMode() { return gameMode; }
    public LiveData<BotDifficulty> getBotDifficulty() { return botDifficulty; }
    public LiveData<List<Peg>> getPegsLiveData() { return pegsLiveData; }
    public LiveData<Integer> getScorePlayer1() { return scorePlayer1; }
    public LiveData<Integer> getScorePlayer2() { return scorePlayer2; }
    public LiveData<Integer> getScorePlayer3() { return scorePlayer3; }
    public LiveData<Integer> getActivePlayer() { return activePlayer; }
    public LiveData<Long> getTimeRemaining() { return timeRemaining; }
    public LiveData<PegColor> getCurrentTargetColor() { return currentTargetColor; }
    public LiveData<Boolean> getIsDiceRolled() { return isDiceRolled; }
    public LiveData<Boolean> getIsGameOver() { return isGameOver; }
    public LiveData<String> getGameOverTitle() { return gameOverTitle; }
    public LiveData<String> getGameOverMessage() { return gameOverMessage; }
    public LiveData<Integer> getSoloHighScore() { return soloHighScore; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<Boolean> getIsBotThinking() { return isBotThinking; }

    public void clearToast() { toastMessage.setValue(null); }

    public void startGame(GameMode mode, BotDifficulty difficulty) {
        cancelTimer();
        lastMoveSnapshot = null;
        this.gameMode.setValue(mode);
        this.botDifficulty.setValue(difficulty);
        this.botMemoryEngine.setDifficulty(difficulty);
        this.botMemoryEngine.resetMemory();

        boardManager.initBoard();
        pegsLiveData.setValue(boardManager.getPegs());

        scorePlayer1.setValue(0);
        scorePlayer2.setValue(0);
        scorePlayer3.setValue(0);
        activePlayer.setValue(1);
        isDiceRolled.setValue(false);
        currentTargetColor.setValue(null);
        isGameOver.setValue(false);
        gameOverTitle.setValue("");
        gameOverMessage.setValue("");
        isBotThinking.setValue(false);

        if (mode == GameMode.SOLO_TIME_ATTACK) {
            startSoloTimer(90);
        }
    }

    private void startSoloTimer(long seconds) {
        cancelTimer();
        timeRemaining.setValue(seconds);
        countDownTimer = new CountDownTimer(seconds * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining.setValue(millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                timeRemaining.setValue(0L);
                endGame("TIME'S UP!", "Final Score: " + scorePlayer1.getValue() + "\nTime Remaining: 0s");
            }
        }.start();
    }

    private void addTimeBonus(long bonusSeconds) {
        if (gameMode.getValue() == GameMode.SOLO_TIME_ATTACK && countDownTimer != null) {
            long current = timeRemaining.getValue() != null ? timeRemaining.getValue() : 0;
            long newTime = Math.max(1, current + bonusSeconds);
            startSoloTimer(newTime);
        }
    }

    public void rollDice() {
        if (Boolean.TRUE.equals(isGameOver.getValue())) return;
        if (Boolean.TRUE.equals(isDiceRolled.getValue())) {
            toastMessage.setValue("Dice already rolled! Pick a peg.");
            return;
        }

        PegColor rolledColor = boardManager.rollDice();
        currentTargetColor.setValue(rolledColor);
        isDiceRolled.setValue(true);
    }

    public boolean checkIsMatch(Peg peg) {
        PegColor target = currentTargetColor.getValue();
        return target != null && peg.getColor() == target;
    }

    public void processPegSelection(Peg peg, boolean isMatch) {
        if (Boolean.TRUE.equals(isGameOver.getValue())) return;
        if (peg == null) return;

        saveSnapshot(peg);
        botMemoryEngine.onPegRevealed(peg);

        if (isMatch) {
            handleMatchSuccess(peg);
        } else {
            handleMatchMiss(peg);
        }
    }

    private void saveSnapshot(Peg peg) {
        lastMoveSnapshot = new MoveSnapshot(
                scorePlayer1.getValue() != null ? scorePlayer1.getValue() : 0,
                scorePlayer2.getValue() != null ? scorePlayer2.getValue() : 0,
                scorePlayer3.getValue() != null ? scorePlayer3.getValue() : 0,
                activePlayer.getValue() != null ? activePlayer.getValue() : 1,
                timeRemaining.getValue() != null ? timeRemaining.getValue() : 0,
                peg,
                peg.getState(),
                currentTargetColor.getValue(),
                Boolean.TRUE.equals(isDiceRolled.getValue())
        );
    }

    public boolean canUndo() {
        return lastMoveSnapshot != null && !Boolean.TRUE.equals(isGameOver.getValue());
    }

    public void triggerUndoMove(Runnable onComplete) {
        if (!canUndo()) {
            toastMessage.setValue("No move to undo!");
            return;
        }

        mainHandler.removeCallbacksAndMessages(null);
        isBotThinking.setValue(false);

        scorePlayer1.setValue(lastMoveSnapshot.scoreP1);
        scorePlayer2.setValue(lastMoveSnapshot.scoreP2);
        scorePlayer3.setValue(lastMoveSnapshot.scoreP3);
        activePlayer.setValue(lastMoveSnapshot.activeP);
        currentTargetColor.setValue(lastMoveSnapshot.targetColor);
        isDiceRolled.setValue(lastMoveSnapshot.diceRolled);

        if (gameMode.getValue() == GameMode.SOLO_TIME_ATTACK) {
            startSoloTimer(lastMoveSnapshot.time);
        }

        lastMoveSnapshot.peg.setState(lastMoveSnapshot.pegState);
        pegsLiveData.setValue(boardManager.getPegs());

        lastMoveSnapshot = null;
        toastMessage.setValue("Move undone! Turn & score restored.");

        if (onComplete != null) onComplete.run();
    }

    private void handleMatchSuccess(Peg peg) {
        if (gameMode.getValue() == GameMode.SOLO_TIME_ATTACK) {
            int newScore = (scorePlayer1.getValue() != null ? scorePlayer1.getValue() : 0) + 10;
            scorePlayer1.setValue(newScore);
            addTimeBonus(3);

            int currentHigh = soloHighScore.getValue() != null ? soloHighScore.getValue() : 0;
            if (newScore > currentHigh) {
                soloHighScore.setValue(newScore);
            }
        } else {
            int active = activePlayer.getValue() != null ? activePlayer.getValue() : 1;
            if (active == 1) {
                scorePlayer1.setValue((scorePlayer1.getValue() != null ? scorePlayer1.getValue() : 0) + 1);
            } else if (active == 2) {
                scorePlayer2.setValue((scorePlayer2.getValue() != null ? scorePlayer2.getValue() : 0) + 1);
            } else if (active == 3) {
                scorePlayer3.setValue((scorePlayer3.getValue() != null ? scorePlayer3.getValue() : 0) + 1);
            }
        }

        peg.setState(Peg.State.REMOVED);
        botMemoryEngine.onPegRemoved(peg);
        isDiceRolled.setValue(false);
        pegsLiveData.setValue(boardManager.getPegs());

        if (boardManager.isBoardEmpty()) {
            checkWinCondition();
        } else if (gameMode.getValue() == GameMode.VS_BOT && activePlayer.getValue() == 2) {
            scheduleBotTurn();
        }
    }

    private void handleMatchMiss(Peg peg) {
        if (gameMode.getValue() == GameMode.SOLO_TIME_ATTACK) {
            addTimeBonus(-5);
        } else if (gameMode.getValue() == GameMode.PASS_AND_PLAY_3P) {
            int curr = activePlayer.getValue() != null ? activePlayer.getValue() : 1;
            int next = (curr % 3) + 1;
            activePlayer.setValue(next);
        } else {
            int nextPlayer = (activePlayer.getValue() == 1) ? 2 : 1;
            activePlayer.setValue(nextPlayer);
        }

        isDiceRolled.setValue(false);
        pegsLiveData.setValue(boardManager.getPegs());

        if (gameMode.getValue() == GameMode.VS_BOT && activePlayer.getValue() == 2) {
            scheduleBotTurn();
        }
    }

    private void checkWinCondition() {
        GameMode mode = gameMode.getValue();
        int p1 = scorePlayer1.getValue() != null ? scorePlayer1.getValue() : 0;
        int p2 = scorePlayer2.getValue() != null ? scorePlayer2.getValue() : 0;
        int p3 = scorePlayer3.getValue() != null ? scorePlayer3.getValue() : 0;
        long time = timeRemaining.getValue() != null ? timeRemaining.getValue() : 0;

        if (mode == GameMode.SOLO_TIME_ATTACK) {
            endGame("BOARD CLEARED!", "Final Score: " + p1 + "\nTime Remaining: " + time + "s\nHigh Score: " + soloHighScore.getValue());
        } else if (mode == GameMode.VS_BOT) {
            if (p1 > p2) {
                endGame("🏆 VICTORY!", "Player 1 Won against the Bot!\n\nPlayer 1 Score: " + p1 + "\nBot Score: " + p2);
            } else if (p2 > p1) {
                endGame("💀 GAME OVER", "Bot Won the Match!\n\nBot Score: " + p2 + "\nPlayer 1 Score: " + p1);
            } else {
                endGame("🤝 TIE GAME!", "It's a Tie!\n\nScore: " + p1 + " - " + p2);
            }
        } else if (mode == GameMode.PASS_AND_PLAY_2P) {
            if (p1 > p2) {
                endGame("🏆 PLAYER 1 WINS!", "Player 1 Won!\n\nPlayer 1: " + p1 + "\nPlayer 2: " + p2);
            } else if (p2 > p1) {
                endGame("🏆 PLAYER 2 WINS!", "Player 2 Won!\n\nPlayer 2: " + p2 + "\nPlayer 1: " + p1);
            } else {
                endGame("🤝 TIE GAME!", "It's a Tie!\n\nScore: " + p1 + " - " + p2);
            }
        } else if (mode == GameMode.PASS_AND_PLAY_3P) {
            if (p1 >= p2 && p1 >= p3) {
                endGame("🏆 PLAYER 1 WINS!", "Player 1 Won!\n\nP1: " + p1 + " | P2: " + p2 + " | P3: " + p3);
            } else if (p2 >= p1 && p2 >= p3) {
                endGame("🏆 PLAYER 2 WINS!", "Player 2 Won!\n\nP1: " + p1 + " | P2: " + p2 + " | P3: " + p3);
            } else {
                endGame("🏆 PLAYER 3 WINS!", "Player 3 Won!\n\nP1: " + p1 + " | P2: " + p2 + " | P3: " + p3);
            }
        }
    }

    public void scheduleBotTurn() {
        if (Boolean.TRUE.equals(isGameOver.getValue())) return;

        isBotThinking.setValue(true);
        mainHandler.postDelayed(() -> {
            if (Boolean.TRUE.equals(isGameOver.getValue())) return;

            rollDice();

            mainHandler.postDelayed(() -> {
                if (Boolean.TRUE.equals(isGameOver.getValue())) return;

                PegColor target = currentTargetColor.getValue();
                List<Peg> unrevealed = boardManager.getUnrevealedPegs();
                Peg chosenPeg = botMemoryEngine.selectPeg(target, unrevealed);

                isBotThinking.setValue(false);
                if (chosenPeg != null) {
                    boolean isMatch = (target != null && chosenPeg.getColor() == target);
                    if (botMoveListener != null) {
                        botMoveListener.onBotChosePeg(chosenPeg, isMatch, () -> {
                            processPegSelection(chosenPeg, isMatch);
                        });
                    } else {
                        processPegSelection(chosenPeg, isMatch);
                    }
                }
            }, 1200);
        }, 1000);
    }

    private void endGame(String title, String message) {
        cancelTimer();
        isGameOver.setValue(true);
        gameOverTitle.setValue(title);
        gameOverMessage.setValue(message);
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelTimer();
        mainHandler.removeCallbacksAndMessages(null);
    }
}
