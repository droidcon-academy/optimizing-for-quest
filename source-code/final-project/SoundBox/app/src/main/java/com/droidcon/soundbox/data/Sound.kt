package com.droidcon.soundbox.data

import androidx.annotation.RawRes
import androidx.compose.ui.graphics.vector.ImageVector

data class Sound(
    val name: String,
    val icon: ImageVector,
    @RawRes val resourceId: Int? = null,
    val filePath: String? = null,
    val city: String = "Unknown City"
)
