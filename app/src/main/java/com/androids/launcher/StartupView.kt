package com.androids.launcher

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * Animación de arranque de dos "pantallas": arriba el wordmark AndroiDS,
 * abajo una barra de carga. Diseño original, no copia ningún logo existente.
 */
class StartupView(context: Context, private val onDone: () -> Unit) : View(context) {

    private val ink = Color.parseColor("#2B2F36")
    private val green = Color.parseColor("#3DDC84")
    private val screenBg = Color.parseColor("#F7F9FB")
    private val hingeColor = Color.parseColor("#8A929D")
    private val track = Color.parseColor("#DDE1E7")

    private val bgPaint = Paint().apply { color = screenBg }
    private val hingePaint = Paint().apply { color = hingeColor }
    private val wordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        color = ink
    }
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = green }
    private val dsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        color = Color.WHITE
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = track }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = green }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = hingeColor
        textAlign = Paint.Align.CENTER
    }

    private var progress = 0f
    private val rect = RectF()

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3200
        interpolator = LinearInterpolator()
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) = onDone()
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.removeAllListeners()
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) onDone()
        return true
    }

    private fun fadeAlpha(): Int {
        val a = when {
            progress < 0.15f -> progress / 0.15f
            progress > 0.85f -> (1f - progress) / 0.15f
            else -> 1f
        }
        return (a.coerceIn(0f, 1f) * 255).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val alpha = fadeAlpha()

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Bisagra entre las dos pantallas
        canvas.drawRect(0f, h / 2f - h * 0.006f, w, h / 2f + h * 0.006f, hingePaint)

        // --- Pantalla de arriba: wordmark "Androi" + insignia "DS" ---
        val textSize = w * 0.15f
        wordPaint.textSize = textSize
        dsPaint.textSize = textSize * 0.9f
        wordPaint.alpha = alpha
        badgePaint.alpha = alpha
        dsPaint.alpha = alpha

        val androiW = wordPaint.measureText("Androi")
        val dsW = dsPaint.measureText("DS")
        val padX = textSize * 0.22f
        val badgeW = dsW + padX * 2
        val gap = textSize * 0.08f
        val total = androiW + gap + badgeW
        val startX = (w - total) / 2f
        val baseline = h * 0.25f + textSize * 0.35f

        canvas.drawText("Androi", startX, baseline, wordPaint)

        val badgeLeft = startX + androiW + gap
        rect.set(
            badgeLeft,
            baseline - textSize * 0.92f,
            badgeLeft + badgeW,
            baseline + textSize * 0.2f
        )
        canvas.drawRoundRect(rect, textSize * 0.2f, textSize * 0.2f, badgePaint)
        canvas.drawText("DS", badgeLeft + padX, baseline - textSize * 0.04f, dsPaint)

        // --- Pantalla de abajo: barra de carga ---
        val barW = w * 0.5f
        val barH = h * 0.012f
        val barLeft = (w - barW) / 2f
        val barTop = h * 0.75f
        trackPaint.alpha = alpha
        fillPaint.alpha = alpha
        rect.set(barLeft, barTop, barLeft + barW, barTop + barH)
        canvas.drawRoundRect(rect, barH, barH, trackPaint)
        rect.set(barLeft, barTop, barLeft + barW * progress, barTop + barH)
        canvas.drawRoundRect(rect, barH, barH, fillPaint)

        hintPaint.textSize = w * 0.035f
        hintPaint.alpha = (alpha * 0.7f).toInt()
        canvas.drawText("Toca para saltar", w / 2f, barTop + barH + h * 0.05f, hintPaint)
    }
}
