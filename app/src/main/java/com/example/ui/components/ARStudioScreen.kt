package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DimensionFormatter
import com.example.model.UnitSystem
import com.example.model.WardrobeConfig
import com.example.spatial.ARPlacementState
import com.example.spatial.DeviceOrientationState
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Screen 5: Adjust & Customize Screen
 * Matching user design: Top [☰ VisionSpace  ⛶], 3D Wardrobe in real room,
 * bottom pill toolbar [ Move ] [ Rotate ] [ Resize ], dimension pill.
 */
@Composable
fun ARStudioScreen(
    currentConfig: WardrobeConfig,
    placement: ARPlacementState,
    deviceOrientation: DeviceOrientationState,
    isZenMode: Boolean,
    isDpadVisible: Boolean,
    onRotateYaw: (Float) -> Unit,
    onTranslate: (Float, Float) -> Unit,
    onToggleDoors: () -> Unit,
    onOpenInterior: () -> Unit,
    onOpenFinish: () -> Unit,
    onOpenAutoFit: () -> Unit,
    onOpenSave: () -> Unit,
    onOpenShare: () -> Unit,
    onToggleDpad: () -> Unit,
    onToggleZenMode: () -> Unit,
    onOpenMenu: () -> Unit,
    onPanLeft: () -> Unit,
    onPanRight: () -> Unit,
    onMoveCloser: () -> Unit,
    onMoveFarther: () -> Unit,
    onElevateUp: () -> Unit,
    onElevateDown: () -> Unit,
    onRotateStepLeft: () -> Unit,
    onRotateStepRight: () -> Unit,
    onSetAngle: (Float) -> Unit,
    onResetPlacement: () -> Unit,
    modifier: Modifier = Modifier,
    unitSystem: UnitSystem = UnitSystem.FEET_INCHES,
    onOpenBOM: () -> Unit = {},
    onToggleUnit: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("ar_studio_screen")
    ) {
        // 1. 3D Wardrobe overlay in AR Viewport
        ARViewport(
            config = currentConfig,
            placement = placement,
            deviceOrientation = deviceOrientation,
            onRotateYaw = onRotateYaw,
            onTranslate = onTranslate,
            onTap = onToggleDoors,
            modifier = Modifier.fillMaxSize(),
            unitSystem = unitSystem
        )

        // 2. Top HUD Bar: [ ☰ VisionSpace ] on Left, [ ⛶ ] on Right
        if (!isZenMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hamburger + VisionSpace Title
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x88000000))
                        .clickable { onOpenMenu() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VisionSpace",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Top Right Action Buttons: Unit Switcher + Fullscreen
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Unit Switcher Badge (FT / IN / CM)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x88000000))
                            .border(1.dp, CyanNeon.copy(alpha = 0.5f), CircleShape)
                            .clickable { onToggleUnit() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("unit_switcher_badge")
                    ) {
                        Text(
                            text = unitSystem.shortLabel,
                            color = CyanNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Fullscreen toggle button
                    IconButton(
                        onClick = onToggleZenMode,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x88000000))
                            .testTag("fullscreen_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isZenMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Quick Floating Toolbar on Right side for fast access to Interior, Finish, AutoFit, BOM, Save, Share
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionFab(
                    label = "Interior",
                    onClick = onOpenInterior,
                    testTag = "fab_interior"
                )
                QuickActionFab(
                    label = "Finish",
                    onClick = onOpenFinish,
                    testTag = "fab_finish"
                )
                QuickActionFab(
                    label = "AutoFit",
                    onClick = onOpenAutoFit,
                    testTag = "fab_autofit"
                )
                QuickActionFab(
                    label = "BOM (₹)",
                    onClick = onOpenBOM,
                    testTag = "fab_bom"
                )
                QuickActionFab(
                    label = "Save",
                    onClick = onOpenSave,
                    testTag = "fab_save"
                )
                QuickActionFab(
                    label = "Share",
                    onClick = onOpenShare,
                    testTag = "fab_share"
                )
            }

            // Precision D-pad overlay if user tapped Move
            if (isDpadVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                ) {
                    SpatialPositioningDpad(
                        placement = placement,
                        onPanLeft = onPanLeft,
                        onPanRight = onPanRight,
                        onMoveCloser = onMoveCloser,
                        onMoveFarther = onMoveFarther,
                        onElevateUp = onElevateUp,
                        onElevateDown = onElevateDown,
                        onRotateLeft = onRotateStepLeft,
                        onRotateRight = onRotateStepRight,
                        onSetAngle = onSetAngle,
                        onReset = onResetPlacement
                    )
                }
            }

            // Bottom Toolbar & Dimension Pill (Matching Screen 5)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pill Toolbar with 3 Buttons: [ Move ] [ Rotate ] [ Resize ]
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xDD111827))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Move Tool Button
                        ToolbarToolItem(
                            icon = Icons.Default.OpenWith,
                            label = "Move",
                            isSelected = isDpadVisible,
                            onClick = onToggleDpad,
                            testTag = "tool_move"
                        )

                        // 2. Rotate Tool Button
                        ToolbarToolItem(
                            icon = Icons.Default.RotateRight,
                            label = "Rotate",
                            isSelected = false,
                            onClick = { onRotateYaw(15f) },
                            testTag = "tool_rotate"
                        )

                        // 3. Resize / AutoFit Tool Button
                        ToolbarToolItem(
                            icon = Icons.Default.FitScreen,
                            label = "Resize",
                            isSelected = false,
                            onClick = onOpenAutoFit,
                            testTag = "tool_resize"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dimension Pill
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xBB090D16))
                        .border(1.dp, Color(0x22FFFFFF), CircleShape)
                        .clickable { onOpenAutoFit() }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("dimensions_pill")
                ) {
                    Text(
                        text = DimensionFormatter.formatDimensions(
                            wCm = currentConfig.widthCm,
                            hCm = currentConfig.heightCm,
                            dCm = currentConfig.depthCm,
                            unitSystem = unitSystem,
                            compact = true
                        ),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Screen 9: Open Wardrobe Screen
 * Dedicated view with doors wide open and a clean [ Close ] button at bottom
 */
@Composable
fun OpenWardrobeScreen(
    currentConfig: WardrobeConfig,
    placement: ARPlacementState,
    deviceOrientation: DeviceOrientationState,
    onCloseDoors: () -> Unit,
    modifier: Modifier = Modifier,
    unitSystem: UnitSystem = UnitSystem.FEET_INCHES
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("open_wardrobe_screen")
    ) {
        // 3D Wardrobe overlay with doors open
        ARViewport(
            config = currentConfig.copy(doorOpenRatio = 1.0f),
            placement = placement.copy(showDimensions = false),
            deviceOrientation = deviceOrientation,
            onRotateYaw = {},
            onTranslate = { _, _ -> },
            onTap = onCloseDoors,
            modifier = Modifier.fillMaxSize(),
            unitSystem = unitSystem
        )

        // Bottom [ Close ] Pill Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            Button(
                onClick = onCloseDoors,
                modifier = Modifier
                    .width(140.dp)
                    .height(48.dp)
                    .testTag("close_doors_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
            ) {
                Text(
                    text = "Close",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ToolbarToolItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isSelected) CyanPrimary else Color(0x44FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.Black else TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) CyanNeon else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun QuickActionFab(
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xCC111827))
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
