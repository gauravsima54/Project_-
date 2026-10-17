package com.example.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.camera.ai.HorizonSensor
import com.example.camera.engine.LumoraCameraManager
import com.example.camera.model.AspectRatioOption
import com.example.camera.model.CameraMode
import com.example.camera.model.CameraSettings
import com.example.camera.model.CapturedMedia
import com.example.camera.model.FilmProfile
import com.example.camera.model.FlashModeOption
import com.example.camera.model.TimerOption
import com.example.ui.components.CameraTopBar
import com.example.ui.components.FilmProfileDock
import com.example.ui.components.FocusRing
import com.example.ui.components.GridOverlay
import com.example.ui.components.ModeSelector
import com.example.ui.components.PortraitControls
import com.example.ui.components.ProControlsHud
import com.example.ui.components.ShutterButton
import com.example.ui.components.ZoomSelector
import com.example.ui.theme.LumoraFocusYellow
import com.example.ui.theme.LumoraGold
import com.example.ui.theme.LumoraObsidian
import com.example.ui.theme.LumoraShutterRed
import com.example.ui.theme.LumoraWhite
import com.example.ui.theme.LumoraWhite50
import com.example.ui.theme.LumoraWhite80
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    cameraManager: LumoraCameraManager,
    horizonSensor: HorizonSensor,
    settings: CameraSettings,
    onSettingsChanged: (CameraSettings) -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Check camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionFallback(
            onGrantClick = {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                )
            }
        )
        return
    }

    // State management
    var currentMode by remember { mutableStateOf(settings.defaultMode) }
    var currentProfile by remember { mutableStateOf(settings.defaultFilmProfile) }
    var currentAperture by remember { mutableStateOf("f/2.8") }
    var evOffset by remember { mutableFloatStateOf(0f) }

    // Focus state
    var focusPosition by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // Flip animation
    var cameraFlipRotation by remember { mutableFloatStateOf(0f) }
    val animatedFlipRotation by animateFloatAsState(
        targetValue = cameraFlipRotation,
        animationSpec = spring(),
        label = "cameraFlip"
    )

    // Countdown timer state
    var countdownValue by remember { mutableIntStateOf(0) }
    var isCountingDown by remember { mutableStateOf(false) }

    // Flash animation effect
    val shutterFlashAlpha = remember { Animatable(0f) }

    // Camera flows
    val zoomState by cameraManager.zoomState.collectAsState()
    val isRecordingVideo by cameraManager.isRecordingVideo.collectAsState()
    val videoDurationSeconds by cameraManager.videoDurationSeconds.collectAsState()
    val isCapturing by cameraManager.isCapturing.collectAsState()
    val latestMedia by cameraManager.latestCapturedMedia.collectAsState()
    val aiSceneResult by cameraManager.aiSceneResult.collectAsState()

    // Sensor flows
    val rollAngle by horizonSensor.rollAngle.collectAsState()
    val isLevel by horizonSensor.isLevel.collectAsState()

    // Manage horizon sensor lifecycle
    DisposableEffect(Unit) {
        horizonSensor.hapticFeedbackEnabled = settings.hapticEnabled
        horizonSensor.start()
        onDispose {
            horizonSensor.stop()
        }
    }

    // Shutter Trigger Handler
    val executeCapture: () -> Unit = {
        if (currentMode == CameraMode.VIDEO) {
            if (isRecordingVideo) {
                cameraManager.stopVideoRecording()
            } else {
                if (!hasAudioPermission) {
                    permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                }
                cameraManager.startVideoRecording(
                    hasAudioPermission = hasAudioPermission,
                    onStarted = {},
                    onFinalized = { media ->
                        scope.launch {
                            snackbarHostState.showSnackbar("Video saved to gallery")
                        }
                    },
                    onError = { err ->
                        scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                    }
                )
            }
        } else {
            // Photo capture with screen flash simulation
            scope.launch {
                shutterFlashAlpha.snapTo(0.85f)
                shutterFlashAlpha.animateTo(0f, tween(180, easing = LinearEasing))
            }
            cameraManager.capturePhoto(
                filmProfile = currentProfile,
                evManualOffset = evOffset,
                quality = settings.quality,
                onPhotoProcessed = { media ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Photo saved (${currentProfile.displayName})")
                    }
                },
                onError = { err ->
                    scope.launch { snackbarHostState.showSnackbar("Capture error: $err") }
                }
            )
        }
    }

    val onShutterClicked: () -> Unit = {
        if (settings.timer != TimerOption.OFF && currentMode != CameraMode.VIDEO && !isCountingDown) {
            isCountingDown = true
            scope.launch {
                for (sec in settings.timer.seconds downTo 1) {
                    countdownValue = sec
                    delay(1000L)
                }
                isCountingDown = false
                countdownValue = 0
                executeCapture()
            }
        } else {
            executeCapture()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Live Camera Viewfinder with Aspect Ratio control
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val aspectModifier = when (settings.aspectRatio) {
                AspectRatioOption.RATIO_1_1 -> Modifier.aspectRatio(1f)
                AspectRatioOption.RATIO_16_9 -> Modifier.aspectRatio(9f / 16f)
                AspectRatioOption.RATIO_4_3 -> Modifier.aspectRatio(3f / 4f)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(aspectModifier)
                    .clip(RoundedCornerShape(0.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            focusPosition = Pair(offset.x, offset.y)
                            previewViewRef?.let { pView ->
                                val meteringPoint = pView.meteringPointFactory.createPoint(offset.x, offset.y)
                                cameraManager.tapToFocus(meteringPoint)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            val currentZoomRatio = zoomState?.zoomRatio ?: 1f
                            cameraManager.setZoomRatio(currentZoomRatio * zoom)
                        }
                    }
            ) {
                // Real Android CameraX Preview
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewViewRef = this
                            cameraManager.initCamera(
                                lifecycleOwner = lifecycleOwner,
                                previewView = this,
                                settings = settings,
                                mode = currentMode,
                                onError = { err ->
                                    scope.launch { snackbarHostState.showSnackbar(err) }
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Grid & Horizon Leveler Overlay
                GridOverlay(
                    gridMode = settings.gridMode,
                    levelerEnabled = settings.levelerEnabled,
                    rollAngle = rollAngle,
                    isLevel = isLevel
                )

                // Animated Tap-to-Focus Ring
                focusPosition?.let { pos ->
                    FocusRing(
                        x = pos.first,
                        y = pos.second,
                        exposureOffset = evOffset,
                        onExposureChanged = { ev ->
                            evOffset = ev
                            val index = (ev * 2).toInt()
                            cameraManager.setExposureIndex(index)
                        }
                    )
                }

                // Shutter flash animation layer
                if (shutterFlashAlpha.value > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = shutterFlashAlpha.value))
                    )
                }
            }
        }

        // 2. Countdown Timer Overlay
        if (isCountingDown && countdownValue > 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$countdownValue",
                    color = LumoraFocusYellow,
                    fontSize = 92.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        // 3. Top Bar (Flash, Timer, Aspect, AI Director, Settings)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .align(Alignment.TopCenter)
        ) {
            CameraTopBar(
                flashMode = settings.flashMode,
                onFlashToggle = {
                    val nextFlash = when (settings.flashMode) {
                        FlashModeOption.AUTO -> FlashModeOption.ON
                        FlashModeOption.ON -> FlashModeOption.OFF
                        FlashModeOption.OFF -> FlashModeOption.AUTO
                    }
                    val updated = settings.copy(flashMode = nextFlash)
                    onSettingsChanged(updated)
                    previewViewRef?.let {
                        cameraManager.bindCameraUseCases(lifecycleOwner, it, updated, currentMode)
                    }
                },
                aspectRatio = settings.aspectRatio,
                onAspectRatioToggle = {
                    val nextAspect = when (settings.aspectRatio) {
                        AspectRatioOption.RATIO_4_3 -> AspectRatioOption.RATIO_16_9
                        AspectRatioOption.RATIO_16_9 -> AspectRatioOption.RATIO_1_1
                        AspectRatioOption.RATIO_1_1 -> AspectRatioOption.RATIO_4_3
                    }
                    val updated = settings.copy(aspectRatio = nextAspect)
                    onSettingsChanged(updated)
                    previewViewRef?.let {
                        cameraManager.bindCameraUseCases(lifecycleOwner, it, updated, currentMode)
                    }
                },
                timer = settings.timer,
                onTimerToggle = {
                    val nextTimer = when (settings.timer) {
                        TimerOption.OFF -> TimerOption.SEC_3
                        TimerOption.SEC_3 -> TimerOption.SEC_10
                        TimerOption.SEC_10 -> TimerOption.OFF
                    }
                    onSettingsChanged(settings.copy(timer = nextTimer))
                },
                aiDirectorEnabled = settings.aiDirectorEnabled,
                aiSceneResult = aiSceneResult,
                onApplyAiRecommendation = {
                    aiSceneResult.recommendedProfile?.let {
                        currentProfile = it
                        scope.launch {
                            snackbarHostState.showSnackbar("Applied ${it.displayName}")
                        }
                    }
                },
                onSettingsClick = onOpenSettings
            )

            // Video Recording Indicator
            if (isRecordingVideo) {
                val mins = videoDurationSeconds / 60
                val secs = videoDurationSeconds % 60
                val timeFormatted = "%02d:%02d".format(mins, secs)

                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LumoraShutterRed.copy(alpha = 0.85f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = timeFormatted,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 4. Center-bottom Controls (Zoom, Pro HUD, Portrait HUD, Film Dock)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Contextual HUD based on mode
            when (currentMode) {
                CameraMode.PRO -> {
                    ProControlsHud(
                        exposureOffset = evOffset,
                        onExposureChanged = { ev ->
                            evOffset = ev
                            val index = (ev * 2).toInt()
                            cameraManager.setExposureIndex(index)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                CameraMode.PORTRAIT -> {
                    PortraitControls(
                        selectedAperture = currentAperture,
                        onApertureSelected = { currentAperture = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                else -> {}
            }

            // Zoom Selector
            zoomState?.let { state ->
                ZoomSelector(
                    currentZoom = state.zoomRatio,
                    minZoom = state.minZoomRatio,
                    maxZoom = state.maxZoomRatio,
                    onZoomSelected = { targetZoom ->
                        cameraManager.setZoomRatio(targetZoom)
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Film Profile Dock
            FilmProfileDock(
                selectedProfile = currentProfile,
                onProfileSelected = { currentProfile = it },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Mode Selector
            ModeSelector(
                currentMode = currentMode,
                onModeSelected = { newMode ->
                    currentMode = newMode
                    previewViewRef?.let {
                        cameraManager.bindCameraUseCases(lifecycleOwner, it, settings, newMode)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Shutter Row: Gallery Thumb, Large Shutter, Camera Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Thumbnail Button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { onOpenGallery() }
                        .testTag("gallery_thumbnail_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (latestMedia?.thumbnail != null) {
                        Image(
                            bitmap = latestMedia!!.thumbnail!!.asImageBitmap(),
                            contentDescription = "Recent Capture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "Gallery",
                            tint = LumoraWhite80,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Shutter Button
                ShutterButton(
                    cameraMode = currentMode,
                    isRecording = isRecordingVideo,
                    isCapturing = isCapturing,
                    onClick = onShutterClicked
                )

                // Camera Switch / Flip Button
                IconButton(
                    onClick = {
                        cameraFlipRotation += 180f
                        previewViewRef?.let {
                            cameraManager.switchCamera(lifecycleOwner, it, settings, currentMode)
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .testTag("camera_switch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = LumoraWhite,
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(animatedFlipRotation)
                    )
                }
            }
        }

        // Snackbar feedback
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        )
    }
}

@Composable
fun CameraPermissionFallback(
    onGrantClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LumoraObsidian)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = LumoraGold,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Lumora AI Camera",
                color = LumoraWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To capture cinematic photos and video with on-device film grading, Lumora requires camera and audio permissions. All media stays 100% on your device.",
                color = LumoraWhite50,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LumoraGold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("grant_camera_permission_button")
            ) {
                Text(
                    text = "Allow Camera Access",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
