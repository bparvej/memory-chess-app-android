package com.example.colormemorychess.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.colormemorychess.model.Peg;
import com.example.colormemorychess.model.PegColor;

import java.util.List;

/**
 * Custom 1:1 Aspect Ratio View rendering a circular wooden board, 24 pegs, and center dice cup.
 */
public class BoardView extends View {

    public interface OnBoardInteractionListener {
        void onPegClicked(Peg peg);
        void onDiceClicked();
    }

    private OnBoardInteractionListener interactionListener;
    private List<Peg> pegs;

    private Paint boardPaint;
    private Paint rimPaint;
    private Paint holePaint;
    private Paint pegWoodPaint;
    private Paint highlightPaint;
    private Paint centerDicePaint;
    private Paint centerDiceRimPaint;
    private Paint diceTextPaint;
    private Paint targetColorPaint;

    private PegColor currentDiceColor = null;
    private boolean isDiceRolled = false;

    private float centerX;
    private float centerY;
    private float boardRadius;
    private float pegRadius;
    private float centerDiceRadius;

    public BoardView(Context context) {
        super(context);
        init();
    }

    public BoardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BoardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Board Wood Paint
        boardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Outer Rim Bevel
        rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setColor(Color.parseColor("#2C1810"));

        // Recessed Hole Paint
        holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        holePaint.setColor(Color.parseColor("#26140B"));

        // Wooden Peg Cap Paint
        pegWoodPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Peek Hint Highlight Glow
        highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setColor(Color.parseColor("#FFD700")); // Gold glow
        highlightPaint.setStrokeWidth(8f);

        // Center Dice Cup
        centerDicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerDiceRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerDiceRimPaint.setStyle(Paint.Style.STROKE);
        centerDiceRimPaint.setColor(Color.parseColor("#D7CCC8"));
        centerDiceRimPaint.setStrokeWidth(6f);

        // Text Paint
        diceTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        diceTextPaint.setColor(Color.WHITE);
        diceTextPaint.setTextAlign(Paint.Align.CENTER);
        diceTextPaint.setTextSize(36f);
        diceTextPaint.setFakeBoldText(true);

        targetColorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public void setInteractionListener(OnBoardInteractionListener listener) {
        this.interactionListener = listener;
    }

    public void setPegs(List<Peg> pegs) {
        this.pegs = pegs;
        invalidate();
    }

