package com.netsentinel.app.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.netsentinel.app.R

/**
 * Custom animated radar meter view for Hunt Mode featuring sweeping radar line & signal strength meter.
 */
class SignalRadarMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var sweepAngle = 0f
    private var rssiDbm = -38

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val targetPinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val targetPulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    private val sweepMatrix = Matrix()

    private var animator: ValueAnimator? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startSweepAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    private fun startSweepAnimation() {
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 2500
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                sweepAngle = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (width.coerceAtMost(height) / 2f) - 30f

        gridPaint.color = ContextCompat.getColor(context, R.color.surface_border_glow)
        targetPinPaint.color = ContextCompat.getColor(context, R.color.alert_red)
        targetPulsePaint.color = ContextCompat.getColor(context, R.color.alert_red_alpha20)
        labelPaint.color = ContextCompat.getColor(context, R.color.text_secondary)

        // Draw concentric radar rings
        canvas.drawCircle(centerX, centerY, radius, gridPaint)
        canvas.drawCircle(centerX, centerY, radius * 0.7f, gridPaint)
        canvas.drawCircle(centerX, centerY, radius * 0.4f, gridPaint)
        canvas.drawCircle(centerX, centerY, radius * 0.15f, gridPaint)

        // Draw crosshairs & compass ticks
        canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, gridPaint)
        canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, gridPaint)

        canvas.drawText("N", centerX, centerY - radius + 25f, labelPaint)
        canvas.drawText("S", centerX, centerY + radius - 10f, labelPaint)
        canvas.drawText("W", centerX - radius + 20f, centerY + 8f, labelPaint)
        canvas.drawText("E", centerX + radius - 20f, centerY + 8f, labelPaint)

        // Draw radar sweep gradient cone
        val sweepGradient = SweepGradient(
            centerX, centerY,
            intColors(Color.TRANSPARENT, Color.parseColor("#6600E676")),
            floatArrayOf(0f, 1f)
        )
        sweepMatrix.setRotate(sweepAngle, centerX, centerY)
        sweepGradient.setLocalMatrix(sweepMatrix)
        sweepPaint.shader = sweepGradient

        canvas.drawCircle(centerX, centerY, radius, sweepPaint)

        // Draw target blip on radar (Rogue AP Target)
        val pinX = centerX + (radius * 0.45f)
        val pinY = centerY - (radius * 0.35f)
        canvas.drawCircle(pinX, pinY, 14f, targetPinPaint)
        canvas.drawCircle(pinX, pinY, 28f + (sweepAngle % 20f), targetPulsePaint)
    }

    private fun intColors(c1: Int, c2: Int): IntArray = intArrayOf(c1, c2)

    fun setRssi(rssi: Int) {
        this.rssiDbm = rssi
        invalidate()
    }
}
