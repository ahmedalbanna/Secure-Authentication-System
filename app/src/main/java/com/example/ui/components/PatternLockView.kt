package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

@Composable
fun PatternLockView(
    modifier: Modifier = Modifier,
    nodeColor: Color = Color(0xFF64748B),         // slate grey
    activeColor: Color = Color(0xFF10B981),       // success emerald green
    errorColor: Color = Color(0xFFEF4444),        // error crimson red
    isError: Boolean = false,
    onPatternComplete: (List<Int>) -> Unit
) {
    var selectedPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }
    
    // Grid geometry computed dynamically
    var sizePx by remember { mutableStateOf(0f) }
    
    // We represent a 3x3 pattern grid
    val rowCount = 3
    val columnCount = 3
    val totalNodes = rowCount * columnCount
    
    Box(
        modifier = modifier
            .background(Color(0xFF0F172A).copy(alpha = 0.6f)) // deep obsidian back
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isError) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Resets on new drag
                            selectedPattern = emptyList()
                            currentTouchPoint = offset
                            
                            // Find nearest node
                            val nodeIdx = findNearestNode(offset, sizePx, rowCount, columnCount)
                            if (nodeIdx != -1) {
                                selectedPattern = listOf(nodeIdx)
                            }
                        },
                        onDragEnd = {
                            if (selectedPattern.isNotEmpty()) {
                                onPatternComplete(selectedPattern)
                            }
                            currentTouchPoint = null
                            selectedPattern = emptyList()
                        },
                        onDragCancel = {
                            currentTouchPoint = null
                            selectedPattern = emptyList()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val nextPoint = (currentTouchPoint ?: change.position) + dragAmount
                            currentTouchPoint = nextPoint
                            
                            val nodeIdx = findNearestNode(nextPoint, sizePx, rowCount, columnCount)
                            if (nodeIdx != -1 && !selectedPattern.contains(nodeIdx)) {
                                selectedPattern = selectedPattern + nodeIdx
                            }
                        }
                    )
                }
        ) {
            sizePx = size.width
            val step = sizePx / 4
            
            // Map indexes 0..8 to columns/rows
            val nodeOffsets = (0 until totalNodes).map { idx ->
                val col = idx % columnCount
                val row = idx / rowCount
                Offset((col + 1) * step, (row + 1) * step)
            }
            
            // Draw lines between selected nodes
            val strokeColor = if (isError) errorColor else activeColor
            if (selectedPattern.isNotEmpty()) {
                for (i in 0 until selectedPattern.size - 1) {
                    val startOffset = nodeOffsets[selectedPattern[i]]
                    val endOffset = nodeOffsets[selectedPattern[i + 1]]
                    drawLine(
                        color = strokeColor,
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                
                // Draw line to current touch point
                val lastOffset = nodeOffsets[selectedPattern.last()]
                currentTouchPoint?.let { touch ->
                    drawLine(
                        color = strokeColor.copy(alpha = 0.5f),
                        start = lastOffset,
                        end = touch,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            
            // Draw the 9 nodes
            nodeOffsets.forEachIndexed { idx, offset ->
                val isSelected = selectedPattern.contains(idx)
                val color = if (isSelected) strokeColor else nodeColor
                
                // Draw outer ripple circle if selected
                if (isSelected) {
                    drawCircle(
                        color = color.copy(alpha = 0.2f),
                        radius = 28.dp.toPx(),
                        center = offset
                    )
                    drawCircle(
                        color = color,
                        radius = 24.dp.toPx(),
                        center = offset,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                
                // Inner solid core
                drawCircle(
                    color = color,
                    radius = if (isSelected) 10.dp.toPx() else 8.dp.toPx(),
                    center = offset
                )
            }
        }
    }
}

// Find nearest node safely
private fun findNearestNode(point: Offset, sizePx: Float, rows: Int, cols: Int): Int {
    if (sizePx <= 0f) return -1
    val step = sizePx / 4
    val maxSelectDistance = step * 0.45f // activation zone radius
    
    for (idx in 0 until (rows * cols)) {
        val col = idx % cols
        val row = idx / rows
        val nodeOffset = Offset((col + 1) * step, (row + 1) * step)
        
        val dist = sqrt((point.x - nodeOffset.x) * (point.x - nodeOffset.x) + (point.y - nodeOffset.y) * (point.y - nodeOffset.y))
        if (dist <= maxSelectDistance) {
            return idx
        }
    }
    return -1
}