    public void setDiceState(PegColor targetColor, boolean isDiceRolled) {
        this.currentDiceColor = targetColor;
        this.isDiceRolled = isDiceRolled;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(width, height);
        // Strict 1:1 aspect ratio constraint
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2.0f;
        centerY = h / 2.0f;
        boardRadius = (Math.min(w, h) / 2.0f) * 0.94f; // 6% padding
        pegRadius = boardRadius * 0.085f;
        centerDiceRadius = boardRadius * 0.22f;

        // Radial Wooden Gradient for Board
        RadialGradient woodGradient = new RadialGradient(
                centerX, centerY, boardRadius,
                new int[]{Color.parseColor("#8D6E63"), Color.parseColor("#5D4037"), Color.parseColor("#3E2723")},
                new float[]{0.0f, 0.75f, 1.0f},
                Shader.TileMode.CLAMP
        );
        boardPaint.setShader(woodGradient);
        rimPaint.setStrokeWidth(boardRadius * 0.06f);

        // Linear Gradient for Wooden Pegs
        LinearGradient pegGradient = new LinearGradient(
                0, 0, pegRadius * 2, pegRadius * 2,
                Color.parseColor("#D7CCC8"), Color.parseColor("#A1887F"),
                Shader.TileMode.CLAMP
        );
        pegWoodPaint.setShader(pegGradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (boardRadius <= 0) return;

        // 1. Draw Wooden Main Board
        canvas.drawCircle(centerX, centerY, boardRadius, boardPaint);
        canvas.drawCircle(centerX, centerY, boardRadius - (rimPaint.getStrokeWidth() / 2), rimPaint);

        // 2. Draw Decorative Wooden Inner Rings
        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(Color.parseColor("#4E342E"));
        ringPaint.setStrokeWidth(3f);
        canvas.drawCircle(centerX, centerY, boardRadius * 0.72f, ringPaint);
        canvas.drawCircle(centerX, centerY, boardRadius * 0.42f, ringPaint);

        // 3. Draw Center Dice Button / Color Indicator
        drawCenterDice(canvas);

        // 4. Draw All 24 Wooden Pegs
        if (pegs != null) {
            for (Peg peg : pegs) {
                drawPeg(canvas, peg);
            }
        }
    }

    private void drawCenterDice(Canvas canvas) {
        if (isDiceRolled && currentDiceColor != null) {
            // Display active rolled color in center dice cup
            targetColorPaint.setColor(currentDiceColor.getColorInt());
            canvas.drawCircle(centerX, centerY, centerDiceRadius, targetColorPaint);
            canvas.drawCircle(centerX, centerY, centerDiceRadius, centerDiceRimPaint);

            // Draw color name or contrast text
            if (currentDiceColor == PegColor.WHITE || currentDiceColor == PegColor.YELLOW) {
                diceTextPaint.setColor(Color.BLACK);
            } else {
                diceTextPaint.setColor(Color.WHITE);
            }
            canvas.drawText(currentDiceColor.getDisplayName(), centerX, centerY + 12f, diceTextPaint);
        } else {
            // Idle Dice Cup
            centerDicePaint.setColor(Color.parseColor("#4E342E"));
            canvas.drawCircle(centerX, centerY, centerDiceRadius, centerDicePaint);
            canvas.drawCircle(centerX, centerY, centerDiceRadius, centerDiceRimPaint);

            diceTextPaint.setColor(Color.WHITE);
            canvas.drawText("ROLL", centerX, centerY + 12f, diceTextPaint);
        }
    }

    private void drawPeg(Canvas canvas, Peg peg) {
        float r = boardRadius * peg.getRingRadiusRatio();
        double rad = Math.toRadians(peg.getAngleDegrees());
        float px = centerX + (float) (r * Math.cos(rad));
        float py = centerY + (float) (r * Math.sin(rad));

        // Always draw recessed hole first
        canvas.drawCircle(px, py, pegRadius, holePaint);

        if (peg.isRemoved()) {
            // Peg removed, hole stays empty
            return;
        }

        // Check if peg is highlighted (Peek Hint active)
        if (peg.isHighlighted()) {
            canvas.drawCircle(px, py, pegRadius + 6f, highlightPaint);
        }

        if (peg.isRevealed() || peg.getAnimProgress() > 0.01f) {
            // REVEALED or ANIMATING state: draw colored peg head
            float scale = 1.0f + (peg.getAnimProgress() * 0.15f); // Subtle lift effect
            Paint colorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            colorPaint.setColor(peg.getColor().getColorInt());

            canvas.drawCircle(px, py - (peg.getAnimProgress() * 10f), pegRadius * scale, colorPaint);

            // Inner highlight dot for 3D sphere illusion
            Paint specular = new Paint(Paint.ANTI_ALIAS_FLAG);
            specular.setColor(Color.argb(80, 255, 255, 255));
            canvas.drawCircle(px - (pegRadius * 0.3f), py - (peg.getAnimProgress() * 10f) - (pegRadius * 0.3f), pegRadius * 0.3f, specular);

        } else {
            // HIDDEN state: draw wooden peg top
            canvas.drawCircle(px, py, pegRadius, pegWoodPaint);

            // Wood grain / top cap detail
            Paint capDetail = new Paint(Paint.ANTI_ALIAS_FLAG);
            capDetail.setStyle(Paint.Style.STROKE);
            capDetail.setColor(Color.parseColor("#8D6E63"));
            capDetail.setStrokeWidth(2f);
            canvas.drawCircle(px, py, pegRadius * 0.6f, capDetail);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float tx = event.getX();
            float ty = event.getY();

            // Check if Center Dice was tapped
            float distToCenter = (float) Math.hypot(tx - centerX, ty - centerY);
            if (distToCenter <= centerDiceRadius) {
                if (interactionListener != null) {
                    interactionListener.onDiceClicked();
                }
                return true;
            }

            // Check if any Peg was tapped
            if (pegs != null) {
                for (Peg peg : pegs) {
                    float r = boardRadius * peg.getRingRadiusRatio();
                    double rad = Math.toRadians(peg.getAngleDegrees());
                    float px = centerX + (float) (r * Math.cos(rad));
                    float py = centerY + (float) (r * Math.sin(rad));

                    float distToPeg = (float) Math.hypot(tx - px, ty - py);
                    if (distToPeg <= pegRadius * 1.4f) {
                        if (interactionListener != null) {
                            interactionListener.onPegClicked(peg);
                        }
                        return true;
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * Smoothly animates lifting / revealing a peg.
     */
    public void animatePegReveal(Peg peg, Runnable onComplete) {
        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            peg.setAnimProgress(val);
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                peg.setState(Peg.State.REVEALED);
                peg.setAnimProgress(0.0f);
                invalidate();
                if (onComplete != null) onComplete.run();
            }
        });
        animator.start();
    }

    /**
     * Smoothly animates hiding a revealed peg back into the wooden hole.
     */
    public void animatePegHide(Peg peg, Runnable onComplete) {
        ValueAnimator animator = ValueAnimator.ofFloat(1.0f, 0.0f);
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            peg.setAnimProgress(val);
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                peg.setState(Peg.State.HIDDEN);
                peg.setAnimProgress(0.0f);
                invalidate();
                if (onComplete != null) onComplete.run();
            }
        });
        animator.start();
    }
}
