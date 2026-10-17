package com.example.camera.ai

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.camera.model.AiSceneResult
import com.example.camera.model.FilmProfile
import com.example.camera.model.SceneCategory
import java.nio.ByteBuffer
import kotlin.math.abs

/**
 * On-Device Real-Time Scene Analyzer.
 * Completely local, runs on throttled background thread without network or cloud.
 * Analyzes luminance histograms, YCbCr chroma distribution (skin-tone hue detection),
 * edge gradients, and ambient lighting.
 */
class SceneAnalyzer(
    private val onResult: (AiSceneResult) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastAnalyzedTimestamp = 0L
    private val throttleIntervalMs = 600L // Analyze 1-2 times per second max for optimal battery & performance

    override fun analyze(image: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp < throttleIntervalMs) {
            image.close()
            return
        }
        lastAnalyzedTimestamp = currentTimestamp

        try {
            val yPlane = image.planes[0]
            val buffer: ByteBuffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val pixelStride = yPlane.pixelStride
            val width = image.width
            val height = image.height

            // Sample pixels across grid (downsampled for lightning fast analysis)
            val sampleStepX = maxOf(4, width / 40)
            val sampleStepY = maxOf(4, height / 40)

            var totalLuma = 0L
            var sampleCount = 0
            var lowLumaCount = 0
            var highLumaCount = 0

            var centerLumaSum = 0L
            var centerCount = 0

            val centerXStart = width * 3 / 10
            val centerXEnd = width * 7 / 10
            val centerYStart = height * 3 / 10
            val centerYEnd = height * 7 / 10

            var prevLuma = -1
            var edgeEnergy = 0L

            for (y in 0 until height step sampleStepY) {
                val rowOffset = y * rowStride
                for (x in 0 until width step sampleStepX) {
                    val index = rowOffset + x * pixelStride
                    if (index < buffer.limit()) {
                        val luma = buffer.get(index).toInt() and 0xFF
                        totalLuma += luma
                        sampleCount++

                        if (luma < 45) lowLumaCount++
                        if (luma > 215) highLumaCount++

                        if (x in centerXStart..centerXEnd && y in centerYStart..centerYEnd) {
                            centerLumaSum += luma
                            centerCount++
                        }

                        if (prevLuma != -1) {
                            edgeEnergy += abs(luma - prevLuma)
                        }
                        prevLuma = luma
                    }
                }
            }

            if (sampleCount == 0) {
                image.close()
                return
            }

            val avgLuma = (totalLuma / sampleCount).toFloat()
            val centerAvgLuma = if (centerCount > 0) (centerLumaSum / centerCount).toFloat() else avgLuma
            val lowLumaRatio = lowLumaCount.toFloat() / sampleCount
            val highLumaRatio = highLumaCount.toFloat() / sampleCount
            val avgEdge = edgeEnergy.toFloat() / sampleCount

            // Inspect Chroma planes (U/V) if available for skin tone detection
            var skinToneScore = 0f
            if (image.planes.size >= 3) {
                val uPlane = image.planes[1]
                val vPlane = image.planes[2]
                val uBuf = uPlane.buffer
                val vBuf = vPlane.buffer

                var skinMatches = 0
                var chromaSamples = 0
                val chromaStep = sampleStepX * 2

                for (y in centerYStart until centerYEnd step sampleStepY * 2) {
                    for (x in centerXStart until centerXEnd step chromaStep) {
                        val uIdx = minOf(y * uPlane.rowStride + x * uPlane.pixelStride, uBuf.limit() - 1)
                        val vIdx = minOf(y * vPlane.rowStride + x * vPlane.pixelStride, vBuf.limit() - 1)

                        val u = (uBuf.get(uIdx).toInt() and 0xFF) - 128
                        val v = (vBuf.get(vIdx).toInt() and 0xFF) - 128

                        // Typical human skin tone range in YCbCr: Cb in [-40, -10], Cr in [10, 45]
                        if (u in -45..-5 && v in 8..50) {
                            skinMatches++
                        }
                        chromaSamples++
                    }
                }
                if (chromaSamples > 0) {
                    skinToneScore = skinMatches.toFloat() / chromaSamples
                }
            }

            // Determine Scene Category & Director Guidance
            val sceneResult = when {
                avgLuma < 38f || lowLumaRatio > 0.55f -> {
                    AiSceneResult(
                        category = SceneCategory.LOW_LIGHT,
                        confidence = 0.90f,
                        directorTip = "Low light detected • Keep camera steady for sharp exposure",
                        recommendedProfile = FilmProfile.MOODY,
                        averageLuminance = avgLuma,
                        hasSubject = false,
                        compositionTip = "Brace arms against body to minimize handshake blur"
                    )
                }

                skinToneScore > 0.16f -> {
                    AiSceneResult(
                        category = SceneCategory.PORTRAIT,
                        confidence = 0.88f,
                        directorTip = "Portrait subject • Portrait Natural profile recommended",
                        recommendedProfile = FilmProfile.PORTRAIT_NATURAL,
                        averageLuminance = avgLuma,
                        hasSubject = true,
                        compositionTip = "Position eyes along upper third grid line"
                    )
                }

                highLumaRatio > 0.22f && lowLumaRatio > 0.25f -> {
                    AiSceneResult(
                        category = SceneCategory.HIGH_CONTRAST,
                        confidence = 0.84f,
                        directorTip = "High dynamic range • Tap focus on subject to balance highlights",
                        recommendedProfile = FilmProfile.CINEMATIC,
                        averageLuminance = avgLuma,
                        hasSubject = abs(centerAvgLuma - avgLuma) > 30f,
                        compositionTip = "Avoid direct lens flare for cleaner black contrast"
                    )
                }

                avgEdge > 26f && avgLuma in 70f..180f -> {
                    AiSceneResult(
                        category = SceneCategory.LANDSCAPE,
                        confidence = 0.82f,
                        directorTip = "Rich landscape details • Level horizon with composition guide",
                        recommendedProfile = FilmProfile.WARM_FILM,
                        averageLuminance = avgLuma,
                        hasSubject = false,
                        compositionTip = "Use leading lines and align horizon to bottom third"
                    )
                }

                centerAvgLuma > avgLuma + 20f && avgEdge > 18f -> {
                    AiSceneResult(
                        category = SceneCategory.FOOD_MACRO,
                        confidence = 0.78f,
                        directorTip = "Close-up subject • Soft Film gives smooth analog look",
                        recommendedProfile = FilmProfile.SOFT_FILM,
                        averageLuminance = avgLuma,
                        hasSubject = true,
                        compositionTip = "Keep 10-15cm minimum distance for optimal focal sharpness"
                    )
                }

                else -> {
                    AiSceneResult(
                        category = SceneCategory.BALANCED,
                        confidence = 0.85f,
                        directorTip = "Lighting balanced • Lumora Film Engine active",
                        recommendedProfile = FilmProfile.NATURAL,
                        averageLuminance = avgLuma,
                        hasSubject = false,
                        compositionTip = null
                    )
                }
            }

            onResult(sceneResult)

        } catch (e: Exception) {
            // Safe fallback if buffer is closed or unexpected frame format
        } finally {
            image.close()
        }
    }
}
