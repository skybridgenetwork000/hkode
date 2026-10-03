package com.hkode.h3nrican3.app

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.SweepGradient
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.EditText
import androidx.core.content.ContextCompat

/**
 * Custom drawable that renders a clean static border when unfocused,
 * and activates an animated sliding gradient stroke strictly on focus or check.
 * Strictly avoids horizontal scaling to prevent screen edge clipping.
 */
class GradientBorderDrawable(
    private val cornerRadius: Float,
    private val strokeWidth: Float,
    private var bgColor: Int,
    private var defaultStrokeColor: Int
) : Drawable() {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = bgColor
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@GradientBorderDrawable.strokeWidth
        color = defaultStrokeColor
    }

    private val rectF = RectF()
    private val matrix = Matrix()
    private var currentAngle = 0f
    private var isFocusedState = false

    // Cyber neon gradient colors: Emerald Green -> Cyan -> Electric Blue -> Violet -> Emerald Green
    private val gradientColors = intArrayOf(
        Color.parseColor("#00E676"),
        Color.parseColor("#00E5FF"),
        Color.parseColor("#2979FF"),
        Color.parseColor("#7C4DFF"),
        Color.parseColor("#00E5FF"),
        Color.parseColor("#00E676")
    )
    private val colorPositions = floatArrayOf(0f, 0.2f, 0.45f, 0.7f, 0.85f, 1f)

    private var sweepGradient: SweepGradient? = null
    private var animator: ValueAnimator? = null

    init {
        strokePaint.shader = null
        strokePaint.color = defaultStrokeColor
    }

    fun setFocused(focused: Boolean) {
        if (isFocusedState == focused) return
        isFocusedState = focused

        if (focused) {
            setupShader()
            strokePaint.shader = sweepGradient
            startAnimation()
        } else {
            stopAnimation()
            strokePaint.shader = null
            strokePaint.color = defaultStrokeColor
            invalidateSelf()
        }
    }

    fun setBackgroundColor(color: Int) {
        bgColor = color
        fillPaint.color = color
        invalidateSelf()
    }

    fun setDefaultStrokeColor(color: Int) {
        defaultStrokeColor = color
        if (!isFocusedState) {
            strokePaint.color = color
            invalidateSelf()
        }
    }

    private fun setupShader() {
        if (rectF.width() > 0 && rectF.height() > 0) {
            sweepGradient = SweepGradient(
                rectF.centerX(),
                rectF.centerY(),
                gradientColors,
                colorPositions
            )
        }
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 3200L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { va ->
                currentAngle = va.animatedValue as Float
                invalidateSelf()
            }
            start()
        }
    }

    fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)
        val inset = strokeWidth / 2f
        rectF.set(
            bounds.left + inset,
            bounds.top + inset,
            bounds.right - inset,
            bounds.bottom - inset
        )
        setupShader()
        if (isFocusedState) {
            strokePaint.shader = sweepGradient
        }
    }

    override fun draw(canvas: Canvas) {
        if (rectF.isEmpty) return

        // 1. Fill inner background
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, fillPaint)

        // 2. Rotate sweep gradient around center if focused
        if (isFocusedState && sweepGradient != null) {
            matrix.setRotate(currentAngle, rectF.centerX(), rectF.centerY())
            sweepGradient?.setLocalMatrix(matrix)
            strokePaint.shader = sweepGradient
        } else {
            strokePaint.shader = null
            strokePaint.color = defaultStrokeColor
        }

        // 3. Draw border stroke
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, strokePaint)
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        strokePaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        strokePaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        /**
         * Attaches focus-controlled gradient animation to a container card and its inner EditText.
         * The sliding gradient only activates when the input has focus, without clipping or stretching bounds.
         */
        fun attachFocusAnimation(
            container: View,
            editText: EditText?,
            cornerRadiusDp: Float = 16f,
            strokeWidthDp: Float = 2.0f,
            onCheck: ((String) -> Unit)? = null
        ): GradientBorderDrawable {
            val density = container.resources.displayMetrics.density
            val radiusPx = cornerRadiusDp * density
            val strokePx = strokeWidthDp * density
            val bgColor = ContextCompat.getColor(container.context, R.color.card_background)
            val strokeColor = ContextCompat.getColor(container.context, R.color.card_stroke)

            val drawable = GradientBorderDrawable(radiusPx, strokePx, bgColor, strokeColor)
            container.background = drawable

            editText?.setOnFocusChangeListener { _, hasFocus ->
                drawable.setFocused(hasFocus)
            }

            editText?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    onCheck?.invoke(s?.toString().orEmpty())
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            return drawable
        }

        fun attachTo(view: View, cornerRadiusDp: Float = 16f, strokeWidthDp: Float = 2.0f) {
            val density = view.resources.displayMetrics.density
            val radiusPx = cornerRadiusDp * density
            val strokePx = strokeWidthDp * density
            val bgColor = ContextCompat.getColor(view.context, R.color.card_background)
            val strokeColor = ContextCompat.getColor(view.context, R.color.card_stroke)

            val drawable = GradientBorderDrawable(radiusPx, strokePx, bgColor, strokeColor)
            view.background = drawable
        }
    }
}
