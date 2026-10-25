package com.example.colormemorychess.viewmodel;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.colormemorychess.logic.BoardManager;
import com.example.colormemorychess.logic.BotMemoryEngine;
import com.example.colormemorychess.model.BotDifficulty;
import com.example.colormemorychess.model.GameMode;
import com.example.colormemorychess.model.Peg;
import com.example.colormemorychess.model.PegColor;

import java.util.List;

/**
 * ViewModel managing the game state, timer, turn switching, and Bot AI interaction.
 */
public class GameViewModel extends ViewModel {

    public interface PegResultCallback {
        void onMatchSuccess(Peg peg);
        void onMatchMiss(Peg peg);
    }

    private final BoardManager boardManager = new BoardManager();
    private final BotMemoryEngine botMemoryEngine = new BotMemoryEngine(BotDifficulty.MEDIUM);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // LiveData state
    private final MutableLiveData<GameMode> gameMode = new MutableLiveData<>(GameMode.SOLO_TIME_ATTACK);
    private final MutableLiveData<BotDifficulty> botDifficulty = new MutableLiveData<>(BotDifficulty.MEDIUM);
    private final MutableLiveData<List<Peg>> pegsLiveData = new MutableLiveData<>();

    private final MutableLiveData<Integer> scorePlayer1 = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> scorePlayer2 = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> activePlayer = new MutableLiveData<>(1); // 1 = P1, 2 = P2 or Bot

    private final MutableLiveData<Long> timeRemaining = new MutableLiveData<>(90L);
    private final MutableLiveData<PegColor> currentTargetColor = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isDiceRolled = new MutableLiveData<>(false);

    private final MutableLiveData<Boolean> isGameOver = new MutableLiveData<>(false);
    private final MutableLiveData<String> gameOverMessage = new MutableLiveData<>("");
    private final MutableLiveData<Integer> soloHighScore = new MutableLiveData<>(0);

    private final MutableLiveData<String> toastMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isBotThinking = new MutableLiveData<>(false);

    private CountDownTimer countDownTimer;

    public GameViewModel() {
        pegsLiveData.setValue(boardManager.getPegs());
    }

