package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import com.example.model.UnitSystem
import com.example.model.WardrobeConfig
import com.example.spatial.ARPlacementState
import com.example.spatial.DeviceOrientationState
import com.example.spatial.SpatialRenderer

@Composable
fun ARViewport(
    config: WardrobeConfig,
    placement: ARPlacementState,
    deviceOrientation: DeviceOrientationState,
    onRotateYaw: (Float) -> Unit,
    onTranslate: (deltaLateral: Float, deltaDistance: Float) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    unitSystem: UnitSystem = UnitSystem.FEET_INCHES
) {
    val textMeasurer = rememberTextMeasurer()

    val infiniteTransition = rememberInfiniteTransition(label = "ar_pulse")
    val animationTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anim_tick"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("ar_viewport")
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onTap()
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    if (rotation != 0f) {
                        onRotateYaw(-rotation)
                    }
                    if (pan.x != 0f || pan.y != 0f) {
                        val deltaLateral = (pan.x / 450f)
                        val deltaDistance = (-pan.y / 450f)
                        onTranslate(deltaLateral, deltaDistance)
                    }
                    if (zoom != 1.0f) {
                        val zoomDelta = (1.0f - zoom) * 1.5f
                        onTranslate(0f, zoomDelta)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            SpatialRenderer.renderWardrobeScene(
                drawScope = this,
                config = config,
                placement = placement,
                deviceOrientation = deviceOrientation,
                textMeasurer = textMeasurer,
                animationTick = animationTick,
                unitSystem = unitSystem
            )
        }
    }
}
