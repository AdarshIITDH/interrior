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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DoorStyle
import com.example.model.WardrobeConfig
import com.example.spatial.ARPlacementState
import com.example.ui.theme.CyanBorderActive
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldLaser
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TopHUDBar(
    isCameraActive: Boolean,
    isDpadVisible: Boolean,
    isZenMode: Boolean,
    statusMessage: String?,
    onToggleDpad: () -> Unit,
    onToggleZenMode: () -> Unit,
    onOpenHamburgerMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Main Streamlined Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sophisticated Brand Header with Live Telemetry
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Gradient Icon Emblem
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CyanNeon, ElectricBlue)
                            )
                        )
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                Text(
                    text = "VisionSpace",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )

                // Live AR Status Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isCameraActive) EmeraldLaser else CyanNeon)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCameraActive) "AR LIVE" else "3D STUDIO",
                        color = TextPrimary,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            // Right Actions: D-Pad Toggle, Zen Mode Toggle, and Floating Hamburger Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Arrow / D-Pad Toggle Button
                IconButton(
                    onClick = onToggleDpad,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isDpadVisible) ObsidianSurfaceVariant else Color(0xCC1A1A1A))
                        .border(1.dp, if (isDpadVisible) CyanBorderActive else GlassBorderSubtle, CircleShape)
                        .testTag("btn_top_toggle_dpad")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenWith,
                        contentDescription = "Toggle Arrow Controls",
                        tint = if (isDpadVisible) CyanNeon else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Zen AR Full-Screen Toggle Button
                IconButton(
                    onClick = onToggleZenMode,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isZenMode) CyanNeon else Color(0xCC1A1A1A))
                        .border(1.dp, if (isZenMode) CyanNeon else GlassBorderSubtle, CircleShape)
                        .testTag("btn_top_toggle_zen")
                ) {
                    Icon(
                        imageVector = if (isZenMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Zen Mode",
                        tint = if (isZenMode) Color.Black else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Hamburger Menu Icon Button
                IconButton(
                    onClick = onOpenHamburgerMenu,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(ObsidianSurfaceVariant)
                        .border(1.dp, CyanBorderActive, CircleShape)
                        .testTag("btn_top_hamburger_menu")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Menu Hub",
                        tint = CyanNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Real-time Status Toast Pill
        AnimatedVisibility(
            visible = !statusMessage.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ObsidianSurface.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanBorderActive)
                ) {
                    Text(
                        text = statusMessage ?: "",
                        color = CyanNeon,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingSpatialToolstrip(
    config: WardrobeConfig,
    placement: ARPlacementState,
    onToggleDoors: () -> Unit,
    onToggleDimensions: () -> Unit,
    onToggleFloorGrid: () -> Unit,
    onOpenBOM: () -> Unit,
    onToggleCustomizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Door Toggle
        if (config.doorStyle != DoorStyle.OPEN_CONCEPT) {
            CompactHUDIcon(
                icon = if (config.doorOpenRatio > 0.5f) Icons.Default.DoorBack else Icons.Default.DoorFront,
                isActive = config.doorOpenRatio > 0.5f,
                testTag = "btn_toggle_doors",
                onClick = onToggleDoors
            )
        }

        // Dimension Rulers Toggle
        CompactHUDIcon(
            icon = Icons.Default.Straighten,
            isActive = placement.showDimensions,
            testTag = "btn_toggle_dimensions",
            onClick = onToggleDimensions
        )

        // AR Grid Toggle
        CompactHUDIcon(
            icon = Icons.Default.Grid4x4,
            isActive = placement.showFloorGrid,
            testTag = "btn_toggle_grid",
            onClick = onToggleFloorGrid
        )

        // Live BOM Calculator Trigger
        CompactHUDIcon(
            icon = Icons.Default.ReceiptLong,
            isActive = false,
            testTag = "btn_open_bom_hud",
            onClick = onOpenBOM
        )

        // Customizer Drawer Trigger
        CompactHUDIcon(
            icon = Icons.Default.Tune,
            isActive = true,
            isPrimary = true,
            testTag = "btn_open_customizer",
            onClick = onToggleCustomizer
        )
    }
}

@Composable
private fun CompactHUDIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    testTag: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (isPrimary) CyanNeon else if (isActive) ObsidianSurfaceVariant else Color(0xD91A1A1A)
            )
            .border(
                1.dp,
                if (isPrimary) CyanNeon else if (isActive) CyanBorderActive else GlassBorderSubtle,
                CircleShape
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPrimary) Color.Black else if (isActive) CyanNeon else TextSecondary,
            modifier = Modifier.size(17.dp)
        )
    }
}
