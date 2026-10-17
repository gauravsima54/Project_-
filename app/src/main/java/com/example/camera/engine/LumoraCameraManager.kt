package com.example.camera.engine

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.camera.ai.SceneAnalyzer
import com.example.camera.film.FilmEngine
import com.example.camera.model.AiSceneResult
import com.example.camera.model.AspectRatioOption
import com.example.camera.model.CameraMode
import com.example.camera.model.CameraSettings
import com.example.camera.model.CapturedMedia
import com.example.camera.model.FilmProfile
import com.example.camera.model.FlashModeOption
import com.example.camera.model.QualityOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LumoraCameraManager(private val context: Context) {

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val processingScope = CoroutineScope(Dispatchers.Default)

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var imageAnalysis: ImageAnalysis? = null

    private var isFrontFacing = false

    // State flows
    private val _zoomState = MutableStateFlow<ZoomState?>(null)
    val zoomState: StateFlow<ZoomState?> = _zoomState.asStateFlow()

    private val _exposureCompensationRange = MutableStateFlow(Pair(-4, 4))
    val exposureCompensationRange: StateFlow<Pair<Int, Int>> = _exposureCompensationRange.asStateFlow()

    private val _exposureIndex = MutableStateFlow(0)
    val exposureIndex: StateFlow<Int> = _exposureIndex.asStateFlow()

    private val _isRecordingVideo = MutableStateFlow(false)
    val isRecordingVideo: StateFlow<Boolean> = _isRecordingVideo.asStateFlow()

    private val _videoDurationSeconds = MutableStateFlow(0L)
    val videoDurationSeconds: StateFlow<Long> = _videoDurationSeconds.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _latestCapturedMedia = MutableStateFlow<CapturedMedia?>(null)
    val latestCapturedMedia: StateFlow<CapturedMedia?> = _latestCapturedMedia.asStateFlow()

    private val _aiSceneResult = MutableStateFlow(AiSceneResult())
    val aiSceneResult: StateFlow<AiSceneResult> = _aiSceneResult.asStateFlow()

    private val sceneAnalyzer = SceneAnalyzer { result ->
        _aiSceneResult.value = result
    }

    fun initCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        settings: CameraSettings,
        mode: CameraMode,
        onError: (String) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView, settings, mode)
            } catch (e: Exception) {
                Log.e("LumoraCamera", "Failed to init camera", e)
                onError(e.localizedMessage ?: "Camera initialization failed")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        settings: CameraSettings,
        mode: CameraMode
    ) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val cameraSelector = if (isFrontFacing) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val targetAspectRatio = when (settings.aspectRatio) {
            AspectRatioOption.RATIO_16_9 -> AspectRatio.RATIO_16_9
            else -> AspectRatio.RATIO_4_3
        }

        // 1. Preview
        preview = Preview.Builder()
            .setTargetAspectRatio(targetAspectRatio)
            .build()
            .also {
                it.surfaceProvider = previewView.surfaceProvider
            }

        // 2. ImageCapture
        val flashModeInt = when (settings.flashMode) {
            FlashModeOption.ON -> ImageCapture.FLASH_MODE_ON
            FlashModeOption.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashModeOption.OFF -> ImageCapture.FLASH_MODE_OFF
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashModeInt)
            .setTargetAspectRatio(targetAspectRatio)
            .build()

        // 3. VideoCapture
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        // 4. ImageAnalysis for AI Scene Director
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(targetAspectRatio)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, sceneAnalyzer)
            }

        try {
            camera = if (mode == CameraMode.VIDEO) {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture,
                    imageAnalysis
                )
            } else {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            }

            cameraControl = camera?.cameraControl
            cameraInfo = camera?.cameraInfo

            cameraInfo?.zoomState?.observe(lifecycleOwner) { state ->
                _zoomState.value = state
            }

            cameraInfo?.exposureState?.let { exp ->
                if (exp.isExposureCompensationSupported) {
                    _exposureCompensationRange.value = Pair(
                        exp.exposureCompensationRange.lower,
                        exp.exposureCompensationRange.upper
                    )
                    _exposureIndex.value = exp.exposureCompensationIndex
                }
            }

        } catch (e: Exception) {
            Log.e("LumoraCamera", "Use case binding failed", e)
        }
    }

    fun switchCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        settings: CameraSettings,
        mode: CameraMode
    ) {
        isFrontFacing = !isFrontFacing
        bindCameraUseCases(lifecycleOwner, previewView, settings, mode)
    }

    fun isUsingFrontCamera(): Boolean = isFrontFacing

    fun setZoomRatio(ratio: Float) {
        val state = _zoomState.value ?: return
        val clamped = ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio)
        cameraControl?.setZoomRatio(clamped)
    }

    fun setLinearZoom(linear: Float) {
        cameraControl?.setLinearZoom(linear.coerceIn(0f, 1f))
    }

    fun setExposureIndex(index: Int) {
        val range = _exposureCompensationRange.value
        val clamped = index.coerceIn(range.first, range.second)
        _exposureIndex.value = clamped
        cameraControl?.setExposureCompensationIndex(clamped)
    }

    fun tapToFocus(meteringPoint: MeteringPoint) {
        val action = FocusMeteringAction.Builder(
            meteringPoint,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        ).setAutoCancelDuration(4, TimeUnit.SECONDS).build()

        cameraControl?.startFocusAndMetering(action)
    }

    fun capturePhoto(
        filmProfile: FilmProfile,
        evManualOffset: Float,
        quality: QualityOption,
        onPhotoProcessed: (CapturedMedia) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError("Camera capture is not ready")
            return
        }

        _isCapturing.value = true

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    processingScope.launch {
                        try {
                            val bitmap = imageProxyToBitmap(image)
                            image.close()

                            if (bitmap == null) {
                                _isCapturing.value = false
                                onError("Failed to decode camera image")
                                return@launch
                            }

                            // Apply Lumora Film Engine processing
                            val processed = FilmEngine.processBitmap(
                                source = bitmap,
                                profile = filmProfile,
                                evOffset = evManualOffset
                            )

                            // Save to Android MediaStore
                            val savedMedia = saveBitmapToGallery(
                                bitmap = processed,
                                filmProfile = filmProfile,
                                quality = quality
                            )

                            _isCapturing.value = false
                            _latestCapturedMedia.value = savedMedia
                            onPhotoProcessed(savedMedia)

                            if (bitmap != processed) {
                                bitmap.recycle()
                            }
                        } catch (e: Exception) {
                            _isCapturing.value = false
                            Log.e("LumoraCamera", "Photo processing failed", e)
                            onError(e.localizedMessage ?: "Photo processing failed")
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    _isCapturing.value = false
                    Log.e("LumoraCamera", "Take picture error", exception)
                    onError(exception.localizedMessage ?: "Failed to capture image")
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun startVideoRecording(
        hasAudioPermission: Boolean,
        onStarted: () -> Unit,
        onFinalized: (CapturedMedia) -> Unit,
        onError: (String) -> Unit
    ) {
        val video = videoCapture ?: run {
            onError("Video capture not initialized")
            return
        }

        val name = "LUMORA_VID_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "$name.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Lumora")
            }
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        var pending = video.output.prepareRecording(context, outputOptions)
        if (hasAudioPermission) {
            try {
                pending = pending.withAudioEnabled()
            } catch (e: Exception) {
                Log.w("LumoraCamera", "Could not enable audio for recording", e)
            }
        }

        val recording = pending.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    _isRecordingVideo.value = true
                    _videoDurationSeconds.value = 0L
                    onStarted()
                }
                is VideoRecordEvent.Status -> {
                    val seconds = recordEvent.recordingStats.recordedDurationNanos / 1_000_000_000L
                    _videoDurationSeconds.value = seconds
                }
                is VideoRecordEvent.Finalize -> {
                    _isRecordingVideo.value = false
                    if (!recordEvent.hasError()) {
                        val outputUri = recordEvent.outputResults.outputUri
                        val media = CapturedMedia(
                            uri = outputUri,
                            displayName = "$name.mp4",
                            isVideo = true,
                            durationSeconds = _videoDurationSeconds.value
                        )
                        _latestCapturedMedia.value = media
                        onFinalized(media)
                    } else {
                        onError(recordEvent.cause?.localizedMessage ?: "Video recording failed")
                    }
                }
            }
        }
        activeRecording = recording
    }

    fun stopVideoRecording() {
        activeRecording?.stop()
        activeRecording = null
        _isRecordingVideo.value = false
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val rotationDegrees = image.imageInfo.rotationDegrees
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        if (rotationDegrees == 0 && !isFrontFacing) {
            return decoded
        }

        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        if (isFrontFacing) {
            matrix.postScale(-1f, 1f) // Mirror selfie
        }

        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated != decoded) {
            decoded.recycle()
        }
        return rotated
    }

    private fun saveBitmapToGallery(
        bitmap: Bitmap,
        filmProfile: FilmProfile,
        quality: QualityOption
    ): CapturedMedia {
        val filename = "LUMORA_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DESCRIPTION, "Captured with Lumora AI - ${filmProfile.displayName}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Lumora")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Could not create MediaStore entry")

        resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.compressionQuality, stream)
            stream.flush()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        // Generate downscaled thumbnail for UI
        val thumbSize = 160
        val thumbnail = Bitmap.createScaledBitmap(
            bitmap,
            thumbSize,
            (thumbSize * (bitmap.height.toFloat() / bitmap.width)).toInt(),
            true
        )

        return CapturedMedia(
            uri = uri,
            displayName = filename,
            width = bitmap.width,
            height = bitmap.height,
            filmProfileName = filmProfile.displayName,
            thumbnail = thumbnail
        )
    }

    fun shutdown() {
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}
