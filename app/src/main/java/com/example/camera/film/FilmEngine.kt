package com.example.camera.film

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.camera.model.FilmProfile
import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance local film processing engine.
 * Applies color matrix grading, vignette shading, and organic analog grain.
 */
object FilmEngine {

    /**
     * Applies a film profile and optional manual fine-tuning to a Bitmap.
     * All operations are strictly local, memory-conscious, and fast.
     */
    fun processBitmap(
        source: Bitmap,
        profile: FilmProfile,
        evOffset: Float = 0f,       // -2.0 to +2.0
        contrastDelta: Float = 0f,  // -0.5 to +0.5
        saturationDelta: Float = 0f,// -0.5 to +0.5
        warmthDelta: Float = 0f,    // -0.5 to +0.5
        vignetteStrength: Float = profile.vignette, // 0.0 to 1.0
        grainStrength: Float = profile.grain        // 0.0 to 1.0
    ): Bitmap {
        val width = source.width
        val height = source.height

        // Create target output bitmap
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 1. Calculate combined ColorMatrix
        val totalExposure = (profile.exposureMultiplier * (1f + evOffset * 0.25f)).coerceIn(0.5f, 2.0f)
        val totalContrast = (profile.contrast + contrastDelta).coerceIn(0.5f, 2.0f)
        val totalSaturation = if (profile == FilmProfile.CLASSIC) 0f else (profile.saturation + saturationDelta).coerceIn(0f, 2.0f)
        val totalWarmth = (profile.warmth + warmthDelta).coerceIn(-0.8f, 0.8f)

        val cm = buildMatrix(totalExposure, totalContrast, totalSaturation, totalWarmth, profile)
        paint.colorFilter = ColorMatrixColorFilter(cm)

        // Draw graded base image
        canvas.drawBitmap(source, 0f, 0f, paint)

        // 2. Apply Vignette if enabled
        val vig = vignetteStrength.coerceIn(0f, 1f)
        if (vig > 0.05f) {
            applyVignette(canvas, width, height, vig)
        }

        // 3. Apply Film Grain if enabled
        val grn = grainStrength.coerceIn(0f, 1f)
        if (grn > 0.05f) {
            applyGrain(canvas, width, height, grn)
        }

        return output
    }

    private fun buildMatrix(
        exposure: Float,
        contrast: Float,
        saturation: Float,
        warmth: Float,
        profile: FilmProfile
    ): ColorMatrix {
        val cm = ColorMatrix()

        if (profile == FilmProfile.CLASSIC || saturation <= 0.01f) {
            // Panchromatic B&W
            val lr = 0.299f * contrast
            val lg = 0.587f * contrast
            val lb = 0.114f * contrast
            val t = (1f - contrast) * 128f * exposure
            val bwMatrix = floatArrayOf(
                lr, lg, lb, 0f, t,
                lr, lg, lb, 0f, t,
                lr, lg, lb, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
            cm.set(bwMatrix)
            return cm
        }

        // 1. Base Saturation
        cm.setSaturation(saturation)

        // 2. Contrast & Exposure scaling
        val t = (1f - contrast) * 128f
        val redGain = (1.0f + (warmth * 0.20f)) * exposure * contrast
        val greenGain = exposure * contrast
        val blueGain = (1.0f - (warmth * 0.20f)) * exposure * contrast

        val extraTealBlue = if (profile == FilmProfile.CINEMATIC) 12f else 0f
        val extraWarmRed = if (profile == FilmProfile.WARM_FILM || profile == FilmProfile.PORTRAIT_NATURAL) 10f else 0f

        val scaleMatrix = ColorMatrix(
            floatArrayOf(
                redGain, 0f, 0f, 0f, t + extraWarmRed,
                0f, greenGain, 0f, 0f, t,
                0f, 0f, blueGain, 0f, t + extraTealBlue,
                0f, 0f, 0f, 1f, 0f
            )
        )

        // Multiply matrices
        scaleMatrix.preConcat(cm)
        return scaleMatrix
    }

    /**
     * Applies a smooth radial vignette gradient to darken corners softly.
     */
    private fun applyVignette(canvas: Canvas, width: Int, height: Int, strength: Float) {
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = max(width, height) * 0.72f

        val alphaInt = (strength * 190).toInt().coerceIn(0, 255)
        val colors = intArrayOf(
            Color.TRANSPARENT,
            Color.TRANSPARENT,
            Color.argb((alphaInt * 0.4f).toInt(), 0, 0, 0),
            Color.argb(alphaInt, 0, 0, 0)
        )
        val stops = floatArrayOf(0.0f, 0.55f, 0.85f, 1.0f)

        val gradient = RadialGradient(
            centerX, centerY, radius,
            colors, stops,
            Shader.TileMode.CLAMP
        )

        val vigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vigPaint)
    }

    /**
     * Generates subtle organic film noise / silver grain.
     * Memory-efficient: uses a scaled noise tile rather than allocating millions of pixels.
     */
    private fun applyGrain(canvas: Canvas, width: Int, height: Int, strength: Float) {
        val tileSize = 256
        val noiseBitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(tileSize * tileSize)
        val random = Random(42)

        val grainAlpha = (strength * 48).toInt().coerceIn(4, 90)

        for (i in pixels.indices) {
            val v = random.nextInt(256)
            pixels[i] = Color.argb(grainAlpha, v, v, v)
        }
        noiseBitmap.setPixels(pixels, 0, tileSize, 0, 0, tileSize, tileSize)

        val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
        }

        // Tile over image
        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                canvas.drawBitmap(noiseBitmap, x.toFloat(), y.toFloat(), grainPaint)
                x += tileSize
            }
            y += tileSize
        }

        noiseBitmap.recycle()
    }
}
