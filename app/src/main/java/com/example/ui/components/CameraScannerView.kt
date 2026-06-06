package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraScannerView(
    modifier: Modifier = Modifier,
    scannerType: String = "FACE", // "FACE" or "IRIS"
    isSuccess: Boolean = false,
    onScanFinished: (Boolean) -> Unit
) {
    var isScanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Animators
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_sweep")
    
    // Laser bar position
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep"
    )

    // Concentric reticle scale
    val rotateAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    // Interactive node glow scale in pixels
    val nodeGlow by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Holographic Reticle Scanning Area
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.8f))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val center = Offset(w / 2, h / 2)
                
                // Draw camera corner brackets
                val bracketLen = 20.dp.toPx()
                val bracketGap = 4.dp.toPx()
                val strokeW = 2.5.dp.toPx()
                val bracketColor = if (isSuccess) Color(0xFF10B981) else if (isScanning) Color(0xFF38BDF8) else Color(0xFF64748B)

                // Top-Left corner
                drawLine(bracketColor, Offset(bracketGap, bracketGap), Offset(bracketGap + bracketLen, bracketGap), strokeW)
                drawLine(bracketColor, Offset(bracketGap, bracketGap), Offset(bracketGap, bracketGap + bracketLen), strokeW)
                
                // Top-Right corner
                drawLine(bracketColor, Offset(w - bracketGap, bracketGap), Offset(w - bracketGap - bracketLen, bracketGap), strokeW)
                drawLine(bracketColor, Offset(w - bracketGap, bracketGap), Offset(w - bracketGap, bracketGap + bracketLen), strokeW)
                
                // Bottom-Left corner
                drawLine(bracketColor, Offset(bracketGap, h - bracketGap), Offset(bracketGap + bracketLen, h - bracketGap), strokeW)
                drawLine(bracketColor, Offset(bracketGap, h - bracketGap), Offset(bracketGap, h - bracketGap - bracketLen), strokeW)
                
                // Bottom-Right corner
                drawLine(bracketColor, Offset(w - bracketGap, h - bracketGap), Offset(w - bracketGap - bracketLen, h - bracketGap), strokeW)
                drawLine(bracketColor, Offset(w - bracketGap, h - bracketGap), Offset(w - bracketGap, h - bracketGap - bracketLen), strokeW)

                // Draw circular target reticle
                drawCircle(
                    color = bracketColor.copy(alpha = 0.1f),
                    radius = w * 0.35f,
                    center = center
                )
                drawCircle(
                    color = bracketColor.copy(alpha = 0.3f),
                    radius = w * 0.35f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
                
                // Moving parts / scan reticle indicators
                drawCircle(
                    color = bracketColor.copy(alpha = 0.7f),
                    radius = w * 0.25f,
                    center = center,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(15f, 15f),
                            rotateAnimation
                        )
                    )
                )

                // Custom holographic facial points (FACE) or concentric details (IRIS)
                if (scannerType == "FACE") {
                    // Coordinates mapping of facial points (Eye clusters, nose bridge, jawline, forehead)
                    val facePoints = listOf(
                        Offset(w * 0.35f, h * 0.38f), // Left eye
                        Offset(w * 0.65f, h * 0.38f), // Right eye
                        Offset(w * 0.50f, h * 0.50f), // Nose bridge
                        Offset(w * 0.50f, h * 0.58f), // Nose tip
                        Offset(w * 0.40f, h * 0.68f), // Left mouth corner
                        Offset(w * 0.60f, h * 0.68f), // Right mouth corner
                        Offset(w * 0.50f, h * 0.74f), // Chin
                        Offset(w * 0.28f, h * 0.55f), // Left cheek
                        Offset(w * 0.72f, h * 0.55f), // Right cheek
                        Offset(w * 0.50f, h * 0.28f)  // Forehead
                    )

                    // Draw connecting lines (Holographic mask grid)
                    if (isScanning || isSuccess) {
                        val gridAlpha = if (isSuccess) 0.5f else 0.25f
                        val edgeColor = if (isSuccess) Color(0xFF10B981) else Color(0xFF0EA5E9)

                        // Draw lines between feature pairs
                        drawLine(edgeColor.copy(alpha = gridAlpha), facePoints[0], facePoints[1], 1.dp.toPx()) // eye connecting
                        drawLine(edgeColor.copy(alpha = gridAlpha), facePoints[0], facePoints[2], 1.dp.toPx())
                        drawLine(edgeColor.copy(alpha = gridAlpha), facePoints[1], facePoints[2], 1.dp.toPx())
                        drawLine(edgeColor.copy(alpha = gridAlpha), facePoints[2], facePoints[3], 1.dp.toPx())
                        drawLine(edgeColor.copy(alpha = gridAlpha), facePoints[3], facePoints[4], 1.dp.toPx())
                        drawLine(edgeColor.copy(alpha = gridAlpha), facePoints[3], facePoints[5], 1.dp.toPx())
                        drawLine(edgeColor.copy(alpha = gridAlpha), facePoints[4], facePoints[6], 1.dp.toPx())
                        drawLine(edgeColor.copy(alpha = gridAlpha), facePoints[5], facePoints[6], 1.dp.toPx())
                        drawLine(edgeColor.copy(alpha = gridAlpha), facePoints[7], facePoints[0], 1.dp.toPx())
                        drawLine(edgeColor.copy(alpha = gridAlpha), facePoints[8], facePoints[1], 1.dp.toPx())
                        drawLine(edgeColor.copy(alpha = gridAlpha), facePoints[7], facePoints[6], 1.dp.toPx())
                        drawLine(edgeColor.copy(alpha = gridAlpha), facePoints[8], facePoints[6], 1.dp.toPx())

                        // Highlight dots
                        facePoints.forEach { pt ->
                            drawCircle(
                                color = if (isSuccess) Color(0xFF34D399) else Color(0xFF38BDF8),
                                radius = nodeGlow,
                                center = pt
                            )
                        }
                    }
                } else {
                    // IRIS SCAN - Draw concentric patterns
                    val patterns = 3
                    for (i in 1..patterns) {
                        drawCircle(
                            color = bracketColor.copy(alpha = 0.4f / i),
                            radius = w * 0.15f * i,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                    if (isScanning || isSuccess) {
                        // Scan details (iris coordinates)
                        val angleCount = 12
                        val irisColor = if (isSuccess) Color(0xFF10B981) else Color(0xFF06B6D4)
                        for (a in 0 until angleCount) {
                            val angleRad = (a * (360f / angleCount) + rotateAnimation) * (Math.PI / 180f).toFloat()
                            val lineStart = Offset(
                                center.x + (w * 0.08f) * kotlin.math.cos(angleRad),
                                center.y + (w * 0.08f) * kotlin.math.sin(angleRad)
                            )
                            val lineEnd = Offset(
                                center.x + (w * 0.22f) * kotlin.math.cos(angleRad),
                                center.y + (w * 0.22f) * kotlin.math.sin(angleRad)
                            )
                            drawLine(
                                color = irisColor.copy(alpha = 0.6f),
                                start = lineStart,
                                end = lineEnd,
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                    }
                }

                // Laser Scanning Sweep Line
                if (isScanning) {
                    val laserY = h * sweepProgress
                    drawLine(
                        color = bracketColor.copy(alpha = 0.8f),
                        start = Offset(6.dp.toPx(), laserY),
                        end = Offset(w - 6.dp.toPx(), laserY),
                        strokeWidth = 2.5.dp.toPx()
                    )
                    // Glow under laser line
                    drawRect(
                        color = bracketColor.copy(alpha = 0.08f),
                        topLeft = Offset(6.dp.toPx(), if (sweepProgress > 0.5f) laserY - 30.dp.toPx() else laserY),
                        size = androidx.compose.ui.geometry.Size(
                            width = w - 12.dp.toPx(),
                            height = 30.dp.toPx()
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info / Metrics Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B))
                .padding(12.dp)
        ) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isSuccess) "الحالة: تم مطابقة البيانات"
                           else if (isScanning) "جاري البحث والمطابقة..."
                           else "اضغط لبدء المسح البيومتري",
                    color = if (isSuccess) Color(0xFF10B981) else if (isScanning) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
                if (isScanning || isSuccess) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (scannerType == "FACE") "نقاط التحقق النشطة: 124 نقطة" else "هيكل القزحية: 256 متجه ميزات",
                        color = Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "الدقة المقاصة: 99.87%",
                        color = Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scan Trigger Button
        Button(
            onClick = {
                if (!isScanning && !isSuccess) {
                    scope.launch {
                        isScanning = true
                        delay(2500) // Simulation timing
                        isScanning = false
                        onScanFinished(true)
                    }
                }
            },
            enabled = !isScanning && !isSuccess,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSuccess) Color(0xFF10B981) else Color(0xFF0284C7),
                disabledContainerColor = if (isSuccess) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF38BDF8).copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(
                text = if (isSuccess) "مكتمل ✓"
                       else if (isScanning) "جاري المسح الضوئي..."
                       else if (scannerType == "FACE") "ابدأ مسح الوجه"
                       else "ابدأ مسح العين",
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
