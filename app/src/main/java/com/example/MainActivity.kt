package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.camera.ai.HorizonSensor
import com.example.camera.engine.LumoraCameraManager
import com.example.camera.model.CameraSettings
import com.example.ui.camera.CameraScreen
import com.example.ui.editor.PhotoEditorScreen
import com.example.ui.gallery.GalleryScreen
import com.example.ui.settings.SettingsSheet
import com.example.ui.theme.MyApplicationTheme

enum class ScreenState {
    CAMERA,
    GALLERY,
    EDITOR
}

class MainActivity : ComponentActivity() {

    private lateinit var cameraManager: LumoraCameraManager
    private lateinit var horizonSensor: HorizonSensor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        cameraManager = LumoraCameraManager(this)
        horizonSensor = HorizonSensor(this)

        setContent {
            MyApplicationTheme {
                LumoraApp(
                    cameraManager = cameraManager,
                    horizonSensor = horizonSensor
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.shutdown()
        horizonSensor.stop()
    }
}

@Composable
fun LumoraApp(
    cameraManager: LumoraCameraManager,
    horizonSensor: HorizonSensor
) {
    var currentScreen by remember { mutableStateOf(ScreenState.CAMERA) }
    var editingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(CameraSettings()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (currentScreen) {
            ScreenState.CAMERA -> {
                CameraScreen(
                    cameraManager = cameraManager,
                    horizonSensor = horizonSensor,
                    settings = settings,
                    onSettingsChanged = { updated ->
                        settings = updated
                        horizonSensor.hapticFeedbackEnabled = updated.hapticEnabled
                    },
                    onOpenGallery = { currentScreen = ScreenState.GALLERY },
                    onOpenSettings = { showSettings = true }
                )
            }
            ScreenState.GALLERY -> {
                GalleryScreen(
                    onNavigateBack = { currentScreen = ScreenState.CAMERA },
                    onEditPhoto = { uri ->
                        editingPhotoUri = uri
                        currentScreen = ScreenState.EDITOR
                    }
                )
            }
            ScreenState.EDITOR -> {
                editingPhotoUri?.let { uri ->
                    PhotoEditorScreen(
                        imageUri = uri,
                        onNavigateBack = { currentScreen = ScreenState.GALLERY },
                        onSaveSuccess = { currentScreen = ScreenState.GALLERY }
                    )
                } ?: run {
                    currentScreen = ScreenState.CAMERA
                }
            }
        }

        if (showSettings) {
            SettingsSheet(
                settings = settings,
                onSettingsChanged = { updated ->
                    settings = updated
                    horizonSensor.hapticFeedbackEnabled = updated.hapticEnabled
                },
                onDismiss = { showSettings = false }
            )
        }
    }
}
