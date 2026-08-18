package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.VisionSpaceDatabase
import com.example.data.local.WardrobeRepository
import com.example.model.DoorStyle
import com.example.model.DimensionFormatter
import com.example.model.FinishType
import com.example.model.InteriorCategory
import com.example.model.InteriorPreset
import com.example.model.LedLighting
import com.example.model.PresetCatalog
import com.example.model.RoomMeasurement
import com.example.model.UnitSystem
import com.example.model.WardrobeConfig
import com.example.model.WardrobePreset
import com.example.model.BOMCalculator
import com.example.spatial.ARPlacementState
import com.example.spatial.DeviceOrientationState
import com.example.spatial.SpatialSensorEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import android.graphics.Bitmap
import com.example.model.WardrobeProject
import com.example.model.SiteCapture
import com.example.model.MeasurementConfidence

enum class AppScreen {
    SPLASH,                 // 1. Splash Screen
    HOME,                   // 2. Home Screen
    SITE_CAMERA_CAPTURE,    // 3. Calibrated Site Photo Capture
    SITE_CALIBRATION,       // 4. Space Calibration (4-point Quad)
    SITE_PHOTO_EDITOR,      // 5. Static Site Photo 3D Studio
    TECHNICAL_DRAWING,      // 6. 2D CAD Technical Drawings (Front/Interior/Plan/Side)
    CARPENTER_SHARE,        // 7. Share with Carpenter (ZIP, DXF, PDF, MP4)
    AR_SCAN,                // 8. Live AR Scan
    PLACE_WARDROBE,         // 9. Place Wardrobe in Live AR
    AR_STUDIO,              // 10. Live AR Studio
    INTERIOR_CONFIG,        // 11. Interior Configuration
    FINISH_SELECTION,       // 12. Finish Selection
    AUTOFIT_SCREEN,         // 13. AutoFit Screen
    OPEN_WARDROBE,          // 14. Open Wardrobe
    SAVE_CONFIRMATION,      // 15. Save Confirmation
    SHARE_DETAILS,          // 16. Share Details
    SPACES_TAB,             // 17. Saved Spaces
    EXPLORE_TAB             // 18. Explore
}

enum class NavigationTab {
    DESIGN,
    SPACES,
    EXPLORE
}

data class VisionSpaceUiState(
    val currentScreen: AppScreen = AppScreen.SPLASH,
    val selectedTab: NavigationTab = NavigationTab.DESIGN,
    val currentConfig: WardrobeConfig = PresetCatalog.PRESETS.first().config,
    val currentProject: WardrobeProject = WardrobeProject(wardrobeConfig = PresetCatalog.PRESETS.first().config),
    val siteBitmap: Bitmap? = null,
    val placement: ARPlacementState = ARPlacementState(),
    val roomMeasurement: RoomMeasurement = RoomMeasurement(),
    val selectedPresetId: String = PresetCatalog.PRESETS.first().id,
    val selectedInteriorCategory: InteriorCategory = InteriorCategory.ALL,
    val unitSystem: UnitSystem = UnitSystem.FEET_INCHES, // Feet & Inches default
    val isCustomizerSheetOpen: Boolean = false,
    val isSavedLayoutsDrawerOpen: Boolean = false,
    val isSaveConfirmationOpen: Boolean = false,
    val isShareDialogOpen: Boolean = false,
    val isBOMDialogOpen: Boolean = false,
    val isMenuSheetOpen: Boolean = false,
    val isDpadVisible: Boolean = false,
    val isZenMode: Boolean = false,
    val isCameraPermissionGranted: Boolean = false,
    val isTorchOn: Boolean = false,
    val isMotionSensorTrackingEnabled: Boolean = true,
    val statusMessage: String? = null,
    val snapshotTimestamp: Long = 0L,
    val activeMeasurementMode: String = "WALL_AUTO" // WALL_AUTO, POINT_TO_POINT
)

class VisionSpaceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WardrobeRepository
    private val spatialSensorEngine: SpatialSensorEngine = SpatialSensorEngine(application)

    private val _uiState = MutableStateFlow(VisionSpaceUiState())
    val uiState: StateFlow<VisionSpaceUiState> = _uiState.asStateFlow()

    val deviceOrientation: StateFlow<DeviceOrientationState> = spatialSensorEngine.orientationState
    val savedWardrobes: StateFlow<List<WardrobeConfig>>

    init {
        val database = VisionSpaceDatabase.getDatabase(application)
        repository = WardrobeRepository(database.wardrobeDao())
        savedWardrobes = repository.savedWardrobes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed sample layouts if database is empty
        viewModelScope.launch {
            repository.savedWardrobes.collect { list ->
                if (list.isEmpty()) {
                    PresetCatalog.PRESETS.take(4).forEach { preset ->
                        repository.saveWardrobe(preset.config.copy(name = preset.name))
                    }
                }
            }
        }

        spatialSensorEngine.startListening()
    }

    override fun onCleared() {
        super.onCleared()
        spatialSensorEngine.stopListening()
    }

    // Navigation Controls
    fun navigateTo(screen: AppScreen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun selectTab(tab: NavigationTab) {
        _uiState.value = _uiState.value.copy(
            selectedTab = tab,
            currentScreen = when (tab) {
                NavigationTab.DESIGN -> AppScreen.HOME
                NavigationTab.SPACES -> AppScreen.SPACES_TAB
                NavigationTab.EXPLORE -> AppScreen.EXPLORE_TAB
            }
        )
    }

    fun updateRoomMeasurement(widthM: Float, heightM: Float, depthM: Float = 0.65f) {
        _uiState.value = _uiState.value.copy(
            roomMeasurement = _uiState.value.roomMeasurement.copy(
                detectedWallWidthM = widthM.coerceIn(1.0f, 6.0f),
                detectedHeightM = heightM.coerceIn(1.8f, 3.5f),
                detectedDepthM = depthM.coerceIn(0.4f, 1.5f),
                isWallDetected = true
            )
        )
    }

    fun generateWardrobeForMeasuredSpace(
        wallWidthM: Float? = null,
        wallHeightM: Float? = null,
        wallDepthM: Float? = null
    ) {
        val wm = wallWidthM ?: _uiState.value.roomMeasurement.detectedWallWidthM
        val hm = wallHeightM ?: _uiState.value.roomMeasurement.detectedHeightM
        val dm = wallDepthM ?: _uiState.value.roomMeasurement.detectedDepthM

        // Update measurement state
        updateRoomMeasurement(wm, hm, dm)

        // Calculate tailored wardrobe dimensions:
        // - Allow 10-15cm side margin (or full fit if alcove)
        // - Allow 15-20cm ceiling loft margin for airflow and assembly
        val targetWidthCm = ((wm * 100f) - 20f).coerceIn(80f, 320f)
        val targetHeightCm = ((hm * 100f) - 15f).coerceIn(180f, 260f)
        val targetDepthCm = 60f // Standard ergonomic hanger depth

        // Calculate optimal modules based on width:
        // < 140cm: 2 bays, 1 rail, 3 shelves, 2 drawers
        // 140 - 220cm: 2 large bays, 2 rails, 4 shelves, 3 drawers
        // 220 - 280cm: 3 bays, 2-3 rails, 4-5 shelves, 3-4 drawers
        // > 280cm: 4 bays, 3 rails, 6 shelves, 4 drawers
        val shelves = when {
            targetWidthCm < 140f -> 3
            targetWidthCm < 220f -> 4
            targetWidthCm < 280f -> 5
            else -> 6
        }
        val rails = when {
            targetWidthCm < 140f -> 1
            targetWidthCm < 220f -> 2
            else -> 3
        }
        val drawers = when {
            targetWidthCm < 140f -> 2
            targetWidthCm < 220f -> 3
            else -> 4
        }
        val optimalDoor = when {
            targetWidthCm > 240f -> DoorStyle.SLIDING_BYPASS
            targetWidthCm > 180f -> DoorStyle.DUAL_HINGED
            else -> DoorStyle.DUAL_HINGED
        }

        _uiState.value = _uiState.value.copy(
            currentConfig = _uiState.value.currentConfig.copy(
                name = "Custom Fit ${targetWidthCm.toInt()}cm Wardrobe",
                widthCm = targetWidthCm,
                heightCm = targetHeightCm,
                depthCm = targetDepthCm,
                shelvesCount = shelves,
                hangingRailsCount = rails,
                drawersCount = drawers,
                doorStyle = optimalDoor
            ),
            currentScreen = AppScreen.AR_STUDIO,
            placement = _uiState.value.placement.copy(isPlaced = true),
            statusMessage = "Custom wardrobe generated for ${wm.format1Dec()}m space"
        )
    }

    private fun Float.format1Dec(): String = String.format("%.2f", this)

    fun startDesigningFlow() {
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.AR_SCAN,
            roomMeasurement = RoomMeasurement(
                detectedWallWidthM = 3.86f,
                detectedHeightM = 2.60f,
                detectedDepthM = 1.20f,
                isWallDetected = true,
                scanProgress = 1.0f
            )
        )
    }

    fun placeWardrobeInAR() {
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.AR_STUDIO,
            placement = _uiState.value.placement.copy(isPlaced = true),
            statusMessage = "Wardrobe anchored to wall"
        )
    }

    fun openInteriorConfig() {
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.INTERIOR_CONFIG,
            currentConfig = _uiState.value.currentConfig.copy(doorOpenRatio = 1.0f) // Open doors to inspect interior
        )
    }

    fun openFinishSelection() {
        _uiState.value = _uiState.value.copy(currentScreen = AppScreen.FINISH_SELECTION)
    }

    fun openAutoFit() {
        _uiState.value = _uiState.value.copy(currentScreen = AppScreen.AUTOFIT_SCREEN)
    }

    fun applyAutoFit() {
        val measurement = _uiState.value.roomMeasurement
        _uiState.value = _uiState.value.copy(
            currentConfig = _uiState.value.currentConfig.copy(
                widthCm = measurement.recommendedWidthCm,
                heightCm = measurement.recommendedHeightCm,
                depthCm = measurement.recommendedDepthCm
            ),
            currentScreen = AppScreen.AR_STUDIO,
            statusMessage = "AutoFit applied: ${measurement.recommendedWidthCm.toInt()} × ${measurement.recommendedHeightCm.toInt()} cm"
        )
    }

    fun toggleTorch() {
        _uiState.value = _uiState.value.copy(isTorchOn = !_uiState.value.isTorchOn)
    }

    fun setCameraPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(isCameraPermissionGranted = granted)
    }

    // Preset & Customization
    fun selectPreset(preset: WardrobePreset) {
        _uiState.value = _uiState.value.copy(
            currentConfig = preset.config,
            selectedPresetId = preset.id,
            statusMessage = "Loaded ${preset.name}"
        )
    }

    fun loadCustomConfig(config: WardrobeConfig) {
        _uiState.value = _uiState.value.copy(
            currentConfig = config,
            selectedPresetId = "custom",
            currentScreen = AppScreen.AR_STUDIO,
            statusMessage = "Loaded ${config.name}"
        )
    }

    fun updateDimensions(widthCm: Float, heightCm: Float, depthCm: Float) {
        _uiState.value = _uiState.value.copy(
            currentConfig = _uiState.value.currentConfig.copy(
                widthCm = widthCm.coerceIn(80f, 320f),
                heightCm = heightCm.coerceIn(160f, 270f),
                depthCm = depthCm.coerceIn(40f, 80f)
            )
        )
    }

    fun updateFinish(finish: FinishType) {
        _uiState.value = _uiState.value.copy(
            currentConfig = _uiState.value.currentConfig.copy(finish = finish)
        )
    }

    fun updateDoorStyle(doorStyle: DoorStyle) {
        _uiState.value = _uiState.value.copy(
            currentConfig = _uiState.value.currentConfig.copy(doorStyle = doorStyle)
        )
    }

    fun toggleDoors() {
        val current = _uiState.value.currentConfig.doorOpenRatio
        val target = if (current > 0.5f) 0.0f else 1.0f
        _uiState.value = _uiState.value.copy(
            currentConfig = _uiState.value.currentConfig.copy(doorOpenRatio = target),
            statusMessage = if (target > 0f) "Doors opened" else "Doors closed"
        )
    }

    fun setDoorOpenRatio(ratio: Float) {
        _uiState.value = _uiState.value.copy(
            currentConfig = _uiState.value.currentConfig.copy(doorOpenRatio = ratio.coerceIn(0f, 1f))
        )
    }

    fun selectInteriorCategory(category: InteriorCategory) {
        _uiState.value = _uiState.value.copy(selectedInteriorCategory = category)
    }

    fun applyInteriorPreset(preset: InteriorPreset) {
        _uiState.value = _uiState.value.copy(
            currentConfig = _uiState.value.currentConfig.copy(
                shelvesCount = preset.shelvesCount,
                hangingRailsCount = preset.hangingRailsCount,
                drawersCount = preset.drawersCount
            ),
            statusMessage = "Applied interior: ${preset.name}"
        )
    }

    fun updateModules(
        shelves: Int? = null,
        hangingRails: Int? = null,
        drawers: Int? = null,
        hasMirror: Boolean? = null,
        led: LedLighting? = null
    ) {
        val cur = _uiState.value.currentConfig
        _uiState.value = _uiState.value.copy(
            currentConfig = cur.copy(
                shelvesCount = shelves ?: cur.shelvesCount,
                hangingRailsCount = hangingRails ?: cur.hangingRailsCount,
                drawersCount = drawers ?: cur.drawersCount,
                hasMirrorPanel = hasMirror ?: cur.hasMirrorPanel,
                ledLighting = led ?: cur.ledLighting
            )
        )
    }

    fun updateConfig(config: WardrobeConfig) {
        _uiState.value = _uiState.value.copy(
            currentConfig = config
        )
    }

    // Spatial Manipulation & D-Pad
    fun rotateYaw(deltaDegrees: Float) {
        val currentPlacement = _uiState.value.placement
        _uiState.value = _uiState.value.copy(
            placement = currentPlacement.copy(
                userRotationYDeg = (currentPlacement.userRotationYDeg + deltaDegrees) % 360f
            )
        )
    }

    fun setYaw(degrees: Float) {
        val currentPlacement = _uiState.value.placement
        _uiState.value = _uiState.value.copy(
            placement = currentPlacement.copy(
                userRotationYDeg = degrees % 360f
            )
        )
    }

    fun translateSpatial(deltaLateralMeters: Float, deltaDistanceMeters: Float) {
        val cur = _uiState.value.placement
        _uiState.value = _uiState.value.copy(
            placement = cur.copy(
                lateralOffsetMeters = (cur.lateralOffsetMeters + deltaLateralMeters).coerceIn(-2.5f, 2.5f),
                distanceMeters = (cur.distanceMeters + deltaDistanceMeters).coerceIn(0.9f, 6.0f)
            )
        )
    }

    fun adjustElevation(deltaElevationMeters: Float) {
        val cur = _uiState.value.placement
        _uiState.value = _uiState.value.copy(
            placement = cur.copy(
                elevationOffsetMeters = (cur.elevationOffsetMeters + deltaElevationMeters).coerceIn(-1.5f, 1.5f)
            )
        )
    }

    fun resetPlacement() {
        _uiState.value = _uiState.value.copy(
            placement = ARPlacementState(
                distanceMeters = 2.4f,
                elevationOffsetMeters = -0.1f,
                lateralOffsetMeters = 0.0f,
                userRotationYDeg = 0f
            ),
            statusMessage = "Position reset"
        )
    }

    fun toggleDimensions() {
        val cur = _uiState.value.placement
        _uiState.value = _uiState.value.copy(
            placement = cur.copy(showDimensions = !cur.showDimensions)
        )
    }

    fun toggleFloorGrid() {
        val cur = _uiState.value.placement
        _uiState.value = _uiState.value.copy(
            placement = cur.copy(showFloorGrid = !cur.showFloorGrid)
        )
    }

    fun toggleDpad(visible: Boolean? = null) {
        val next = visible ?: !_uiState.value.isDpadVisible
        _uiState.value = _uiState.value.copy(isDpadVisible = next)
    }

    fun toggleZenMode(enabled: Boolean? = null) {
        val next = enabled ?: !_uiState.value.isZenMode
        _uiState.value = _uiState.value.copy(isZenMode = next)
    }

    fun setUnitSystem(unitSystem: UnitSystem) {
        _uiState.value = _uiState.value.copy(unitSystem = unitSystem)
    }

    fun toggleUnitSystem() {
        val next = when (_uiState.value.unitSystem) {
            UnitSystem.FEET_INCHES -> UnitSystem.INCHES
            UnitSystem.INCHES -> UnitSystem.CENTIMETERS
            UnitSystem.CENTIMETERS -> UnitSystem.FEET_INCHES
        }
        _uiState.value = _uiState.value.copy(unitSystem = next)
    }

    fun toggleMenuSheet(open: Boolean? = null) {
        val next = open ?: !_uiState.value.isMenuSheetOpen
        _uiState.value = _uiState.value.copy(isMenuSheetOpen = next)
    }

    // Dialog Controls
    fun openSaveConfirmation() {
        _uiState.value = _uiState.value.copy(isSaveConfirmationOpen = true)
    }

    fun closeSaveConfirmation() {
        _uiState.value = _uiState.value.copy(isSaveConfirmationOpen = false)
    }

    fun openShareDialog() {
        _uiState.value = _uiState.value.copy(
            isShareDialogOpen = true,
            snapshotTimestamp = System.currentTimeMillis()
        )
    }

    fun closeShareDialog() {
        _uiState.value = _uiState.value.copy(isShareDialogOpen = false)
    }

    fun openBOMDialog() {
        _uiState.value = _uiState.value.copy(isBOMDialogOpen = true)
    }

    fun closeBOMDialog() {
        _uiState.value = _uiState.value.copy(isBOMDialogOpen = false)
    }

    fun saveCurrentDesign(customName: String? = null) {
        viewModelScope.launch {
            val configToSave = if (!customName.isNullOrBlank()) {
                _uiState.value.currentConfig.copy(name = customName.trim())
            } else {
                _uiState.value.currentConfig
            }
            repository.saveWardrobe(configToSave)
            _uiState.value = _uiState.value.copy(
                isSaveConfirmationOpen = true,
                statusMessage = "Saved '${configToSave.name}' to Spaces"
            )
        }
    }

    fun deleteSavedDesign(id: String) {
        viewModelScope.launch {
            repository.deleteWardrobe(id)
            _uiState.value = _uiState.value.copy(statusMessage = "Design removed")
        }
    }

    fun shareWardrobe(context: Context) {
        val config = _uiState.value.currentConfig
        val bom = BOMCalculator.calculateBOM(config)
        val formattedDimensions = DimensionFormatter.formatDimensions(
            config.widthCm,
            config.heightCm,
            config.depthCm,
            _uiState.value.unitSystem
        )
        val shareText = """
            📐 VisionSpace 3D Wardrobe Design
            ------------------------------------
            Model: ${config.name}
            Dimensions: $formattedDimensions (${config.widthCm.toInt()}W × ${config.heightCm.toInt()}H × ${config.depthCm.toInt()}D cm)
            Material: ${config.finish.title}
            Door Style: ${config.doorStyle.title}
            Interior Modules: ${config.shelvesCount} Shelves, ${config.hangingRailsCount} Hanging Rails, ${config.drawersCount} Soft-Close Drawers
            Lighting: ${config.ledLighting.title}
            Estimated BOM Cost: ${DimensionFormatter.formatCurrencyINR(bom.totalEstimatedCostInr)} (INR)
            ------------------------------------
            Designed & Placed with VisionSpace AR
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share VisionSpace Wardrobe Design")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun startPhotoCaptureFlow() {
        _uiState.value = _uiState.value.copy(currentScreen = AppScreen.SITE_CAMERA_CAPTURE)
    }

    fun onSitePhotoCaptured(siteCapture: SiteCapture, bitmap: Bitmap) {
        val updatedProject = _uiState.value.currentProject.copy(
            siteCapture = siteCapture,
            wardrobeConfig = _uiState.value.currentConfig
        )
        _uiState.value = _uiState.value.copy(
            currentProject = updatedProject,
            siteBitmap = bitmap,
            currentScreen = AppScreen.SITE_PHOTO_EDITOR,
            statusMessage = "Site photo captured with spatial calibration"
        )
    }

    fun onStartGalleryCalibration(bitmap: Bitmap) {
        val siteCapture = SiteCapture(
            imageWidthPx = bitmap.width,
            imageHeightPx = bitmap.height,
            confidence = MeasurementConfidence.VISUAL_ONLY
        )
        _uiState.value = _uiState.value.copy(
            siteBitmap = bitmap,
            currentProject = _uiState.value.currentProject.copy(siteCapture = siteCapture),
            currentScreen = AppScreen.SITE_CALIBRATION
        )
    }

    fun onCalibrationCompleted(siteCapture: SiteCapture) {
        val updatedProject = _uiState.value.currentProject.copy(siteCapture = siteCapture)
        _uiState.value = _uiState.value.copy(
            currentProject = updatedProject,
            currentScreen = AppScreen.SITE_PHOTO_EDITOR,
            statusMessage = "Calibration applied • Ready to design"
        )
    }

    fun updateProject(project: WardrobeProject) {
        _uiState.value = _uiState.value.copy(
            currentProject = project,
            currentConfig = project.wardrobeConfig
        )
    }

    fun openTechnicalDrawing() {
        _uiState.value = _uiState.value.copy(
            currentProject = _uiState.value.currentProject.copy(wardrobeConfig = _uiState.value.currentConfig),
            currentScreen = AppScreen.TECHNICAL_DRAWING
        )
    }

    fun openCarpenterShare() {
        _uiState.value = _uiState.value.copy(
            currentProject = _uiState.value.currentProject.copy(wardrobeConfig = _uiState.value.currentConfig),
            currentScreen = AppScreen.CARPENTER_SHARE
        )
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}
