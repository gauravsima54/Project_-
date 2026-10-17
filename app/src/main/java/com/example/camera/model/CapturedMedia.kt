package com.example.camera.model

import android.graphics.Bitmap
import android.net.Uri

data class CapturedMedia(
    val uri: Uri,
    val displayName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVideo: Boolean = false,
    val durationSeconds: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val filmProfileName: String = "Natural",
    val thumbnail: Bitmap? = null
)
