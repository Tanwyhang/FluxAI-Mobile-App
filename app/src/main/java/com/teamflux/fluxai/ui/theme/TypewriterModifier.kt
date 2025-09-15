package com.teamflux.fluxai.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.delay

/**
 * Typewriter effect as a Modifier extension. Supply empty text in the composable you attach this to.
 * Uses device monospace font and a larger default font size.
 */
@Composable
fun Modifier.typewriterEffect(
    fullText: String,
    delayMillis: Long = 40,
    onFinished: (() -> Unit)? = null,
    fontSize: TextUnit? = null,
    style: TextStyle? = null
): Modifier {
    var displayedText by remember(fullText) { mutableStateOf("") }
    LaunchedEffect(fullText) {
        displayedText = ""
        fullText.forEachIndexed { i, _ ->
            displayedText = fullText.substring(0, i + 1)
            delay(delayMillis)
        }
        onFinished?.invoke()
    }
    val density = LocalDensity.current
    val contentColor: Color = LocalContentColor.current
    // Use a larger default font size (no 20% reduction)
    val resolvedFontSize = (fontSize ?: style?.fontSize ?: 18.sp)
    val resolvedWeight = style?.fontWeight ?: FontWeight.Normal

    return this.then(
        Modifier.drawWithContent {
            // Do NOT call drawContent() here to prevent the Text composable from drawing its own font.
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = contentColor.toArgb()
                textSize = with(density) { resolvedFontSize.toPx() }
                typeface = android.graphics.Typeface.MONOSPACE
                isFakeBoldText = resolvedWeight >= FontWeight.Bold
            }

            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    displayedText,
                    0f,
                    size.height / 2 + paint.textSize / 4, // center vertically with baseline offset
                    paint
                )
            }
        }
    )
}
