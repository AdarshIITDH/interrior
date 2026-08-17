package com.example.ui.components

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.model.FinishType
import com.example.model.WardrobeConfig
import com.example.spatial.ARPlacementState
import com.example.spatial.DeviceOrientationState
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Screen 7: Finish Selection Screen
 * Top [← Finish  ☰], 6 Circular Swatches in 2 rows (Walnut, Oak, Teak / White, Graphite, Beige),
 * and full-width cyan [ Apply ] button.
 */
@Composable
fun FinishSelectionScreen(
    currentConfig: WardrobeConfig,
    placement: ARPlacementState,
    deviceOrientation: DeviceOrientationState,
    onSelectFinish: (FinishType) -> Unit,
    onBack: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val row1 = listOf(FinishType.WALNUT, FinishType.OAK, FinishType.TEAK)
    val row2 = listOf(FinishType.WHITE, FinishType.GRAPHITE, FinishType.BEIGE)

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("finish_selection_screen")
    ) {
        // 1. 3D AR Wardrobe (Doors closed, showing finish)
        ARViewport(
            config = currentConfig,
            placement = placement.copy(showDimensions = false),
            deviceOrientation = deviceOrientation,
            onRotateYaw = {},
            onTranslate = { _, _ -> },
            onTap = {},
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Header Bar: [←  Finish  ☰]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x88000000))
                        .testTag("finish_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Finish",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x88000000))
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = TextPrimary
                )
            }
        }

        // 3. Bottom Controls: 6 Swatches in 2 Rows of 3 + [ Apply ] Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row 1 Swatches: Walnut, Oak, Teak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row1.forEach { finish ->
                    FinishSwatchCircle(
                        finish = finish,
                        isSelected = currentConfig.finish == finish,
                        onSelect = { onSelectFinish(finish) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 2 Swatches: White, Graphite, Beige
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row2.forEach { finish ->
                    FinishSwatchCircle(
                        finish = finish,
                        isSelected = currentConfig.finish == finish,
                        onSelect = { onSelectFinish(finish) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Apply Button (Cyan Full-Width Pill)
            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("apply_finish_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text(
                    text = "Apply",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FinishSwatchCircle(
    finish: FinishType,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onSelect() }
            .testTag("swatch_${finish.name.lowercase()}")
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(finish.primaryColor)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) CyanNeon else Color(0x55FFFFFF),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = if (finish == FinishType.WHITE || finish == FinishType.BEIGE) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = finish.title,
            color = if (isSelected) CyanNeon else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
