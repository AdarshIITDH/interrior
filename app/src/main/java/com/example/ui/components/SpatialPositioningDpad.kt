package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spatial.ARPlacementState
import com.example.ui.theme.CyanBorderActive
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun SpatialPositioningDpad(
    placement: ARPlacementState,
    onPanLeft: () -> Unit,
    onPanRight: () -> Unit,
    onMoveCloser: () -> Unit,
    onMoveFarther: () -> Unit,
    onElevateUp: () -> Unit,
    onElevateDown: () -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onSetAngle: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xE6141416)) // Translucent obsidian glass
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
            .padding(8.dp)
    ) {
        // Top Header Bar of Dpad
        Row(
            modifier = Modifier
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.OpenWith,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "POSITION & ROTATION",
                    color = TextPrimary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "${((placement.userRotationYDeg % 360 + 360) % 360).roundToInt()}°",
                color = CyanNeon,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Column(
                modifier = Modifier.padding(top = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Section 1: 4-Way Arrow Pad (Pan & Distance)
                Box(
                    modifier = Modifier
                        .size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Up (Move Farther)
                    DpadButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        contentDesc = "Move Farther",
                        testTag = "dpad_up",
                        onClick = onMoveFarther,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(32.dp)
                    )

                    // Down (Move Closer)
                    DpadButton(
                        icon = Icons.Default.KeyboardArrowDown,
                        contentDesc = "Move Closer",
                        testTag = "dpad_down",
                        onClick = onMoveCloser,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(32.dp)
                    )

                    // Left (Pan Left)
                    DpadButton(
                        icon = Icons.Default.KeyboardArrowLeft,
                        contentDesc = "Pan Left",
                        testTag = "dpad_left",
                        onClick = onPanLeft,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(32.dp)
                    )

                    // Right (Pan Right)
                    DpadButton(
                        icon = Icons.Default.KeyboardArrowRight,
                        contentDesc = "Pan Right",
                        testTag = "dpad_right",
                        onClick = onPanRight,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(32.dp)
                    )

                    // Center Reset Button
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(ObsidianSurfaceVariant)
                            .border(1.dp, GlassBorderSubtle, CircleShape)
                            .clickable(onClick = onReset)
                            .testTag("dpad_center_reset"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Center",
                            tint = CyanNeon,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Section 2: Step Rotation Controls (↶ -15° and ↷ +15°)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rotate Left Button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianSurfaceVariant)
                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(8.dp))
                            .clickable(onClick = onRotateLeft)
                            .padding(horizontal = 6.dp)
                            .testTag("btn_rotate_left_15"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateLeft,
                            contentDescription = "Rotate Left 15°",
                            tint = CyanNeon,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "↶ -15°", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Rotate Right Button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianSurfaceVariant)
                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(8.dp))
                            .clickable(onClick = onRotateRight)
                            .padding(horizontal = 6.dp)
                            .testTag("btn_rotate_right_15"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "+15° ↷", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate Right 15°",
                            tint = CyanNeon,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Section 3: Preset Orientation Snap Pill Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        "Front" to 0f,
                        "3/4 Iso" to 45f,
                        "Side" to 90f,
                        "Back" to 180f
                    ).forEach { (label, deg) ->
                        val isSelected = ((placement.userRotationYDeg % 360 + 360) % 360).roundToInt() == deg.toInt()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CyanNeon else ObsidianSurfaceVariant)
                                .clickable { onSetAngle(deg) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .testTag("snap_angle_${deg.toInt()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else TextSecondary,
                                fontSize = 8.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DpadButton(
    icon: ImageVector,
    contentDesc: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianSurfaceVariant)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = CyanNeon,
            modifier = Modifier.size(20.dp)
        )
    }
}
