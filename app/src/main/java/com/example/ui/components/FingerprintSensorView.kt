package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FingerprintSensorView(
    modifier: Modifier = Modifier,
    isScanningActive: Boolean = false,
    successState: Boolean = false,
    onFingerprintScan: (Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isPressing by remember { mutableStateOf(false) }
    
    // Constant pulsing animation when scanning or idle
    val infiniteTransition = rememberInfiniteTransition(label = "fingerprint_ripple")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    // Laser bar sliding animation when pressing
    val laserTransition = rememberInfiniteTransition(label = "laser_bar")
    val laserProgress by laserTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // Pulse effects behind the main button
        if (isPressing || isScanningActive) {
            Canvas(modifier = Modifier.size(180.dp)) {
                drawCircle(
                    color = (if (successState) Color(0xFF10B981) else Color(0xFF38BDF8)).copy(alpha = pulseAlpha),
                    radius = (size.width / 2) * pulseScale
                )
                drawCircle(
                    color = (if (successState) Color(0xFF10B981) else Color(0xFF38BDF8)).copy(alpha = pulseAlpha * 0.5f),
                    radius = (size.width / 3.4f) * pulseScale,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Central visual button
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    if (successState) Color(0xFF10B981).copy(alpha = 0.15f)
                    else if (isPressing) Color(0xFF0284C7).copy(alpha = 0.2f)
                    else Color(0xFF1E293B)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isPressing && !successState) {
                        coroutineScope.launch {
                            isPressing = true
                            delay(1800) // Simulate scanning
                            isPressing = false
                            onFingerprintScan(true)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Animated scanner bar
            if (isPressing) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val y = size.height * laserProgress
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }

            // Central icon
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "بصمة الإصبع",
                modifier = Modifier.size(64.dp),
                tint = if (successState) Color(0xFF10B981)
                       else if (isPressing) Color(0xFF38BDF8)
                       else Color(0xFF94A3B8)
            )
        }
    }
}
