package com.example.camera.model

/**
 * Lumora Film Engine - 8 distinct cinematic and film profiles.
 * Includes precise 4x5 ColorMatrix transforms for real-time GPU/Compose previews
 * and pixel-level pipeline processing.
 */
enum class FilmProfile(
    val id: String,
    val displayName: String,
    val tagline: String,
    val exposureMultiplier: Float, // 1.0 = normal
    val contrast: Float,          // 1.0 = normal, 1.2 = high
    val saturation: Float,        // 1.0 = normal, 0.0 = B&W
    val warmth: Float,            // Red/Blue shift (-1.0 to 1.0)
    val vignette: Float,          // 0.0 to 1.0
    val grain: Float              // 0.0 to 1.0
) {
    NATURAL(
        id = "natural",
        displayName = "Natural",
        tagline = "True-to-life color fidelity and balanced dynamic range",
        exposureMultiplier = 1.0f,
        contrast = 1.0f,
        saturation = 1.0f,
        warmth = 0.0f,
        vignette = 0.0f,
        grain = 0.0f
    ),

    SOFT_FILM(
        id = "soft_film",
        displayName = "Soft Film",
        tagline = "Creamy pastels, lifted shadow blacks, and gentle highlights",
        exposureMultiplier = 1.05f,
        contrast = 0.88f,
        saturation = 0.92f,
        warmth = 0.12f,
        vignette = 0.15f,
        grain = 0.18f
    ),

    WARM_FILM(
        id = "warm_film",
        displayName = "Warm Film",
        tagline = "Golden hour warmth, amber tones, inspired by vintage Kodachrome",
        exposureMultiplier = 1.02f,
        contrast = 1.10f,
        saturation = 1.15f,
        warmth = 0.35f,
        vignette = 0.22f,
        grain = 0.20f
    ),

    COOL_FILM(
        id = "cool_film",
        displayName = "Cool Film",
        tagline = "Cyan-tinted shadows and crisp daylight whites (Fuji style)",
        exposureMultiplier = 1.0f,
        contrast = 1.08f,
        saturation = 1.05f,
        warmth = -0.28f,
        vignette = 0.12f,
        grain = 0.15f
    ),

    CINEMATIC(
        id = "cinematic",
        displayName = "Cinematic",
        tagline = "Split-toned teal shadows and warm skin tone contrast",
        exposureMultiplier = 0.98f,
        contrast = 1.22f,
        saturation = 1.12f,
        warmth = 0.08f,
        vignette = 0.35f,
        grain = 0.25f
    ),

    CLASSIC(
        id = "classic",
        displayName = "Classic",
        tagline = "Panchromatic black & white with rich silver grain and deep blacks",
        exposureMultiplier = 1.0f,
        contrast = 1.25f,
        saturation = 0.0f, // B&W
        warmth = 0.0f,
        vignette = 0.25f,
        grain = 0.32f
    ),

    MOODY(
        id = "moody",
        displayName = "Moody",
        tagline = "Subdued chroma, dramatic shadows, and atmospheric vignette",
        exposureMultiplier = 0.92f,
        contrast = 1.28f,
        saturation = 0.82f,
        warmth = -0.10f,
        vignette = 0.45f,
        grain = 0.28f
    ),

    PORTRAIT_NATURAL(
        id = "portrait_natural",
        displayName = "Portrait Natural",
        tagline = "Optimized for radiant skin tones, soft roll-off, and subtle warmth",
        exposureMultiplier = 1.04f,
        contrast = 0.95f,
        saturation = 1.02f,
        warmth = 0.15f,
        vignette = 0.18f,
        grain = 0.10f
    );

    /**
     * Generates a 4x5 ColorMatrix array (20 floats) for Android ColorMatrix and Compose ColorFilter.
     */
    fun buildColorMatrix(): FloatArray {
        if (this == CLASSIC) {
            // Panchromatic B&W matrix with contrast boost
            val c = contrast
            val t = (1f - c) * 0.5f * 255f
            val lr = 0.299f * c
            val lg = 0.587f * c
            val lb = 0.114f * c
            return floatArrayOf(
                lr, lg, lb, 0f, t,
                lr, lg, lb, 0f, t,
                lr, lg, lb, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        }

        // Color adjustments
        val c = contrast
        val s = saturation
        val t = (1f - c) * 0.5f * 255f

        // Warmth adjusts red and blue channels
        val redGain = 1.0f + (warmth * 0.18f)
        val blueGain = 1.0f - (warmth * 0.18f)
        val greenGain = 1.0f

        // Standard saturation coefficients
        val rw = 0.213f * (1f - s)
        val gw = 0.715f * (1f - s)
        val bw = 0.072f * (1f - s)

        val r0 = (rw + s) * c * redGain * exposureMultiplier
        val r1 = gw * c * redGain * exposureMultiplier
        val r2 = bw * c * redGain * exposureMultiplier

        val g0 = rw * c * greenGain * exposureMultiplier
        val g1 = (gw + s) * c * greenGain * exposureMultiplier
        val g2 = bw * c * greenGain * exposureMultiplier

        val b0 = rw * c * blueGain * exposureMultiplier
        val b1 = gw * c * blueGain * exposureMultiplier
        val b2 = (bw + s) * c * blueGain * exposureMultiplier

        // Extra teal-orange split toning for CINEMATIC
        val extraBlueOffset = if (this == CINEMATIC) 8f else 0f
        val extraRedOffset = if (this == CINEMATIC) 12f else 0f

        return floatArrayOf(
            r0, r1, r2, 0f, t + extraRedOffset,
            g0, g1, g2, 0f, t,
            b0, b1, b2, 0f, t + extraBlueOffset,
            0f, 0f, 0f, 1f, 0f
        )
    }
}
