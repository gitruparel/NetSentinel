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
 * Optimized for 60fps rendering with pre-allocated shaders, matrix transforms, and density scaling.
 */
class SignalRadarMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private fun sp(v: Float): Float = v * context.resources.displayMetrics.scaledDensity

    private var sweepAngle = 0f
    private var rssiDbm = -38

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
    }

    private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val targetPinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val targetPulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(12f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private val sweepMatrix = Matrix()
    private var sweepGradient: SweepGradient? = null
    private var animator: ValueAnimator? = null

    private val gradientColors = intArrayOf(Color.TRANSPARENT, Color.parseColor("#5500E676"))
    private val gradientPositions = floatArrayOf(0f, 1f)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startSweepAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    private fun startSweepAnimation() {
        animator?.cancel()
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cx = w / 2f
        val cy = h / 2f
        sweepGradient = SweepGradient(cx, cy, gradientColors, gradientPositions)
        sweepPaint.shader = sweepGradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (width.coerceAtMost(height) / 2f) - dp(16f)
        if (radius <= 0) return

        gridPaint.color = ContextCompat.getColor(context, R.color.surface_border_glow)
        targetPinPaint.color = ContextCompat.getColor(context, R.color.alert_red)
        targetPulsePaint.color = ContextCompat.getColor(context, R.color.alert_red_alpha20)
        labelPaint.color = ContextCompat.getColor(context, R.color.text_secondary)

        // Draw concentric radar rings
        canvas.drawCircle(centerX, centerY, radius, gridPaint)
        canvas.drawCircle(centerX, centerY, radius * 0.7f, gridPaint)
        canvas.drawCircle(centerX, centerY, radius * 0.4f, gridPaint)
        canvas.drawCircle(centerX, centerY, radius * 0.15f, gridPaint)

        // Draw crosshairs
        canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, gridPaint)
        canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, gridPaint)

        // Draw compass labels
        canvas.drawText("N", centerX, centerY - radius + dp(14f), labelPaint)
        canvas.drawText("S", centerX, centerY + radius - dp(6f), labelPaint)
        canvas.drawText("W", centerX - radius + dp(12f), centerY + dp(4f), labelPaint)
        canvas.drawText("E", centerX + radius - dp(12f), centerY + dp(4f), labelPaint)

        // Rotate sweep cone via Matrix transform (Zero GC object allocation)
        sweepGradient?.let { shader ->
            sweepMatrix.setRotate(sweepAngle, centerX, centerY)
            shader.setLocalMatrix(sweepMatrix)
            canvas.drawCircle(centerX, centerY, radius, sweepPaint)
        }

        // Draw target blip on radar (Rogue AP Target)
        val pinX = centerX + (radius * 0.45f)
        val pinY = centerY - (radius * 0.35f)
        canvas.drawCircle(pinX, pinY, dp(7f), targetPinPaint)
        canvas.drawCircle(pinX, pinY, dp(14f) + (sweepAngle % dp(10f)), targetPulsePaint)
    }

    fun setRssi(rssi: Int) {
        this.rssiDbm = rssi
        invalidate()
    }
}
