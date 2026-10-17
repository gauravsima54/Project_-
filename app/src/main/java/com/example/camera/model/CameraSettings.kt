package com.example.camera.model

enum class AspectRatioOption(val label: String, val ratioFloat: Float) {
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_1_1("1:1", 1f / 1f)
}

enum class FlashModeOption(val label: String) {
    AUTO("Auto"),
    ON("On"),
    OFF("Off")
}

enum class TimerOption(val label: String, val seconds: Int) {
    OFF("Off", 0),
    SEC_3("3s", 3),
    SEC_10("10s", 10)
}

enum class GridModeOption(val label: String) {
    OFF("Off"),
    RULE_OF_THIRDS("3x3 Grid"),
    GOLDEN_RATIO("Golden Ratio")
}

enum class QualityOption(val label: String, val compressionQuality: Int) {
    STANDARD("Standard (90%)", 90),
    HIGH("High (96%)", 96),
    MAX("Max Uncompressed (100%)", 100)
}

data class CameraSettings(
    val flashMode: FlashModeOption = FlashModeOption.AUTO,
    val timer: TimerOption = TimerOption.OFF,
    val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_4_3,
    val gridMode: GridModeOption = GridModeOption.RULE_OF_THIRDS,
    val levelerEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val quality: QualityOption = QualityOption.HIGH,
    val aiDirectorEnabled: Boolean = true,
    val defaultFilmProfile: FilmProfile = FilmProfile.NATURAL,
    val defaultMode: CameraMode = CameraMode.PHOTO,
    val saveLocation: Boolean = false
)
