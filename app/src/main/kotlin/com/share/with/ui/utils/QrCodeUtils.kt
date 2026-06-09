package com.share.with.ui.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import qrcode.QRCode

fun generateQrCodeBitmap(content: String): ImageBitmap? {
    return try {
        val qrCode = QRCode(content)
        val bitmap = qrCode.render().nativeImage() as Bitmap
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
