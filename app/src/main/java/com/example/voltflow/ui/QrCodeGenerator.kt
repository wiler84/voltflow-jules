package com.example.voltflow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.voltflow.ui.VoltflowDesign
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeGenerator {
    fun generatePrepaidToken(transactionId: String): String {
        val hash = transactionId.hashCode().toLong().let { if (it < 0) -it else it }
        val random = java.util.Random(hash)
        val sb = java.lang.StringBuilder()
        for (i in 0 until 5) {
            val block = 1000 + random.nextInt(9000)
            sb.append(block)
            if (i < 4) sb.append("-")
        }
        return sb.toString()
    }
}

@Composable
fun QrCodeView(
    token: String,
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.onSurface,
    backgroundColor: Color = Color.Transparent
) {
    val bitMatrix = remember(token) {
        try {
            val writer = QRCodeWriter()
            writer.encode(token, BarcodeFormat.QR_CODE, 512, 512)
        } catch (e: Exception) {
            null
        }
    }

    if (bitMatrix != null) {
        Canvas(modifier = modifier) {
            if (backgroundColor != Color.Transparent) {
                drawRect(color = backgroundColor)
            }
            val size = bitMatrix.width
            val cellSize = this.size.width / size
            for (x in 0 until size) {
                for (y in 0 until size) {
                    if (bitMatrix.get(x, y)) {
                        drawRect(
                            color = tintColor,
                            topLeft = androidx.compose.ui.geometry.Offset(x * cellSize, y * cellSize),
                            size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                        )
                    }
                }
            }
        }
    } else {
        Box(modifier = modifier.background(VoltflowDesign.IconCircleBg))
    }
}
