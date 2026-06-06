package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun VoiceAuthenticatorView(
    modifier: Modifier = Modifier,
    isSuccess: Boolean = false,
    passphrase: String = "افتح تطبيقي الخاص الآمن بمحاكي التحقق البيومتري",
    onVoiceMatch: (Boolean) -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var matchRatio by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    // Waveform continuous phase animation during recording
    val phaseTransition = rememberInfiniteTransition(label = "waveform_phase")
    val phaseShift by phaseTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Waveform height multiplier (dynamic amplitude simulation)
    val amplitudeTransition = rememberInfiniteTransition(label = "waveform_amp")
    val pulseAmp by amplitudeTransition.animateFloat(
        initialValue = 10f,
        targetValue = 45f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutBounce),
            repeatMode = RepeatMode.Reverse
        ),
        label = "amplitude"
    )

    val progressAnim = remember { Animatable(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Passphrase display box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "الجملة الآمنة لتأكيد الهوية:",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "“$passphrase”",
                    color = Color(0xFFE2E8F0),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Beautiful visual dynamic audio waveforms canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isRecording) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val midY = height / 2f
                    
                    // Drawing 3 overlays of mathematical sine waves with varied phasing & speeds
                    val waveSpacing = 0.05f
                    val wavePoints = (width.toInt() / 4)
                    
                    // Wave 1: Primary cyan wave
                    val path1 = androidx.compose.ui.graphics.Path()
                    path1.moveTo(0f, midY)
                    for (i in 0..wavePoints) {
                        val x = i * (width / wavePoints)
                        val angle = (i * waveSpacing) + phaseShift
                        val y = midY + sin(angle) * pulseAmp
                        path1.lineTo(x, y)
                    }
                    drawPath(
                        path = path1,
                        brush = Brush.horizontalGradient(listOf(Color(0xFF22D3EE), Color(0xFF0EA5E9))),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Wave 2: Secondary blue wave (opposite phase)
                    val path2 = androidx.compose.ui.graphics.Path()
                    path2.moveTo(0f, midY)
                    for (i in 0..wavePoints) {
                        val x = i * (width / wavePoints)
                        val angle = (i * waveSpacing) - phaseShift + 1.5f
                        val y = midY + sin(angle) * (pulseAmp * 0.7f)
                        path2.lineTo(x, y)
                    }
                    drawPath(
                        path = path2,
                        brush = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF3B82F6))),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Wave 3: Ambient green glow
                    val path3 = androidx.compose.ui.graphics.Path()
                    path3.moveTo(0f, midY)
                    for (i in 0..wavePoints) {
                        val x = i * (width / wavePoints)
                        val angle = (i * waveSpacing) + (phaseShift * 1.5f)
                        val y = midY + sin(angle) * (pulseAmp * 0.4f)
                        path3.lineTo(x, y)
                    }
                    drawPath(
                        path = path3,
                        color = Color(0xFF10B981).copy(alpha = 0.4f),
                        style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            } else if (isSuccess) {
                Text(
                    text = "✓ الصوت متطابق بنسبة 99.1% (مقبول)",
                    color = Color(0xFF10B981),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "اضغط على الميكروفون المضيء بالأسفل لتسجيل البصمة",
                    color = Color(0xFF475569),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large animated microphone sensor
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(
                    if (isSuccess) Color(0xFF10B981).copy(alpha = 0.2f)
                    else if (isRecording) Color(0xFFEF4444).copy(alpha = 0.15f)
                    else Color(0xFF1E293B)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Radial recording progress ring
            if (isRecording) {
                Canvas(modifier = Modifier.size(90.dp)) {
                    drawArc(
                        color = Color(0xFFEF4444),
                        startAngle = -90f,
                        sweepAngle = progressAnim.value * 360f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }

            IconButton(
                onClick = {
                    if (!isRecording && !isSuccess) {
                        scope.launch {
                            isRecording = true
                            progressAnim.animateTo(
                                targetValue = 1.0f,
                                animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
                            )
                            isRecording = false
                            matchRatio = 99.1f
                            onVoiceMatch(true)
                        }
                    }
                },
                enabled = !isRecording && !isSuccess,
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSuccess) Color(0xFF10B981)
                        else if (isRecording) Color(0xFFEF4444)
                        else Color(0xFF0EA5E9)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "مستشعر الصوت المطور",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // State Feedback Summary
        if (isRecording) {
            Text(
                text = "جاري التقاط الصوت ومستويات التردد الرقمية...",
                color = Color(0xFFEF4444),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        } else if (isSuccess) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "معلومات الكشف الفريدة:",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "نموذج صوت MFCC مدمج | التردد الأساسي: 175Hz",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