    // Getters for LiveData
    public LiveData<GameMode> getGameMode() { return gameMode; }
    public LiveData<BotDifficulty> getBotDifficulty() { return botDifficulty; }
    public LiveData<List<Peg>> getPegsLiveData() { return pegsLiveData; }
    public LiveData<Integer> getScorePlayer1() { return scorePlayer1; }
    public LiveData<Integer> getScorePlayer2() { return scorePlayer2; }
    public LiveData<Integer> getActivePlayer() { return activePlayer; }
    public LiveData<Long> getTimeRemaining() { return timeRemaining; }
    public LiveData<PegColor> getCurrentTargetColor() { return currentTargetColor; }
    public LiveData<Boolean> getIsDiceRolled() { return isDiceRolled; }
    public LiveData<Boolean> getIsGameOver() { return isGameOver; }
    public LiveData<String> getGameOverMessage() { return gameOverMessage; }
    public LiveData<Integer> getSoloHighScore() { return soloHighScore; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<Boolean> getIsBotThinking() { return isBotThinking; }

    public void clearToast() { toastMessage.setValue(null); }

    public void startGame(GameMode mode, BotDifficulty difficulty) {
        cancelTimer();
        this.gameMode.setValue(mode);
        this.botDifficulty.setValue(difficulty);
        this.botMemoryEngine.setDifficulty(difficulty);
        this.botMemoryEngine.resetMemory();

        boardManager.initBoard();
        pegsLiveData.setValue(boardManager.getPegs());

        scorePlayer1.setValue(0);
        scorePlayer2.setValue(0);
        activePlayer.setValue(1);
        isDiceRolled.setValue(false);
        currentTargetColor.setValue(null);
        isGameOver.setValue(false);
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
                endGame("Time's Up! Final Score: " + scorePlayer1.getValue());
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

    public void onPegSelected(Peg peg, PegResultCallback callback) {
        if (Boolean.TRUE.equals(isGameOver.getValue())) return;

        if (!Boolean.TRUE.equals(isDiceRolled.getValue())) {
            toastMessage.setValue("Please roll the dice first!");
            return;
        }

        if (peg == null || !peg.isHidden()) return;

        PegColor target = currentTargetColor.getValue();
        botMemoryEngine.onPegRevealed(peg);

        if (target != null && peg.getColor() == target) {
            // MATCH SUCCESS!
            handleMatchSuccess(peg, callback);
        } else {
            // MATCH MISS!
            handleMatchMiss(peg, callback);
        }
    }

    private void handleMatchSuccess(Peg peg, PegResultCallback callback) {
        if (gameMode.getValue() == GameMode.SOLO_TIME_ATTACK) {
            int newScore = (scorePlayer1.getValue() != null ? scorePlayer1.getValue() : 0) + 10;
            scorePlayer1.setValue(newScore);
            addTimeBonus(3); // +3s bonus time

            int currentHigh = soloHighScore.getValue() != null ? soloHighScore.getValue() : 0;
            if (newScore > currentHigh) {
                soloHighScore.setValue(newScore);
            }
        } else {
            // 2 Player / Vs Bot
            if (activePlayer.getValue() == 1) {
                int s1 = (scorePlayer1.getValue() != null ? scorePlayer1.getValue() : 0) + 1;
                scorePlayer1.setValue(s1);
            } else {
                int s2 = (scorePlayer2.getValue() != null ? scorePlayer2.getValue() : 0) + 1;
                scorePlayer2.setValue(s2);
            }
            // Matching player gets ANOTHER turn!
        }

        peg.setState(Peg.State.REMOVED);
        botMemoryEngine.onPegRemoved(peg);
        isDiceRolled.setValue(false);
        pegsLiveData.setValue(boardManager.getPegs());

        if (callback != null) callback.onMatchSuccess(peg);

        // Check Win Condition
        if (boardManager.isBoardEmpty()) {
            if (gameMode.getValue() == GameMode.SOLO_TIME_ATTACK) {
                endGame("Board Cleared! Final Score: " + scorePlayer1.getValue());
            } else {
                int p1 = scorePlayer1.getValue() != null ? scorePlayer1.getValue() : 0;
                int p2 = scorePlayer2.getValue() != null ? scorePlayer2.getValue() : 0;
                String winner;
                if (p1 > p2) {
                    winner = "Player 1 Wins! (" + p1 + " - " + p2 + ")";
                } else if (p2 > p1) {
                    winner = (gameMode.getValue() == GameMode.VS_BOT ? "Bot Wins!" : "Player 2 Wins!") + " (" + p2 + " - " + p1 + ")";
                } else {
                    winner = "Tie Game! (" + p1 + " - " + p2 + ")";
                }
                endGame(winner);
            }
        } else if (gameMode.getValue() == GameMode.VS_BOT && activePlayer.getValue() == 2) {
            // Bot matched, gets another turn!
            scheduleBotTurn();
        }
    }

    private void handleMatchMiss(Peg peg, PegResultCallback callback) {
        if (gameMode.getValue() == GameMode.SOLO_TIME_ATTACK) {
            addTimeBonus(-5); // -5s time penalty
        } else {
            // Switch Turn
            int nextPlayer = (activePlayer.getValue() == 1) ? 2 : 1;
            activePlayer.setValue(nextPlayer);
        }

        isDiceRolled.setValue(false);

        if (callback != null) callback.onMatchMiss(peg);

        if (gameMode.getValue() == GameMode.VS_BOT && activePlayer.getValue() == 2) {
            scheduleBotTurn();
        }
    }

    public void scheduleBotTurn() {
        if (Boolean.TRUE.equals(isGameOver.getValue())) return;

        isBotThinking.setValue(true);
        mainHandler.postDelayed(() -> {
            if (Boolean.TRUE.equals(isGameOver.getValue())) return;

            // 1. Bot Rolls Dice
            rollDice();

            // 2. Delay before choosing peg
            mainHandler.postDelayed(() -> {
                if (Boolean.TRUE.equals(isGameOver.getValue())) return;

                PegColor target = currentTargetColor.getValue();
                List<Peg> unrevealed = boardManager.getUnrevealedPegs();
                Peg chosenPeg = botMemoryEngine.selectPeg(target, unrevealed);

                isBotThinking.setValue(false);
                if (chosenPeg != null) {
                    onPegSelected(chosenPeg, null);
                }
            }, 1200);
        }, 1000);
    }

    public void triggerPeekHint(Runnable onComplete) {
        PegColor target = currentTargetColor.getValue();
        List<Peg> unrevealed = boardManager.getUnrevealedPegs();
        Peg matchPeg = null;

        if (target != null) {
            for (Peg peg : unrevealed) {
                if (peg.getColor() == target) {
                    matchPeg = peg;
                    break;
                }
            }
        }

        if (matchPeg == null && !unrevealed.isEmpty()) {
            matchPeg = unrevealed.get(0);
        }

        if (matchPeg != null) {
            final Peg hintPeg = matchPeg;
            hintPeg.setHighlighted(true);
            pegsLiveData.setValue(boardManager.getPegs());

            mainHandler.postDelayed(() -> {
                hintPeg.setHighlighted(false);
                pegsLiveData.setValue(boardManager.getPegs());
                if (onComplete != null) onComplete.run();
            }, 2000);
        }
    }

    private void endGame(String winnerMessage) {
        cancelTimer();
        isGameOver.setValue(true);
        gameOverMessage.setValue(winnerMessage);
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
