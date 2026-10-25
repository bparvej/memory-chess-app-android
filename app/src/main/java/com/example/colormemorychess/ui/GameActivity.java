package com.example.colormemorychess.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.colormemorychess.R;
import com.example.colormemorychess.ads.AdManager;
import com.example.colormemorychess.model.BotDifficulty;
import com.example.colormemorychess.model.GameMode;
import com.example.colormemorychess.model.Peg;
import com.example.colormemorychess.viewmodel.GameViewModel;
import com.google.android.material.button.MaterialButton;

/**
 * Controller connecting UI, BoardView, Mode Selection, ViewModel, and AdMob integration.
 */
public class GameActivity extends AppCompatActivity {

    private GameViewModel viewModel;
    private BoardView boardView;
    private FrameLayout bannerContainer;

    private TextView tvTurnStatus;
    private TextView tvModeBadge;
    private TextView tvTimerScore;
    private TextView tvSecondaryScore;
    private MaterialButton btnRollDice;
    private MaterialButton btnPeekHint;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Bind Views
        boardView = findViewById(R.id.board_view);
        bannerContainer = findViewById(R.id.banner_container);
        tvTurnStatus = findViewById(R.id.tv_turn_status);
        tvModeBadge = findViewById(R.id.tv_mode_badge);
        tvTimerScore = findViewById(R.id.tv_timer_score);
        tvSecondaryScore = findViewById(R.id.tv_secondary_score);
        btnRollDice = findViewById(R.id.btn_roll_dice);
        btnPeekHint = findViewById(R.id.btn_peek_hint);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(GameViewModel.class);

        // Initialize AdMob AdManager
        AdManager.getInstance().initialize(this);
        bannerContainer.post(() -> AdManager.getInstance().loadAdaptiveBanner(GameActivity.this, bannerContainer));

        // Setup Board Callbacks
        setupBoardViewListeners();

        // Setup Button Callbacks
        btnRollDice.setOnClickListener(v -> viewModel.rollDice());
        btnPeekHint.setOnClickListener(v -> onPeekHintRequested());

        // Observe ViewModel State
        observeViewModel();

        // Prompt Mode Selection Dialog on Launch
        showGameModeDialog();
    }

    private void setupBoardViewListeners() {
        boardView.setInteractionListener(new BoardView.OnBoardInteractionListener() {
            @Override
            public void onPegClicked(Peg peg) {
                viewModel.onPegSelected(peg, new GameViewModel.PegResultCallback() {
                    @Override
                    public void onMatchSuccess(Peg p) {
                        boardView.animatePegReveal(p, null);
                    }

                    @Override
                    public void onMatchMiss(Peg p) {
                        boardView.animatePegReveal(p, () -> {
                            mainHandler.postDelayed(() -> boardView.animatePegHide(p, null), 800);
                        });
                    }
                });
            }

            @Override
            public void onDiceClicked() {
                viewModel.rollDice();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getPegsLiveData().observe(this, pegs -> {
            boardView.setPegs(pegs);
        });

        viewModel.getIsDiceRolled().observe(this, rolled -> {
            boardView.setDiceState(viewModel.getCurrentTargetColor().getValue(), Boolean.TRUE.equals(rolled));
            btnRollDice.setEnabled(!Boolean.TRUE.equals(rolled) && !Boolean.TRUE.equals(viewModel.getIsBotThinking().getValue()));
        });

        viewModel.getCurrentTargetColor().observe(this, color -> {
            boardView.setDiceState(color, Boolean.TRUE.equals(viewModel.getIsDiceRolled().getValue()));
        });

        viewModel.getScorePlayer1().observe(this, s1 -> updateHudText());
        viewModel.getScorePlayer2().observe(this, s2 -> updateHudText());
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
                String winMsg = viewModel.getGameOverMessage().getValue();
                showGameOverDialog(winMsg);
            }
        });

        viewModel.getIsBotThinking().observe(this, thinking -> {
            if (Boolean.TRUE.equals(thinking)) {
                tvTurnStatus.setText(R.string.turn_bot);
            }
        });
    }

    private void updateHudText() {
        GameMode mode = viewModel.getGameMode().getValue();
        if (mode == null) return;

        tvModeBadge.setText(mode.getTitle());

        if (mode == GameMode.SOLO_TIME_ATTACK) {
            tvTurnStatus.setText("Solo Mode");
            Long time = viewModel.getTimeRemaining().getValue();
            tvTimerScore.setText(getString(R.string.time_left, time != null ? time : 0));

            Integer score = viewModel.getScorePlayer1().getValue();
            Integer highScore = viewModel.getSoloHighScore().getValue();
            tvSecondaryScore.setText("Score: " + (score != null ? score : 0) + " | High: " + (highScore != null ? highScore : 0));

        } else if (mode == GameMode.PASS_AND_PLAY) {
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

    private void onPeekHintRequested() {
        if (!Boolean.TRUE.equals(viewModel.getIsDiceRolled().getValue())) {
            Toast.makeText(this, R.string.roll_first_prompt, Toast.LENGTH_SHORT).show();
            return;
        }

        AdManager.getInstance().showRewarded(this,
                () -> {
                    // Reward Earned Callback
                    Toast.makeText(this, R.string.peek_hint_used, Toast.LENGTH_SHORT).show();
                    viewModel.triggerPeekHint(null);
                },
                () -> Toast.makeText(this, "Ad not ready yet. Try again!", Toast.LENGTH_SHORT).show()
        );
    }

    private void showGameModeDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_game_mode);
        dialog.setCancelable(false);

        RadioGroup rgModes = dialog.findViewById(R.id.rg_modes);
        RadioButton rbSolo = dialog.findViewById(R.id.rb_solo);
        RadioButton rbVsBot = dialog.findViewById(R.id.rb_vs_bot);
        LinearLayout layoutDiff = dialog.findViewById(R.id.layout_difficulty);
        RadioGroup rgDiff = dialog.findViewById(R.id.rg_difficulty);
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
            GameMode mode = GameMode.SOLO_TIME_ATTACK;
            if (rgModes.getCheckedRadioButtonId() == R.id.rb_pass_play) {
                mode = GameMode.PASS_AND_PLAY;
            } else if (rgModes.getCheckedRadioButtonId() == R.id.rb_vs_bot) {
                mode = GameMode.VS_BOT;
            }

            BotDifficulty difficulty = BotDifficulty.MEDIUM;
            if (mode == GameMode.VS_BOT) {
                if (rbEasy.isChecked()) difficulty = BotDifficulty.EASY;
                else if (rbHard.isChecked()) difficulty = BotDifficulty.HARD;
            }

            dialog.dismiss();
            viewModel.startGame(mode, difficulty);
        });

        dialog.show();
    }

    private void showGameOverDialog(String message) {
        // Show Interstitial Ad on Match Completion
        AdManager.getInstance().showInterstitial(this, () -> {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_game_over);
            dialog.setCancelable(false);

            TextView tvMsg = dialog.findViewById(R.id.tv_dialog_message);
            tvMsg.setText(message);

            MaterialButton btnAgain = dialog.findViewById(R.id.btn_play_again);
            MaterialButton btnChangeMode = dialog.findViewById(R.id.btn_change_mode);

            btnAgain.setOnClickListener(v -> {
                dialog.dismiss();
                viewModel.startGame(viewModel.getGameMode().getValue(), viewModel.getBotDifficulty().getValue());
            });

            btnChangeMode.setOnClickListener(v -> {
                dialog.dismiss();
                showGameModeDialog();
            });

            dialog.show();
        });
    }
}
