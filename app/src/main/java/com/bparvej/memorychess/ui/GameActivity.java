package com.bparvej.memorychess.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bparvej.memorychess.R;
import com.bparvej.memorychess.ads.AdManager;
import com.bparvej.memorychess.audio.SoundManager;
import com.bparvej.memorychess.model.BotDifficulty;
import com.bparvej.memorychess.model.GameMode;
import com.bparvej.memorychess.model.Peg;
import com.bparvej.memorychess.model.PegColor;
import com.bparvej.memorychess.viewmodel.GameViewModel;
import com.google.android.material.button.MaterialButton;

public class GameActivity extends AppCompatActivity {

    private GameViewModel viewModel;
    private BoardView boardView;
    private FrameLayout bannerContainer;

    private TextView tvTurnStatus;
    private TextView tvTimerScore;
    private TextView tvSecondaryScore;
    private MaterialButton btnRollDice;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PegColor lastRolledColor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        boardView = findViewById(R.id.board_view);
        bannerContainer = findViewById(R.id.banner_container);
        tvTurnStatus = findViewById(R.id.tv_turn_status);
        tvTimerScore = findViewById(R.id.tv_timer_score);
        tvSecondaryScore = findViewById(R.id.tv_secondary_score);
        btnRollDice = findViewById(R.id.btn_roll_dice);
        MaterialButton btnUndo = findViewById(R.id.btn_undo);
        MaterialButton btnHowToPlay = findViewById(R.id.btn_how_to_play);
        MaterialButton btnQuit = findViewById(R.id.btn_quit);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showQuitConfirmationDialog();
            }
        });

        viewModel = new ViewModelProvider(this).get(GameViewModel.class);

        viewModel.setBotMoveListener((peg, isMatch, onActionComplete) -> {
            boardView.animatePegReveal(peg, () -> mainHandler.postDelayed(() -> {
                if (!isMatch) {
                    boardView.animatePegHide(peg, onActionComplete);
                } else {
                    onActionComplete.run();
                }
            }, 1200));
        });

        AdManager.getInstance().initialize(this);
        bannerContainer.post(() -> AdManager.getInstance().loadAdaptiveBanner(GameActivity.this, bannerContainer));

        setupBoardViewListeners();

        btnRollDice.setOnClickListener(v -> triggerDiceRoll());
        btnUndo.setOnClickListener(v -> onUndoRequested());
        btnHowToPlay.setOnClickListener(v -> showHowToPlayDialog());
        btnQuit.setOnClickListener(v -> showQuitConfirmationDialog());

        observeViewModel();
        showGameModeDialog();
    }

    private void triggerDiceRoll() {
        if (!Boolean.TRUE.equals(viewModel.getIsDiceRolled().getValue())) {
            SoundManager.getInstance().playDiceRollSound();
            viewModel.rollDice();
        } else {
            Toast.makeText(this, "Dice already rolled! Tap a peg.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBoardViewListeners() {
        boardView.setInteractionListener(new BoardView.OnBoardInteractionListener() {
            @Override
            public void onPegClicked(Peg peg) {
                if (!Boolean.TRUE.equals(viewModel.getIsDiceRolled().getValue())) {
                    Toast.makeText(GameActivity.this, R.string.roll_first_prompt, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (peg == null || !peg.isHidden()) return;

                boardView.hideFingerHint();
                boolean isMatch = viewModel.checkIsMatch(peg);

                boardView.animatePegReveal(peg, () -> {
                    if (isMatch) {
                        viewModel.processPegSelection(peg, true);
                    } else {
                        mainHandler.postDelayed(() -> boardView.animatePegHide(peg, () -> {
                            viewModel.processPegSelection(peg, false);
                        }), 800);
                    }
                });
            }

            @Override
            public void onDiceClicked() {
                triggerDiceRoll();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getPegsLiveData().observe(this, pegs -> boardView.setPegs(pegs));

        viewModel.getIsDiceRolled().observe(this, rolled -> {
            boolean isRolled = Boolean.TRUE.equals(rolled);
            btnRollDice.setEnabled(!isRolled && !Boolean.TRUE.equals(viewModel.getIsBotThinking().getValue()));

            if (!isRolled) {
                lastRolledColor = null;
                boardView.setDiceState(null, false);
                boardView.scheduleIdleFingerHint();
            }
        });

        viewModel.getCurrentTargetColor().observe(this, color -> {
            if (color != null && color != lastRolledColor) {
                lastRolledColor = color;
                boardView.animateLudoDiceRoll(color, null);
            }
        });

        viewModel.getScorePlayer1().observe(this, s1 -> updateHudText());
        viewModel.getScorePlayer2().observe(this, s2 -> updateHudText());
        viewModel.getScorePlayer3().observe(this, s3 -> updateHudText());
        viewModel.getActivePlayer().observe(this, active -> updateHudText());
        viewModel.getGameMode().observe(this, mode -> updateHudText());
        viewModel.getTimeRemaining().observe(this, time -> updateHudText());
        viewModel.getSoloHighScore().observe(this, hs -> updateHudText());

        viewModel.getToastMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(GameActivity.this, message, Toast.LENGTH_SHORT).show();
                viewModel.clearToast();
            }
        });

        viewModel.getIsGameOver().observe(this, isOver -> {
            if (Boolean.TRUE.equals(isOver)) {
                boardView.hideFingerHint();
                String title = viewModel.getGameOverTitle().getValue();
                String msg = viewModel.getGameOverMessage().getValue();

                if (title != null && (title.contains("VICTORY") || title.contains("WINS") || title.contains("CLEARED"))) {
                    SoundManager.getInstance().playWinSound();
                } else {
                    SoundManager.getInstance().playDefeatSound();
                }

                showGameOverDialog(title != null ? title : "Game Over", msg != null ? msg : "");
            }
        });

        viewModel.getIsBotThinking().observe(this, thinking -> {
            if (Boolean.TRUE.equals(thinking)) {
                boardView.hideFingerHint();
                tvTurnStatus.setText(R.string.turn_bot);
            }
        });
    }

    private void updateHudText() {
        GameMode mode = viewModel.getGameMode().getValue();
        if (mode == null) return;

        if (mode == GameMode.SOLO_TIME_ATTACK) {
            tvTurnStatus.setText("Solo Mode");
            Long time = viewModel.getTimeRemaining().getValue();
            tvTimerScore.setText(getString(R.string.time_left, time != null ? time : 0));

            Integer score = viewModel.getScorePlayer1().getValue();
            Integer highScore = viewModel.getSoloHighScore().getValue();
            tvSecondaryScore.setText("Score: " + (score != null ? score : 0) + " | High: " + (highScore != null ? highScore : 0));

        } else if (mode == GameMode.PASS_AND_PLAY_3P) {
            Integer active = viewModel.getActivePlayer().getValue();
            if (active != null && active == 1) tvTurnStatus.setText(R.string.turn_p1);
            else if (active != null && active == 2) tvTurnStatus.setText(R.string.turn_p2);
            else tvTurnStatus.setText(R.string.turn_p3);

            Integer p1 = viewModel.getScorePlayer1().getValue();
            Integer p2 = viewModel.getScorePlayer2().getValue();
            Integer p3 = viewModel.getScorePlayer3().getValue();

            tvTimerScore.setText("P1: " + (p1 != null ? p1 : 0) + " | P2: " + (p2 != null ? p2 : 0));
            tvSecondaryScore.setText("P3: " + (p3 != null ? p3 : 0));

        } else if (mode == GameMode.PASS_AND_PLAY_2P) {
            Integer active = viewModel.getActivePlayer().getValue();
            tvTurnStatus.setText(active != null && active == 1 ? R.string.turn_p1 : R.string.turn_p2);

            Integer p1 = viewModel.getScorePlayer1().getValue();
            tvTimerScore.setText(getString(R.string.p1_score, p1 != null ? p1 : 0));

            Integer p2 = viewModel.getScorePlayer2().getValue();
            tvSecondaryScore.setText(getString(R.string.p2_score, p2 != null ? p2 : 0));

        } else if (mode == GameMode.VS_BOT) {
            Integer active = viewModel.getActivePlayer().getValue();
            if (active != null && active == 2) {
                tvTurnStatus.setText(R.string.turn_bot);
            } else {
                tvTurnStatus.setText(R.string.turn_p1);
            }

            Integer p1 = viewModel.getScorePlayer1().getValue();
            tvTimerScore.setText(getString(R.string.p1_score, p1 != null ? p1 : 0));

            Integer bot = viewModel.getScorePlayer2().getValue();
            tvSecondaryScore.setText(getString(R.string.bot_score, bot != null ? bot : 0));
        }
    }

    private void onUndoRequested() {
        if (!viewModel.canUndo()) {
            Toast.makeText(this, R.string.no_undo_available, Toast.LENGTH_SHORT).show();
            return;
        }

        AdManager.getInstance().showRewarded(this,
                () -> {
                    viewModel.triggerUndoMove(null);
                    boardView.scheduleIdleFingerHint();
                },
                () -> Toast.makeText(this, "Ad not ready yet. Try again!", Toast.LENGTH_SHORT).show()
        );
    }

    private void showHowToPlayDialog() {
        boardView.hideFingerHint();
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_how_to_play);
        dialog.show();
        if (dialog.getWindow() != null) {
            int dialogWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        MaterialButton btnClose = dialog.findViewById(R.id.btn_close_how_to_play);
        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            boardView.scheduleIdleFingerHint();
        });
    }

    private void showQuitConfirmationDialog() {
        boardView.hideFingerHint();
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_quit);
        dialog.show();
        if (dialog.getWindow() != null) {
            int dialogWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel_quit);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm_quit);

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            boardView.scheduleIdleFingerHint();
        });

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            showGameModeDialog();
        });
    }

    private void showGameModeDialog() {
        boardView.hideFingerHint();
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_game_mode);
        dialog.setCancelable(false);
        dialog.show();
        if (dialog.getWindow() != null) {
            int dialogWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        RadioGroup rgModes = dialog.findViewById(R.id.rg_modes);
        LinearLayout layoutDiff = dialog.findViewById(R.id.layout_difficulty);
        RadioButton rbEasy = dialog.findViewById(R.id.rb_easy);
        RadioButton rbHard = dialog.findViewById(R.id.rb_hard);
        Button btnStart = dialog.findViewById(R.id.btn_start_game);

        rgModes.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_vs_bot) {
                layoutDiff.setVisibility(View.VISIBLE);
            } else {
                layoutDiff.setVisibility(View.GONE);
            }
        });

        btnStart.setOnClickListener(v -> {
            GameMode mode = GameMode.VS_BOT;
            int selectedId = rgModes.getCheckedRadioButtonId();

            if (selectedId == R.id.rb_solo) {
                mode = GameMode.SOLO_TIME_ATTACK;
            } else if (selectedId == R.id.rb_pass_play_2p) {
                mode = GameMode.PASS_AND_PLAY_2P;
            } else if (selectedId == R.id.rb_pass_play_3p) {
                mode = GameMode.PASS_AND_PLAY_3P;
            } else if (selectedId == R.id.rb_vs_bot) {
                mode = GameMode.VS_BOT;
            }

            BotDifficulty difficulty = BotDifficulty.EASY;
            if (mode == GameMode.VS_BOT) {
                if (rbEasy.isChecked()) difficulty = BotDifficulty.EASY;
                else if (rbHard.isChecked()) difficulty = BotDifficulty.HARD;
                else difficulty = BotDifficulty.MEDIUM;
            }

            dialog.dismiss();
            viewModel.startGame(mode, difficulty);
            boardView.scheduleIdleFingerHint();
        });
    }

    private void showGameOverDialog(String titleText, String messageText) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_game_over);
        dialog.setCancelable(false);
        dialog.show();

        if (dialog.getWindow() != null) {
            int dialogWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        TextView tvMsg = dialog.findViewById(R.id.tv_dialog_message);
        ImageView ivIcon = dialog.findViewById(R.id.iv_result_icon);

        tvTitle.setText(titleText);
        tvMsg.setText(messageText);

        if (titleText.contains("VICTORY") || titleText.contains("WINS") || titleText.contains("CLEARED")) {
            ivIcon.setImageResource(R.drawable.ic_victory_trophy);
        } else {
            ivIcon.setImageResource(R.drawable.ic_defeat_shield);
        }

        MaterialButton btnAgain = dialog.findViewById(R.id.btn_play_again);
        MaterialButton btnChangeMode = dialog.findViewById(R.id.btn_change_mode);

        btnAgain.setOnClickListener(v -> {
            dialog.dismiss();
            AdManager.getInstance().showInterstitial(this, () -> {
                viewModel.startGame(viewModel.getGameMode().getValue(), viewModel.getBotDifficulty().getValue());
                boardView.scheduleIdleFingerHint();
            });
        });

        btnChangeMode.setOnClickListener(v -> {
            dialog.dismiss();
            AdManager.getInstance().showInterstitial(this, this::showGameModeDialog);
        });
    }
}
