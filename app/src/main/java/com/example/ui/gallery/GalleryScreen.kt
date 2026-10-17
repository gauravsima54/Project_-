package com.example.ui.gallery

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.camera.model.CapturedMedia
import com.example.ui.theme.LumoraFocusYellow
import com.example.ui.theme.LumoraGold
import com.example.ui.theme.LumoraObsidian
import com.example.ui.theme.LumoraShutterRed
import com.example.ui.theme.LumoraSurfaceDark
import com.example.ui.theme.LumoraWhite
import com.example.ui.theme.LumoraWhite50
import com.example.ui.theme.LumoraWhite80
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onNavigateBack: () -> Unit,
    onEditPhoto: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaList by remember { mutableStateOf<List<CapturedMedia>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedMedia by remember { mutableStateOf<CapturedMedia?>(null) }
    var showInfoSheet by remember { mutableStateOf(false) }

    BackHandler {
        if (selectedMedia != null) {
            selectedMedia = null
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            mediaList = loadLumoraMedia(context)
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LumoraObsidian)
    ) {
        if (selectedMedia != null) {
            // Fullscreen Single Photo View
            val currentMedia = selectedMedia!!
            FullscreenPhotoView(
                media = currentMedia,
                onBack = { selectedMedia = null },
                onEdit = { onEditPhoto(currentMedia.uri) },
                onShare = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, currentMedia.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Photo"))
                },
                onInfo = { showInfoSheet = true },
                onDelete = {
                    try {
                        context.contentResolver.delete(currentMedia.uri, null, null)
                        mediaList = mediaList.filter { it.uri != currentMedia.uri }
                        selectedMedia = null
                    } catch (_: Exception) {}
                }
            )

            if (showInfoSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showInfoSheet = false },
                    sheetState = rememberModalBottomSheetState(),
                    containerColor = LumoraSurfaceDark
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .navigationBarsPadding()
                    ) {
                        Text(
                            text = "Photo Information",
                            color = LumoraWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        InfoRow(label = "Filename", value = currentMedia.displayName)
                        InfoRow(label = "Date", value = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(currentMedia.timestamp)))
                        InfoRow(label = "Film Profile", value = currentMedia.filmProfileName)
                        InfoRow(label = "Storage", value = "Local MediaStore (100% On-Device)")
                        InfoRow(label = "Resolution", value = if (currentMedia.width > 0) "${currentMedia.width} x ${currentMedia.height}" else "Full Resolution")
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

        } else {
            // Grid View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .testTag("gallery_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LumoraWhite
                        )
                    }

                    Text(
                        text = "Lumora Gallery",
                        color = LumoraWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(modifier = Modifier.size(40.dp))
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LumoraGold)
                    }
                } else if (mediaList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = LumoraWhite50,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No photos captured yet",
                                color = LumoraWhite80,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the shutter to take your first cinematic photo",
                                color = LumoraWhite50,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .testTag("gallery_grid"),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(mediaList) { item ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(2.dp))
                                    .clickable { selectedMedia = item }
                                    .testTag("gallery_item_${item.displayName}")
                            ) {
                                AsyncImage(
                                    model = item.uri,
                                    contentDescription = item.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullscreenPhotoView(
    media: CapturedMedia,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main Image
        AsyncImage(
            model = media.uri,
            contentDescription = media.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("photo_view_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LumoraWhite
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onInfo,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = LumoraWhite
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("photo_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = LumoraShutterRed
                    )
                }
            }
        }

        // Bottom Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Share Button
            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .testTag("photo_share_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = LumoraWhite
                )
            }

            // Edit in Lumora Studio Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(LumoraGold)
                    .clickable { onEdit() }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag("photo_edit_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Edit Look",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = LumoraWhite50, fontSize = 13.sp)
        Text(text = value, color = LumoraWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

private fun loadLumoraMedia(context: Context): List<CapturedMedia> {
    val list = mutableListOf<CapturedMedia>()
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT
    )
    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

    try {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            var count = 0
            while (cursor.moveToNext() && count < 60) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "Photo"
                val date = cursor.getLong(dateCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                list.add(
                    CapturedMedia(
                        uri = contentUri,
                        displayName = name,
                        timestamp = date,
                        width = width,
                        height = height,
                        filmProfileName = if (name.startsWith("LUMORA")) "Lumora Film" else "Photo"
                    )
                )
                count++
            }
        }
    } catch (_: Exception) {}

    return list
}
