package com.example.ui.components

import android.Manifest
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraView(
    onPermissionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var isCameraBound by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraProviderInstance?.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(cameraPermissionState.status) {
        onPermissionChanged(cameraPermissionState.status.isGranted)
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProviderInstance = cameraProvider
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                            isCameraBound = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Futuristic Architectural Studio Canvas Simulator (Active when camera not granted or in emulator)
            SimulatedStudioRoomBackground()
        }
    }
}

@Composable
fun SimulatedStudioRoomBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A0A),
                        Color(0xFF121212),
                        Color(0xFF0A0A0A)
                    )
                )
            )
    ) {
        // Perspective room wall & floor horizon guides with subtle dot matrix & wireframe
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizonY = h * 0.62f

            // Dot matrix grid on floor and backdrop
            val step = 32f
            var x = 0f
            while (x < w) {
                var y = 0f
                while (y < h) {
                    drawCircle(
                        color = Color(0x1FFFFFFF),
                        radius = 1.2f,
                        center = Offset(x, y)
                    )
                    y += step
                }
                x += step
            }

            // Wall/Floor boundary line
            drawLine(
                color = Color(0x3322D3EE),
                start = Offset(0f, horizonY),
                end = Offset(w, horizonY),
                strokeWidth = 1.2f
            )

            // Wall corner perspective lines
            drawLine(
                color = Color(0x1F22D3EE),
                start = Offset(w * 0.2f, 0f),
                end = Offset(w * 0.2f, horizonY),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0x1F22D3EE),
                start = Offset(w * 0.8f, 0f),
                end = Offset(w * 0.8f, horizonY),
                strokeWidth = 1f
            )

            // Floor perspective grid lines
            drawLine(
                color = Color(0x1A22D3EE),
                start = Offset(w * 0.2f, horizonY),
                end = Offset(0f, h),
                strokeWidth = 1.2f
            )
            drawLine(
                color = Color(0x1A22D3EE),
                start = Offset(w * 0.8f, horizonY),
                end = Offset(w, h),
                strokeWidth = 1.2f
            )
        }
    }
}
