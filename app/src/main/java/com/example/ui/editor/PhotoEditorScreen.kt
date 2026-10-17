package com.example.ui.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.film.FilmEngine
import com.example.camera.model.FilmProfile
import com.example.ui.components.FilmProfileDock
import com.example.ui.theme.LumoraFocusYellow
import com.example.ui.theme.LumoraGold
import com.example.ui.theme.LumoraObsidian
import com.example.ui.theme.LumoraSurfaceDark
import com.example.ui.theme.LumoraWhite
import com.example.ui.theme.LumoraWhite50
import com.example.ui.theme.LumoraWhite80
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PhotoEditorScreen(
    imageUri: Uri,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isComparingOriginal by remember { mutableStateOf(false) }

    // Film profile & adjustment parameters
    var selectedProfile by remember { mutableStateOf(FilmProfile.NATURAL) }
    var exposureDelta by remember { mutableFloatStateOf(0f) }
    var contrastDelta by remember { mutableFloatStateOf(0f) }
    var saturationDelta by remember { mutableFloatStateOf(0f) }
    var warmthDelta by remember { mutableFloatStateOf(0f) }
    var vignetteStrength by remember { mutableFloatStateOf(0f) }
    var grainStrength by remember { mutableFloatStateOf(0f) }

    // Selected tab: 0 = Film Profile, 1 = Adjustments
    var activeTab by remember { mutableStateOf(0) }

    BackHandler {
        onNavigateBack()
    }

    // Load original bitmap
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            originalBitmap = loadSampledBitmap(context, imageUri, 1440)
            previewBitmap = originalBitmap
            isLoading = false
        }
    }

    // Recompute processed bitmap when settings change
    val recomputeFilter: () -> Unit = {
        val orig = originalBitmap
        if (orig != null) {
            scope.launch(Dispatchers.Default) {
                val processed = FilmEngine.processBitmap(
                    source = orig,
                    profile = selectedProfile,
                    evOffset = exposureDelta,
                    contrastDelta = contrastDelta,
                    saturationDelta = saturationDelta,
                    warmthDelta = warmthDelta,
                    vignetteStrength = vignetteStrength,
                    grainStrength = grainStrength
                )
                withContext(Dispatchers.Main) {
                    previewBitmap = processed
                }
            }
        }
    }

    // Rotate 90 degrees
    val rotateBitmap: () -> Unit = {
        val orig = originalBitmap
        if (orig != null) {
            val matrix = Matrix().apply { postRotate(90f) }
            val rotated = Bitmap.createBitmap(orig, 0, 0, orig.width, orig.height, matrix, true)
            originalBitmap = rotated
            recomputeFilter()
        }
    }

    // Reset adjustments
    val resetAdjustments: () -> Unit = {
        selectedProfile = FilmProfile.NATURAL
        exposureDelta = 0f
        contrastDelta = 0f
        saturationDelta = 0f
        warmthDelta = 0f
        vignetteStrength = 0f
        grainStrength = 0f
        previewBitmap = originalBitmap
    }

    // Save edited copy to MediaStore
    val saveCopy: () -> Unit = {
        val toSave = previewBitmap
        if (toSave != null) {
            isSaving = true
            scope.launch(Dispatchers.IO) {
                saveEditedPhoto(context, toSave, selectedProfile)
                withContext(Dispatchers.Main) {
                    isSaving = false
                    onSaveSuccess()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LumoraObsidian)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LumoraGold)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Editor Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .testTag("editor_cancel_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel",
                            tint = LumoraWhite
                        )
                    }

                    Text(
                        text = "Lumora Studio",
                        color = LumoraWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Save Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(LumoraGold)
                            .clickable(enabled = !isSaving) { saveCopy() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("editor_save_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Save Copy",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Main Image View with Before/After Hold-to-Compare
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val displayBitmap = if (isComparingOriginal) originalBitmap else previewBitmap
                    displayBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Edited Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Before / After Indicator overlay badge
                    if (isComparingOriginal) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .border(1.dp, LumoraFocusYellow, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ORIGINAL (HOLDING)",
                                color = LumoraFocusYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Quick Actions floating row (Rotate, Compare, Reset)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Compare Button (Press & Hold to see original)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isComparingOriginal = true
                                            tryAwaitRelease()
                                            isComparingOriginal = false
                                        }
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("editor_compare_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Compare,
                                    contentDescription = "Compare",
                                    tint = LumoraWhite80,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Hold Compare",
                                    color = LumoraWhite80,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Rotate 90 button
                        IconButton(
                            onClick = rotateBitmap,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .testTag("editor_rotate_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = "Rotate",
                                tint = LumoraWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Reset button
                        IconButton(
                            onClick = resetAdjustments,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .testTag("editor_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = LumoraWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Bottom Editing Deck
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LumoraSurfaceDark)
                        .padding(vertical = 8.dp)
                ) {
                    // Tab Header: FILMS vs FINE-TUNE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        EditorTabItem(
                            title = "FILM LOOKS",
                            isSelected = activeTab == 0,
                            onClick = { activeTab = 0 }
                        )
                        Spacer(modifier = Modifier.width(32.dp))
                        EditorTabItem(
                            title = "FINE-TUNE",
                            isSelected = activeTab == 1,
                            onClick = { activeTab = 1 }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (activeTab == 0) {
                        // Film Profile Dock
                        FilmProfileDock(
                            selectedProfile = selectedProfile,
                            onProfileSelected = { prof ->
                                selectedProfile = prof
                                recomputeFilter()
                            }
                        )
                    } else {
                        // Fine-tuning Sliders
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(horizontal = 20.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            AdjustmentSlider(
                                label = "Exposure",
                                value = exposureDelta,
                                range = -1f..1f,
                                onValueChange = {
                                    exposureDelta = it
                                    recomputeFilter()
                                }
                            )
                            AdjustmentSlider(
                                label = "Contrast",
                                value = contrastDelta,
                                range = -0.4f..0.4f,
                                onValueChange = {
                                    contrastDelta = it
                                    recomputeFilter()
                                }
                            )
                            AdjustmentSlider(
                                label = "Saturation",
                                value = saturationDelta,
                                range = -0.5f..0.5f,
                                onValueChange = {
                                    saturationDelta = it
                                    recomputeFilter()
                                }
                            )
                            AdjustmentSlider(
                                label = "Warmth",
                                value = warmthDelta,
                                range = -0.5f..0.5f,
                                onValueChange = {
                                    warmthDelta = it
                                    recomputeFilter()
                                }
                            )
                            AdjustmentSlider(
                                label = "Vignette",
                                value = vignetteStrength,
                                range = 0f..1f,
                                onValueChange = {
                                    vignetteStrength = it
                                    recomputeFilter()
                                }
                            )
                            AdjustmentSlider(
                                label = "Film Grain",
                                value = grainStrength,
                                range = 0f..0.8f,
                                onValueChange = {
                                    grainStrength = it
                                    recomputeFilter()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) LumoraFocusYellow else LumoraWhite50,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .background(if (isSelected) LumoraFocusYellow else Color.Transparent)
        )
    }
}

@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = LumoraWhite80,
            fontSize = 12.sp,
            modifier = Modifier.width(84.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = LumoraFocusYellow,
                activeTrackColor = LumoraFocusYellow,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
        Text(
            text = "%.2f".format(value),
            color = LumoraWhite50,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(38.dp)
        )
    }
}

private fun loadSampledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    var inputStream: InputStream? = null
    try {
        inputStream = context.contentResolver.openInputStream(uri)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        var sampleSize = 1
        var w = options.outWidth
        var h = options.outHeight
        while (w > maxDimension || h > maxDimension) {
            w /= 2
            h /= 2
            sampleSize *= 2
        }

        inputStream = context.contentResolver.openInputStream(uri)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeStream(inputStream, null, decodeOptions)
    } catch (_: Exception) {
        return null
    } finally {
        inputStream?.close()
    }
}

private fun saveEditedPhoto(context: Context, bitmap: Bitmap, profile: FilmProfile) {
    val filename = "LUMORA_EDIT_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.DESCRIPTION, "Edited with Lumora Studio - ${profile.displayName}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Lumora")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return

    resolver.openOutputStream(uri)?.use { stream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 96, stream)
        stream.flush()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
    }
}
