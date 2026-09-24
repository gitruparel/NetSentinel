package com.netsentinel.app.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Tactical Google Maps container placeholder view rendering dark cyber grid & safe/threat pin overlays.
 */
class GoogleMapViewPlaceholder @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#15243B")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E3252")
        strokeWidth = 14f
        style = Paint.Style.STROKE
    }

    private val safePinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        style = Paint.Style.FILL
    }

    private val threatPinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5252")
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        // Draw tactical grid
        val step = 100f
        var x = 0f
        while (x < w) {
            canvas.drawLine(x, 0f, x, h, gridPaint)
            x += step
        }
        var y = 0f
        while (y < h) {
            canvas.drawLine(0f, y, w, y, gridPaint)
            y += step
        }

        // Draw simulated roads
        canvas.drawLine(w * 0.1f, h * 0.5f, w * 0.9f, h * 0.5f, roadPaint)
        canvas.drawLine(w * 0.4f, h * 0.1f, w * 0.4f, h * 0.9f, roadPaint)

        // Draw Safe Pins (Green)
        drawPin(canvas, w * 0.3f, h * 0.4f, "Home Mesh (Safe)", safePinPaint)
        drawPin(canvas, w * 0.7f, h * 0.3f, "Office Secure (Safe)", safePinPaint)

        // Draw Threat Pins (Red)
        drawPin(canvas, w * 0.5f, h * 0.65f, "THREAT: Rogue AP Clone", threatPinPaint)
        drawPin(canvas, w * 0.25f, h * 0.75f, "THREAT: ARP Poisoner", threatPinPaint)
    }

    private fun drawPin(canvas: Canvas, px: Float, py: Float, title: String, paint: Paint) {
        canvas.drawCircle(px, py, 18f, paint)
        canvas.drawCircle(px, py, 32f, Paint(paint).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
        })
        canvas.drawText(title, px + 36f, py + 10f, labelPaint)
    }
}
