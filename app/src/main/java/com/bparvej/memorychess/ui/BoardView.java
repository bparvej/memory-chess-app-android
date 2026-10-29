package com.bparvej.memorychess.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.Nullable;

import com.bparvej.memorychess.model.Peg;
import com.bparvej.memorychess.model.PegColor;

import java.util.List;

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
    private Paint centerCupPaint;
    private Paint centerCupRimPaint;
    private Paint diceTextPaint;
    private Paint diceFacePaint;
    private Paint diceOutlinePaint;
    private Paint diceShadowPaint;
    private Paint diceSpecularPaint;

    private Paint fingerPaint;
    private Paint fingerOutlinePaint;
    private Paint ripplePaint;

    private PegColor currentDiceColor = null;
    private PegColor rollingTempColor = null;
    private boolean isDiceRolled = false;
    private boolean isRolling = false;

    private float rollProgress = 0f;
    private float diceRotZ = 0f;
    private float diceScale = 1.0f;

    private boolean showFingerHint = false;
    private float fingerTapOffset = 0f;
    private float rippleAlpha = 0f;
    private float rippleScale = 1.0f;

    private ValueAnimator rollAnimator;
    private ValueAnimator fingerAnimator;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable idleHintRunnable = this::showFingerHint;

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
        boardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setColor(Color.parseColor("#2C1810"));

        holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        holePaint.setColor(Color.parseColor("#26140B"));

        pegWoodPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setColor(Color.parseColor("#FFD700"));
        highlightPaint.setStrokeWidth(8f);

        centerCupPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerCupRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerCupRimPaint.setStyle(Paint.Style.STROKE);
        centerCupRimPaint.setColor(Color.parseColor("#D7CCC8"));
        centerCupRimPaint.setStrokeWidth(5f);

        diceTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        diceTextPaint.setColor(Color.WHITE);
        diceTextPaint.setTextAlign(Paint.Align.CENTER);
        diceTextPaint.setTextSize(32f);
        diceTextPaint.setFakeBoldText(true);

        diceFacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        diceOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        diceOutlinePaint.setStyle(Paint.Style.STROKE);
        diceOutlinePaint.setStrokeWidth(5f);
        diceOutlinePaint.setColor(Color.parseColor("#2C1810"));

        diceShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        diceShadowPaint.setColor(Color.argb(110, 0, 0, 0));

        diceSpecularPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        diceSpecularPaint.setColor(Color.argb(55, 255, 255, 255));

        fingerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fingerPaint.setColor(Color.WHITE);
        fingerPaint.setStyle(Paint.Style.FILL);

        fingerOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fingerOutlinePaint.setColor(Color.parseColor("#3E2723"));
        fingerOutlinePaint.setStyle(Paint.Style.STROKE);
        fingerOutlinePaint.setStrokeWidth(4f);

        ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ripplePaint.setStyle(Paint.Style.STROKE);
        ripplePaint.setColor(Color.parseColor("#FFD700"));
        ripplePaint.setStrokeWidth(6f);
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

        if (!isDiceRolled && !isRolling) {
            scheduleIdleFingerHint();
        } else {
            hideFingerHint();
        }
        invalidate();
    }

    public void scheduleIdleFingerHint() {
        handler.removeCallbacks(idleHintRunnable);
        hideFingerHint();
        handler.postDelayed(idleHintRunnable, 3500);
    }

    public void showFingerHint() {
        showFingerHint = true;
        if (fingerAnimator != null) fingerAnimator.cancel();

        fingerAnimator = ValueAnimator.ofFloat(0f, 1f);
        fingerAnimator.setDuration(1200);
        fingerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        fingerAnimator.setRepeatMode(ValueAnimator.RESTART);
        fingerAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            fingerTapOffset = (float) Math.sin(progress * Math.PI * 2) * 18f;
            rippleScale = 1.0f + (progress * 0.5f);
            rippleAlpha = (1.0f - progress) * 255f;
            invalidate();
        });
        fingerAnimator.start();
    }

    public void hideFingerHint() {
        handler.removeCallbacks(idleHintRunnable);
        showFingerHint = false;
        if (fingerAnimator != null) {
            fingerAnimator.cancel();
            fingerAnimator = null;
        }
        invalidate();
    }

    public void animateLudoDiceRoll(PegColor finalColor, Runnable onComplete) {
        hideFingerHint();
        if (isRolling) return;

        isRolling = true;
        if (rollAnimator != null) rollAnimator.cancel();

        rollAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        rollAnimator.setDuration(800);
        rollAnimator.setInterpolator(new OvershootInterpolator(1.3f));

        rollAnimator.addUpdateListener(animation -> {
            rollProgress = animation.getAnimatedFraction();

            diceRotZ = rollProgress * 720f;
            diceScale = 1.0f + (float) Math.sin(rollProgress * Math.PI) * 0.35f;

            if (rollProgress < 0.85f) {
                rollingTempColor = PegColor.getRandom();
            } else {
                rollingTempColor = finalColor;
            }

            invalidate();
        });

        rollAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                isRolling = false;
                currentDiceColor = finalColor;
                isDiceRolled = true;
                diceRotZ = 0f;
                diceScale = 1.0f;
                rollingTempColor = null;
                invalidate();
                if (onComplete != null) onComplete.run();
            }
        });

        rollAnimator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2.0f;
        centerY = h / 2.0f;
        boardRadius = (Math.min(w, h) / 2.0f) * 0.94f;
        pegRadius = boardRadius * 0.082f;
        centerDiceRadius = boardRadius * 0.23f;

        RadialGradient woodGradient = new RadialGradient(
                centerX, centerY, boardRadius,
                new int[]{Color.parseColor("#8D6E63"), Color.parseColor("#5D4037"), Color.parseColor("#3E2723")},
                new float[]{0.0f, 0.75f, 1.0f},
                Shader.TileMode.CLAMP
        );
        boardPaint.setShader(woodGradient);
        rimPaint.setStrokeWidth(boardRadius * 0.06f);

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

        canvas.drawCircle(centerX, centerY, boardRadius, boardPaint);
        canvas.drawCircle(centerX, centerY, boardRadius - (rimPaint.getStrokeWidth() / 2), rimPaint);

        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(Color.parseColor("#4E342E"));
        ringPaint.setStrokeWidth(3f);
        canvas.drawCircle(centerX, centerY, boardRadius * 0.72f, ringPaint);
        canvas.drawCircle(centerX, centerY, boardRadius * 0.42f, ringPaint);

        drawCenterDiceArea(canvas);

        if (pegs != null) {
            for (Peg peg : pegs) {
                drawPeg(canvas, peg);
            }
        }

        if (showFingerHint && !isDiceRolled && !isRolling) {
            drawFingerHint(canvas);
        }
    }

    private void drawCenterDiceArea(Canvas canvas) {
        PegColor activeColor = isRolling ? rollingTempColor : (isDiceRolled ? currentDiceColor : null);

        if (activeColor != null) {
            centerCupPaint.setColor(activeColor.getColorInt());
            canvas.drawCircle(centerX, centerY, centerDiceRadius, centerCupPaint);
            canvas.drawCircle(centerX, centerY, centerDiceRadius, centerCupRimPaint);

            canvas.save();
            canvas.scale(diceScale, diceScale, centerX, centerY);
            if (isRolling) {
                canvas.rotate(diceRotZ, centerX, centerY);
            }

            float cubeSize = centerDiceRadius * 0.82f;
            float cornerRadius = 18f;

            RectF shadowRect = new RectF(centerX - cubeSize / 2 + 5f, centerY - cubeSize / 2 + 6f, centerX + cubeSize / 2 + 5f, centerY + cubeSize / 2 + 6f);
            canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, diceShadowPaint);

            RectF diceRect = new RectF(centerX - cubeSize / 2, centerY - cubeSize / 2, centerX + cubeSize / 2, centerY + cubeSize / 2);
            diceFacePaint.setColor(activeColor.getColorInt());
            canvas.drawRoundRect(diceRect, cornerRadius, cornerRadius, diceFacePaint);

            canvas.drawRoundRect(diceRect, cornerRadius, cornerRadius, diceSpecularPaint);
            canvas.drawRoundRect(diceRect, cornerRadius, cornerRadius, diceOutlinePaint);

            if (!isRolling) {
                diceTextPaint.setColor((activeColor == PegColor.WHITE || activeColor == PegColor.YELLOW) ? Color.BLACK : Color.WHITE);
                canvas.drawText(activeColor.getDisplayName(), centerX, centerY + 10f, diceTextPaint);
            } else {
                Paint pipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                pipPaint.setColor(Color.WHITE);
                float offset = cubeSize * 0.25f;
                canvas.drawCircle(centerX - offset, centerY - offset, 6f, pipPaint);
                canvas.drawCircle(centerX + offset, centerY + offset, 6f, pipPaint);
                canvas.drawCircle(centerX, centerY, 7f, pipPaint);
            }

            canvas.restore();
        } else {
            centerCupPaint.setColor(Color.parseColor("#3E2723"));
            canvas.drawCircle(centerX, centerY, centerDiceRadius, centerCupPaint);
            canvas.drawCircle(centerX, centerY, centerDiceRadius, centerCupRimPaint);

            canvas.save();
            canvas.scale(diceScale, diceScale, centerX, centerY);

            float cubeSize = centerDiceRadius * 0.82f;
            float cornerRadius = 18f;

            RectF shadowRect = new RectF(centerX - cubeSize / 2 + 5f, centerY - cubeSize / 2 + 6f, centerX + cubeSize / 2 + 5f, centerY + cubeSize / 2 + 6f);
            canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, diceShadowPaint);

            RectF diceRect = new RectF(centerX - cubeSize / 2, centerY - cubeSize / 2, centerX + cubeSize / 2, centerY + cubeSize / 2);
            diceFacePaint.setColor(Color.parseColor("#5D4037"));
            canvas.drawRoundRect(diceRect, cornerRadius, cornerRadius, diceFacePaint);

            Paint goldOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
            goldOutline.setStyle(Paint.Style.STROKE);
            goldOutline.setColor(Color.parseColor("#FFD700"));
            goldOutline.setStrokeWidth(5f);
            canvas.drawRoundRect(diceRect, cornerRadius, cornerRadius, goldOutline);

            diceTextPaint.setColor(Color.WHITE);
            canvas.drawText("ROLL", centerX, centerY + 10f, diceTextPaint);

            canvas.restore();
        }
    }

    private void drawFingerHint(Canvas canvas) {
        ripplePaint.setAlpha((int) Math.max(0, Math.min(255, rippleAlpha)));
        canvas.drawCircle(centerX, centerY, centerDiceRadius * rippleScale, ripplePaint);

        float handBaseX = centerX + 12f;
        float handBaseY = centerY + centerDiceRadius + 28f + fingerTapOffset;

        Path handPath = new Path();
        handPath.moveTo(handBaseX - 16f, handBaseY + 45f);
        handPath.lineTo(handBaseX - 16f, handBaseY + 15f);
        handPath.lineTo(handBaseX - 12f, handBaseY - 35f);
        handPath.lineTo(handBaseX - 2f, handBaseY - 35f);
        handPath.lineTo(handBaseX - 2f, handBaseY + 5f);
        handPath.lineTo(handBaseX + 16f, handBaseY + 12f);
        handPath.lineTo(handBaseX + 16f, handBaseY + 45f);
        handPath.close();

        canvas.drawPath(handPath, fingerPaint);
        canvas.drawPath(handPath, fingerOutlinePaint);
    }

    private void drawPeg(Canvas canvas, Peg peg) {
        float r = boardRadius * peg.getRingRadiusRatio();
        double rad = Math.toRadians(peg.getAngleDegrees());
        float px = centerX + (float) (r * Math.cos(rad));
        float py = centerY + (float) (r * Math.sin(rad));

        canvas.drawCircle(px, py, pegRadius, holePaint);
        if (peg.isRemoved()) return;

        if (peg.isHighlighted()) {
            canvas.drawCircle(px, py, pegRadius + 6f, highlightPaint);
        }

        if (peg.isRevealed() || peg.getAnimProgress() > 0.01f) {
            float scale = 1.0f + (peg.getAnimProgress() * 0.15f);
            Paint colorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            colorPaint.setColor(peg.getColor().getColorInt());
            canvas.drawCircle(px, py - (peg.getAnimProgress() * 10f), pegRadius * scale, colorPaint);

            Paint specular = new Paint(Paint.ANTI_ALIAS_FLAG);
            specular.setColor(Color.argb(80, 255, 255, 255));
            canvas.drawCircle(px - (pegRadius * 0.3f), py - (peg.getAnimProgress() * 10f) - (pegRadius * 0.3f), pegRadius * 0.3f, specular);
        } else {
            canvas.drawCircle(px, py, pegRadius, pegWoodPaint);

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
            hideFingerHint();

            float tx = event.getX();
            float ty = event.getY();

            if (Math.hypot(tx - centerX, ty - centerY) <= centerDiceRadius * 1.2f) {
                if (interactionListener != null) {
                    interactionListener.onDiceClicked();
                }
                return true;
            }

            if (pegs != null) {
                for (Peg peg : pegs) {
                    float r = boardRadius * peg.getRingRadiusRatio();
                    double rad = Math.toRadians(peg.getAngleDegrees());
                    float px = centerX + (float) (r * Math.cos(rad));
                    float py = centerY + (float) (r * Math.sin(rad));

                    if (Math.hypot(tx - px, ty - py) <= pegRadius * 1.4f) {
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

    public void animatePegReveal(Peg peg, Runnable onComplete) {
        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(300);
        animator.addUpdateListener(anim -> {
            peg.setAnimProgress((float) anim.getAnimatedValue());
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

    public void animatePegHide(Peg peg, Runnable onComplete) {
        ValueAnimator animator = ValueAnimator.ofFloat(1.0f, 0.0f);
        animator.setDuration(300);
        animator.addUpdateListener(anim -> {
            peg.setAnimProgress((float) anim.getAnimatedValue());
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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacksAndMessages(null);
        if (rollAnimator != null) rollAnimator.cancel();
        if (fingerAnimator != null) fingerAnimator.cancel();
    }
}
