package com.example.camera.model

enum class CameraMode(val label: String, val description: String) {
    PHOTO("PHOTO", "High-resolution balanced capture with film profiling"),
    PORTRAIT("PORTRAIT", "Subject separation with simulated optical aperture"),
    CINEMATIC("CINEMATIC", "Wide 16:9 cinematic aspect ratio with teal-amber curve"),
    VIDEO("VIDEO", "Smooth high-bitrate video recording"),
    PRO("PRO", "Manual exposure compensation, ISO & shutter metering")
}
