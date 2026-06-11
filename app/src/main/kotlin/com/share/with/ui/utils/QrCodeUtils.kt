package com.share.with.ui.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import qrcode.QRCode

fun generateQrCodeBitmap(
    content: String?,
    foregroundColor: Color = Color.Black,
    backgroundColor: Color = Color.White,
): ImageBitmap? {
    if (content == null) return null
    return try {
        val qrCode =
            QRCode.ofSquares()
                .withColor(foregroundColor.toArgb())
                .withBackgroundColor(backgroundColor.toArgb())
                .build(content)
        val bitmap = qrCode.render().nativeImage() as Bitmap
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
