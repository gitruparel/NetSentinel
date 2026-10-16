package com.netsentinel.app.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.netsentinel.app.R
import com.netsentinel.app.repository.ThreatMapPin
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance 60fps Vector Canvas Cyber Map Engine
 * Rendering glowing hexagonal cyber nodes, signal ripple rings, and topology lines.
 * Optimized with reusable path objects, lifecycle-bound animators, and dp/sp scaling.
 */
class CyberMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private fun sp(v: Float): Float = v * context.resources.displayMetrics.scaledDensity

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(1f)
        style = Paint.Style.STROKE
    }

    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }

    private val userNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val safeNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val safeHexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
    }

    private val threatNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val threatHexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
    }

    private val connectionLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
        pathEffect = DashPathEffect(floatArrayOf(dp(6f), dp(6f)), 0f)
    }

    private val selectedHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(13f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(11f)
    }

    private val hexPath = Path()

    private var pulseRadius = dp(20f)
    private var pulseAlpha = 255
    private var scanLineY = 0f
    private var hexAngle = 0f
    private var userLat = 37.7749
    private var userLng = -122.4194

    private var pins: List<ThreatMapPin> = emptyList()
    private var selectedPin: ThreatMapPin? = null

    var onPinSelectedListener: ((ThreatMapPin?) -> Unit)? = null

    private var pulseAnimator: ValueAnimator? = null
    private var scanAnimator: ValueAnimator? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimators()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimators()
    }

    private fun startAnimators() {
        stopAnimators()

        pulseAnimator = ValueAnimator.ofFloat(dp(15f), dp(90f)).apply {
            duration = 2500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                pulseRadius = anim.animatedValue as Float
                val fraction = anim.animatedFraction
                pulseAlpha = ((1f - fraction) * 255).toInt().coerceIn(0, 255)
                hexAngle = (hexAngle + 1.5f) % 360f
                invalidate()
            }
            start()
        }

        scanAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                scanLineY = anim.animatedValue as Float * height
                invalidate()
            }
            start()
        }
    }

    private fun stopAnimators() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        scanAnimator?.cancel()
        scanAnimator = null
    }

    fun setLocationAndPins(lat: Double, lng: Double, mapPins: List<ThreatMapPin>) {
        this.userLat = lat
        this.userLng = lng
        this.pins = mapPins
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val cx = w / 2f
        val cy = h / 2f

        gridPaint.color = ContextCompat.getColor(context, R.color.surface_border_glow)
        pulsePaint.color = ContextCompat.getColor(context, R.color.cyber_cyan)
        userNodePaint.color = ContextCompat.getColor(context, R.color.cyber_cyan)
        safeNodePaint.color = ContextCompat.getColor(context, R.color.neon_green)
        safeHexPaint.color = ContextCompat.getColor(context, R.color.neon_green)
        threatNodePaint.color = ContextCompat.getColor(context, R.color.alert_red)
        threatHexPaint.color = ContextCompat.getColor(context, R.color.alert_red)
        connectionLinePaint.color = ContextCompat.getColor(context, R.color.cyber_cyan_alpha20)
        textPaint.color = ContextCompat.getColor(context, R.color.text_primary)
        subTextPaint.color = ContextCompat.getColor(context, R.color.text_secondary)

        // Draw Tactical Grid Matrix
        val gridSize = dp(50f)
        var x = 0f
        while (x < w) {
            canvas.drawLine(x, 0f, x, h, gridPaint)
            x += gridSize
        }
        var y = 0f
        while (y < h) {
            canvas.drawLine(0f, y, w, y, gridPaint)
            y += gridSize
        }

        // Draw Lat/Lng Coordinates Bar
        canvas.drawText("TACTICAL CANVAS [60 FPS VECTOR RADAR]", dp(16f), dp(24f), textPaint)
        canvas.drawText("GPS: ${String.format("%.4f", userLat)}° N, ${String.format("%.4f", userLng)}° W", dp(16f), dp(42f), subTextPaint)

        // Draw Scanning Laser Line
        gridPaint.alpha = 80
        canvas.drawLine(0f, scanLineY, w, scanLineY, gridPaint)
        gridPaint.alpha = 255

        // Draw Connection Vector Lines
        for (pin in pins) {
            val (px, py) = calculatePinScreenPos(pin, cx, cy)
            canvas.drawLine(cx, cy, px, py, connectionLinePaint)
        }

        // Draw Device GPS Center Node
        pulsePaint.alpha = pulseAlpha
        canvas.drawCircle(cx, cy, pulseRadius, pulsePaint)
        canvas.drawCircle(cx, cy, dp(10f), userNodePaint)
        canvas.drawText("MY DEVICE", cx + dp(16f), cy + dp(4f), textPaint)

        // Draw Hexagonal Cyber AP Nodes
        for (pin in pins) {
            val (px, py) = calculatePinScreenPos(pin, cx, cy)
            val fillPaint = if (pin.isThreat) threatNodePaint else safeNodePaint
            val hexPaint = if (pin.isThreat) threatHexPaint else safeHexPaint

            // Signal Ripple Ring
            fillPaint.alpha = 30
            canvas.drawCircle(px, py, dp(38f), fillPaint)
            fillPaint.alpha = 255

            // Hexagonal Frame (Zero heap allocation by reusing hexPath)
            drawHexagon(canvas, px, py, dp(16f), hexAngle, hexPaint)

            // Inner Core Dot
            canvas.drawCircle(px, py, dp(6f), fillPaint)

            // Selected Node Highlight Ring
            val isSel = selectedPin?.id == pin.id
            if (isSel) {
                selectedHighlightPaint.color = ContextCompat.getColor(context, R.color.cyber_cyan)
                canvas.drawCircle(px, py, dp(22f), selectedHighlightPaint)
            }

            // Labels
            canvas.drawText(pin.title, px + dp(20f), py - dp(4f), textPaint)
            canvas.drawText(pin.details, px + dp(20f), py + dp(12f), subTextPaint)
        }
    }

    private fun drawHexagon(canvas: Canvas, cx: Float, cy: Float, radius: Float, rotationDeg: Float, paint: Paint) {
        hexPath.reset()
        val rotRad = Math.toRadians(rotationDeg.toDouble())
        for (i in 0 until 6) {
            val angle = rotRad + i * Math.PI / 3.0
            val x = (cx + radius * cos(angle)).toFloat()
            val y = (cy + radius * sin(angle)).toFloat()
            if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
        }
        hexPath.close()
        canvas.drawPath(hexPath, paint)
    }

    private fun calculatePinScreenPos(pin: ThreatMapPin, cx: Float, cy: Float): Pair<Float, Float> {
        val latDiff = (pin.latitude - userLat) * 80000f
        val lngDiff = (pin.longitude - userLng) * 80000f
        val px = (cx + lngDiff.toFloat()).coerceIn(dp(40f), width.toFloat() - dp(40f))
        val py = (cy - latDiff.toFloat()).coerceIn(dp(60f), height.toFloat() - dp(60f))
        return Pair(px, py)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            performClick()
            val cx = width / 2f
            val cy = height / 2f
            var clicked: ThreatMapPin? = null

            for (pin in pins) {
                val (px, py) = calculatePinScreenPos(pin, cx, cy)
                val dist = Math.hypot((event.x - px).toDouble(), (event.y - py).toDouble())
                if (dist < dp(40f)) {
                    clicked = pin
                    break
                }
            }

            selectedPin = clicked
            onPinSelectedListener?.invoke(clicked)
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }
}
