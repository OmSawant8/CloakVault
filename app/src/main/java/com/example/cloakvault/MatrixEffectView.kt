package com.example.cloakvault

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import java.util.Random

class MatrixEffectView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint = Paint()
    private val random = Random()

    // CONFIG: Made font bigger and faster for better visibility
    private val fontSize = 50f  // Increased from 40f
    private val columnSize = 50
    private val rainSpeed = 50

    private var txtPosByColumn: IntArray? = null
    private var width = 0
    private var height = 0

    init {
        // CHANGED: Brighter Neon Cyan (High Contrast)
        paint.color = Color.parseColor("#00FFFF")
        paint.textSize = fontSize
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.MONOSPACE // bold look

        // Add a "Glow" effect (Shadow)
        paint.setShadowLayer(10f, 0f, 0f, Color.CYAN)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        width = w
        height = h

        val columnsCount = width / columnSize
        txtPosByColumn = IntArray(columnsCount + 1)

        for (i in txtPosByColumn!!.indices) {
            txtPosByColumn!![i] = random.nextInt(height)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (txtPosByColumn == null) return

        for (i in txtPosByColumn!!.indices) {
            // Mix 1s, 0s, and X to look cooler
            val chars = listOf("1", "0", "X", "¥", "§")
            val char = chars[random.nextInt(chars.size)]

            val x = (i * columnSize).toFloat()
            val y = txtPosByColumn!![i].toFloat()

            // Randomly make some characters brighter (White-ish) for "glint" effect
            if (random.nextFloat() > 0.95f) {
                paint.color = Color.WHITE
            } else {
                paint.color = Color.parseColor("#00FFFF") // Reset to Cyan
            }

            canvas.drawText(char, x, y, paint)

            if (y > height && random.nextFloat() > 0.975f) {
                txtPosByColumn!![i] = 0
            }

            txtPosByColumn!![i] += rainSpeed
        }

        postInvalidateDelayed(33)
    }
}