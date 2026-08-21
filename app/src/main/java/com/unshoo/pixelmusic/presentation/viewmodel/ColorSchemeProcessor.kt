package com.unshoo.pixelmusic.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Trace
import android.util.LruCache
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.unshoo.pixelmusic.data.preferences.AlbumArtColorAccuracy
import com.unshoo.pixelmusic.data.preferences.AlbumArtPaletteStyle
import com.unshoo.pixelmusic.