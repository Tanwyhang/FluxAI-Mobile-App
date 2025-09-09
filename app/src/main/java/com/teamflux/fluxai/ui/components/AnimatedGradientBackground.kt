package com.teamflux.fluxai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import kotlin.math.*

@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier
) {
    // Use Compose's animation system with Bezier easing
    val infiniteTransition = rememberInfiniteTransition(label = "gradient_animation")
    val animationValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = CubicBezierEasing(0.1f, -1f, 0.6f, 1f)), // Bezier curve
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Calculate animated gradient parameters with Bezier-eased progress
        val progress = (sin(animationValue * PI.toFloat() * 2f) + 1f) / 2f // Consistent 0-1 oscillation
        val alternateProgress = (sin((animationValue + 0.5f) * PI.toFloat() * 2f) + 1f) / 2f // Phase-shifted by 0.5 for alternating

        // Keep radial gradient centered
        val centerX = width / 2f
        val centerY = height * 1.3f
        val radius = (width + height) * 1.3f

        // Secondary gradient: Contrasting effect with alternating phase
        val bg = Brush.radialGradient(
            0.0f to Color.Black.copy(alpha = 1f),
            1f to Color.Black.copy(alpha = 1f)
        )

        drawRect(
            brush = bg,
            size = size
        )

        // Violet Ring System - Secondary gradient with alternating phase
        val violetSecondaryGradient = Brush.radialGradient(
            0f to Color(0xFFECDAFF).copy(alpha = 0.1f + 0.0f * alternateProgress), // Phase-shifted violet
            0.28f to Color.Transparent, // Transparent instead of black
            0.35f to Color(0xFFA754FF).copy(alpha = 0.3f + 0.7f * alternateProgress), // Phase-shifted violet
            0.38f to Color.Transparent, // Transparent instead of black
            1f to Color.Transparent, // Transparent outer edge
            center = Offset(centerX, -height * 0.3f), // Same position as violet primary
            radius = radius * 0.8f // Slightly smaller for layering effect
        )

        drawRect(
            brush = violetSecondaryGradient,
            size = size
        )

        // White Ring System - Primary gradient (matches violet style but uses progress)
        val whitePrimaryGradient = Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.1f + 0.7f * progress), // Uses progress for pulsing
            0.28f to Color.Transparent,
            0.35f to Color.White.copy(alpha = 0.3f + 0.7f * progress), // Uses progress for pulsing
            0.36f to Color.Transparent,
            1f to Color.Transparent,
            center = Offset(centerX, height * 1.3f), // Different position from violet ring
            radius = radius * 0.8f
        )

        drawRect(
            brush = whitePrimaryGradient,
            size = size
        )
    }
}
