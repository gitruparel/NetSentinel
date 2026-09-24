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
 */
class CyberMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val userNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val safeNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val safeHexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val threatNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val threatHexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val connectionLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
    }

    private val selectedHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
    }

    private var pulseRadius = 40f
    private var pulseAlpha = 255
    private var scanLineY = 0f
    private var hexAngle = 0f
    private var userLat = 37.7749
    private var userLng = -122.4194

    private var pins: List<ThreatMapPin> = emptyList()
    private var selectedPin: ThreatMapPin? = null

    var onPinSelectedListener: ((ThreatMapPin?) -> Unit)? = null

    private val pulseAnimator = ValueAnimator.ofFloat(20f, 180f).apply {
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
    }

    private val scanAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 4000
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = LinearInterpolator()
        addUpdateListener { anim ->
            scanLineY = anim.animatedValue as Float * height
            invalidate()
        }
    }

    init {
        pulseAnimator.start()
        scanAnimator.start()
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
        val gridSize = 90f
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
        canvas.drawText("TACTICAL CANVAS [60 FPS VECTOR RADAR]", 30f, 44f, textPaint)
        canvas.drawText("GPS: ${String.format("%.4f", userLat)}° N, ${String.format("%.4f", userLng)}° W", 30f, 78f, subTextPaint)

        // Draw Scanning Laser Line
        gridPaint.alpha = 100
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
        canvas.drawCircle(cx, cy, 18f, userNodePaint)
        canvas.drawText("MY DEVICE", cx + 28f, cy + 8f, textPaint)

        // Draw Hexagonal Cyber AP Nodes
        for (pin in pins) {
            val (px, py) = calculatePinScreenPos(pin, cx, cy)
            val fillPaint = if (pin.isThreat) threatNodePaint else safeNodePaint
            val hexPaint = if (pin.isThreat) threatHexPaint else safeHexPaint

            // Signal Ripple Ring
            fillPaint.alpha = 35
            canvas.drawCircle(px, py, 75f, fillPaint)
            fillPaint.alpha = 255

            // Hexagonal Frame
            drawHexagon(canvas, px, py, 28f, hexAngle, hexPaint)

            // Inner Core Dot
            canvas.drawCircle(px, py, 12f, fillPaint)

            // Selected Node Highlight Ring
            val isSel = selectedPin?.id == pin.id
            if (isSel) {
                selectedHighlightPaint.color = ContextCompat.getColor(context, R.color.cyber_cyan)
                canvas.drawCircle(px, py, 38f, selectedHighlightPaint)
            }

            // Labels
            canvas.drawText(pin.title, px + 36f, py - 8f, textPaint)
            canvas.drawText(pin.details, px + 36f, py + 22f, subTextPaint)
        }
    }

    private fun drawHexagon(canvas: Canvas, cx: Float, cy: Float, radius: Float, rotationDeg: Float, paint: Paint) {
        val path = Path()
        val rotRad = Math.toRadians(rotationDeg.toDouble())
        for (i in 0 until 6) {
            val angle = rotRad + i * Math.PI / 3.0
            val x = (cx + radius * cos(angle)).toFloat()
            val y = (cy + radius * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun calculatePinScreenPos(pin: ThreatMapPin, cx: Float, cy: Float): Pair<Float, Float> {
        val latDiff = (pin.latitude - userLat) * 110000f
        val lngDiff = (pin.longitude - userLng) * 110000f
        val px = (cx + lngDiff.toFloat()).coerceIn(70f, width.toFloat() - 70f)
        val py = (cy - latDiff.toFloat()).coerceIn(120f, height.toFloat() - 120f)
        return Pair(px, py)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val cx = width / 2f
            val cy = height / 2f
            var clicked: ThreatMapPin? = null

            for (pin in pins) {
                val (px, py) = calculatePinScreenPos(pin, cx, cy)
                val dist = Math.hypot((event.x - px).toDouble(), (event.y - py).toDouble())
                if (dist < 70) {
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator.cancel()
        scanAnimator.cancel()
    }
}
