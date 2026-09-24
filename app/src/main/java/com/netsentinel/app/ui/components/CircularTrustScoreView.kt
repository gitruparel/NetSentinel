package com.netsentinel.app.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.netsentinel.app.R

/**
 * Custom animated canvas view displaying a circular NetTrust score indicator
 * with multi-ring glowing outer arcs, dynamic theme-aware text colors, and status badges.
 */
class CircularTrustScoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentScore: Int = 92
    private var animatedScore: Float = 92f

    private val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 36f
        strokeCap = Paint.Cap.ROUND
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 24f
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 24f
        strokeCap = Paint.Cap.ROUND
    }

    private val textScorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 96f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val textLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 30f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val textSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    private val rectF = RectF()
    private var animator: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = 46f
        rectF.set(padding, padding, w.toFloat() - padding, h.toFloat() - padding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        // Theme-Aware Dynamic Color Resolution
        val primaryTextColor = ContextCompat.getColor(context, R.color.text_primary)
        val secondaryTextColor = ContextCompat.getColor(context, R.color.text_secondary)
        val trackBorderColor = ContextCompat.getColor(context, R.color.surface_border_glow)

        trackPaint.color = trackBorderColor
        textScorePaint.color = primaryTextColor
        textSubPaint.color = secondaryTextColor

        // Draw track arc (240 degrees sweep starting from 150 deg)
        val startAngle = 150f
        val maxSweepAngle = 240f
        canvas.drawArc(rectF, startAngle, maxSweepAngle, false, trackPaint)

        // Determine score color
        val scoreColor = when {
            animatedScore >= 85 -> ContextCompat.getColor(context, R.color.neon_green)
            animatedScore >= 60 -> ContextCompat.getColor(context, R.color.warning_yellow)
            else -> ContextCompat.getColor(context, R.color.alert_red)
        }

        outerGlowPaint.color = scoreColor
        outerGlowPaint.alpha = 40
        canvas.drawArc(rectF, startAngle, (animatedScore / 100f) * maxSweepAngle, false, outerGlowPaint)

        progressPaint.color = scoreColor
        val currentSweep = (animatedScore / 100f) * maxSweepAngle
        canvas.drawArc(rectF, startAngle, currentSweep, false, progressPaint)

        // Draw text: Score Number
        canvas.drawText("${animatedScore.toInt()}", centerX, centerY - 15f, textScorePaint)

        // Draw text: Status Label
        val statusText = when {
            animatedScore >= 85 -> "SECURE NETWORK"
            animatedScore >= 60 -> "WARNING ANOMALY"
            else -> "CRITICAL THREAT"
        }
        textLabelPaint.color = scoreColor
        canvas.drawText(statusText, centerX, centerY + 35f, textLabelPaint)

        // Draw text: Subtitle
        canvas.drawText("NetTrust Score (0-100)", centerX, centerY + 75f, textSubPaint)
    }

    fun setScore(score: Int, animate: Boolean = true) {
        this.currentScore = score.coerceIn(0, 100)
        animator?.cancel()

        if (animate) {
            animator = ValueAnimator.ofFloat(animatedScore, score.toFloat()).apply {
                duration = 1000
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    animatedScore = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            animatedScore = score.toFloat()
            invalidate()
        }
    }
}
