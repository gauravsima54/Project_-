package com.example.camera.model

enum class SceneCategory(val title: String, val icon: String) {
    BALANCED("Balanced", "✦"),
    PORTRAIT("Portrait", "👤"),
    LANDSCAPE("Landscape", "⛰️"),
    FOOD_MACRO("Food / Macro", "🍽️"),
    LOW_LIGHT("Low Light", "🌙"),
    HIGH_CONTRAST("Backlit / HDR", "☀️")
}

data class AiSceneResult(
    val category: SceneCategory = SceneCategory.BALANCED,
    val confidence: Float = 0.85f,
    val directorTip: String = "Scene balanced • Ready for capture",
    val recommendedProfile: FilmProfile? = null,
    val averageLuminance: Float = 128f,
    val hasSubject: Boolean = false,
    val compositionTip: String? = null
)
