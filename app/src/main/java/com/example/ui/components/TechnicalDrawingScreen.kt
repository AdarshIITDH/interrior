package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CutPanel
import com.example.model.WardrobeProject
import com.example.spatial.TechnicalDrawingEngine
import com.example.spatial.TechnicalDrawingViewType
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Screen: Architectural 2D Technical Drawing Studio.
 * Crisp CAD elevations with pan/zoom, dimension extension lines, and multi-format exports.
 */
@Composable
fun TechnicalDrawingScreen(
    project: WardrobeProject,
    onOpenShare: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedView by remember { mutableStateOf(TechnicalDrawingViewType.ALL_IN_ONE) }
    var isDarkCadTheme by remember { mutableStateOf(false) }

    // Pan & Zoom gestures
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 4.0f)
        offset += panChange
    }

    // Render bitmap on state change
    val drawingBitmap = remember(project, selectedView, isDarkCadTheme) {
        TechnicalDrawingEngine.generateDrawingBitmap(
            project = project,
            viewType = selectedView,
            widthPx = 2560,
            heightPx = 1440,
            isDarkTheme = isDarkCadTheme
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkCadTheme) Color(0xFF0F172A) else Color(0xFFE2E8F0))
            .testTag("technical_drawing_screen")
    ) {
        // Drawing Viewport Canvas with Smooth Pan & Zoom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformableState),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = drawingBitmap.asImageBitmap(),
                contentDescription = selectedView.title,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit
            )
        }

        // Top Navigation & Drawing Mode Selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC090D16))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }

                // Drawing No Badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xEE090D16))
                        .border(1.dp, CyanNeon, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${selectedView.drawingNumber} • ${selectedView.title}",
                        color = CyanNeon,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Theme Toggle & Reset Zoom
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC090D16))
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset Zoom", tint = TextPrimary)
                    }

                    IconButton(
                        onClick = { isDarkCadTheme = !isDarkCadTheme },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC090D16))
                    ) {
                        Icon(
                            imageVector = if (isDarkCadTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Theme",
                            tint = CyanNeon
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Architectural View Tabs: [ All ] [ Front ] [ Interior ] [ Plan ] [ Side ]
            ScrollableTabRow(
                selectedTabIndex = selectedView.ordinal,
                containerColor = Color(0xDD090D16),
                contentColor = CyanNeon,
                edgePadding = 8.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedView.ordinal]),
                        color = CyanNeon,
                        height = 2.5.dp
                    )
                },
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                TechnicalDrawingViewType.entries.forEach { viewType ->
                    Tab(
                        selected = selectedView == viewType,
                        onClick = { selectedView = viewType },
                        text = {
                            Text(
                                text = viewType.title,
                                color = if (selectedView == viewType) CyanNeon else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (selectedView == viewType) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }
        }

        // Bottom Action Bar: Export & Share
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    color = Color(0xF5090D16),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(project.formattedOverallDimensionsFtIn, color = CyanNeon, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Scale: N.T.S. • Precision Joinery", color = TextSecondary, fontSize = 11.sp)
            }

            Button(
                onClick = onOpenShare,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                modifier = Modifier.testTag("export_drawing_btn")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export DXF / PDF", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
