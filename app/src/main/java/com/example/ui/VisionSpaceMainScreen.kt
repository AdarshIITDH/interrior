package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ARScanScreen
import com.example.ui.components.ARStudioScreen
import com.example.ui.components.AutoFitScreen
import com.example.ui.components.BOMReportDialog
import com.example.ui.components.CameraView
import com.example.ui.components.ExploreScreen
import com.example.ui.components.FinishSelectionScreen
import com.example.ui.components.HomeScreen
import com.example.ui.components.InteriorConfigScreen
import com.example.ui.components.OpenWardrobeScreen
import com.example.ui.components.PlaceWardrobeScreen
import com.example.ui.components.SaveConfirmationScreen
import com.example.ui.components.ShareDetailsScreen
import com.example.ui.components.SpacesScreen
import com.example.ui.components.SplashScreen
import com.example.ui.components.VisionSpaceBottomNav
import com.example.ui.components.VisionSpaceMenuSheet
import com.example.ui.theme.ObsidianBackground
import com.example.viewmodel.AppScreen
import com.example.viewmodel.NavigationTab
import com.example.viewmodel.VisionSpaceViewModel

@Composable
fun VisionSpaceMainScreen(
    viewModel: VisionSpaceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deviceOrientation by viewModel.deviceOrientation.collectAsStateWithLifecycle()
    val savedWardrobes by viewModel.savedWardrobes.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val isARScreen = uiState.currentScreen in setOf(
        AppScreen.AR_SCAN,
        AppScreen.PLACE_WARDROBE,
        AppScreen.AR_STUDIO,
        AppScreen.INTERIOR_CONFIG,
        AppScreen.FINISH_SELECTION,
        AppScreen.AUTOFIT_SCREEN,
        AppScreen.OPEN_WARDROBE
    )

    val showBottomNav = uiState.currentScreen == AppScreen.HOME ||
            uiState.currentScreen == AppScreen.SPACES_TAB ||
            uiState.currentScreen == AppScreen.EXPLORE_TAB

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBackground,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomNav) {
                VisionSpaceBottomNav(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { tab -> viewModel.selectTab(tab) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBackground)
        ) {
            // Persistent Camera Preview in Background for all AR Screens to prevent disappearing on placement
            if (isARScreen) {
                CameraView(
                    onPermissionChanged = { granted ->
                        viewModel.setCameraPermissionGranted(granted)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            AnimatedContent(
                targetState = uiState.currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    // Screen 1: Splash
                    AppScreen.SPLASH -> {
                        SplashScreen(
                            onStartDesigning = { viewModel.startDesigningFlow() }
                        )
                    }

                    // Screen 2: Home
                    AppScreen.HOME -> {
                        HomeScreen(
                            onDesignInAR = { viewModel.startDesigningFlow() },
                            onOpenSpaces = { viewModel.selectTab(NavigationTab.SPACES) },
                            onOpenExplore = { viewModel.selectTab(NavigationTab.EXPLORE) }
                        )
                    }

                    // Screen 3: AR Scan & Interactive Space Area Selection
                    AppScreen.AR_SCAN -> {
                        ARScanScreen(
                            roomMeasurement = uiState.roomMeasurement,
                            isTorchOn = uiState.isTorchOn,
                            onToggleTorch = { viewModel.toggleTorch() },
                            onCloseScan = { viewModel.navigateTo(AppScreen.HOME) },
                            onScanReady = { viewModel.navigateTo(AppScreen.PLACE_WARDROBE) },
                            onGenerateCustomWardrobe = { wM, hM, dM ->
                                viewModel.generateWardrobeForMeasuredSpace(wM, hM, dM)
                            }
                        )
                    }

                    // Screen 4: Place Wardrobe
                    AppScreen.PLACE_WARDROBE -> {
                        PlaceWardrobeScreen(
                            roomMeasurement = uiState.roomMeasurement,
                            onClose = { viewModel.navigateTo(AppScreen.HOME) },
                            onPlaceWardrobe = { viewModel.placeWardrobeInAR() }
                        )
                    }

                    // Screen 5: Adjust & Customize (AR Studio)
                    AppScreen.AR_STUDIO -> {
                        ARStudioScreen(
                            currentConfig = uiState.currentConfig,
                            placement = uiState.placement,
                            deviceOrientation = deviceOrientation,
                            isZenMode = uiState.isZenMode,
                            isDpadVisible = uiState.isDpadVisible,
                            unitSystem = uiState.unitSystem,
                            onRotateYaw = { delta -> viewModel.rotateYaw(delta) },
                            onTranslate = { deltaLat, deltaDist -> viewModel.translateSpatial(deltaLat, deltaDist) },
                            onToggleDoors = { viewModel.navigateTo(AppScreen.OPEN_WARDROBE) },
                            onOpenInterior = { viewModel.openInteriorConfig() },
                            onOpenFinish = { viewModel.openFinishSelection() },
                            onOpenAutoFit = { viewModel.openAutoFit() },
                            onOpenBOM = { viewModel.openBOMDialog() },
                            onToggleUnit = { viewModel.toggleUnitSystem() },
                            onOpenSave = {
                                viewModel.saveCurrentDesign()
                                viewModel.navigateTo(AppScreen.SAVE_CONFIRMATION)
                            },
                            onOpenShare = { viewModel.navigateTo(AppScreen.SHARE_DETAILS) },
                            onToggleDpad = { viewModel.toggleDpad() },
                            onToggleZenMode = { viewModel.toggleZenMode() },
                            onOpenMenu = { viewModel.toggleMenuSheet(true) },
                            onPanLeft = { viewModel.translateSpatial(-0.08f, 0f) },
                            onPanRight = { viewModel.translateSpatial(0.08f, 0f) },
                            onMoveCloser = { viewModel.translateSpatial(0f, -0.1f) },
                            onMoveFarther = { viewModel.translateSpatial(0f, 0.1f) },
                            onElevateUp = { viewModel.adjustElevation(0.05f) },
                            onElevateDown = { viewModel.adjustElevation(-0.05f) },
                            onRotateStepLeft = { viewModel.rotateYaw(-15f) },
                            onRotateStepRight = { viewModel.rotateYaw(15f) },
                            onSetAngle = { deg -> viewModel.setYaw(deg) },
                            onResetPlacement = { viewModel.resetPlacement() }
                        )
                    }

                    // Screen 6: Interior Configuration
                    AppScreen.INTERIOR_CONFIG -> {
                        InteriorConfigScreen(
                            currentConfig = uiState.currentConfig,
                            placement = uiState.placement,
                            deviceOrientation = deviceOrientation,
                            selectedCategory = uiState.selectedInteriorCategory,
                            onSelectCategory = { cat -> viewModel.selectInteriorCategory(cat) },
                            onApplyPreset = { preset -> viewModel.applyInteriorPreset(preset) },
                            onBack = { viewModel.navigateTo(AppScreen.AR_STUDIO) }
                        )
                    }

                    // Screen 7: Finish Selection
                    AppScreen.FINISH_SELECTION -> {
                        FinishSelectionScreen(
                            currentConfig = uiState.currentConfig,
                            placement = uiState.placement,
                            deviceOrientation = deviceOrientation,
                            onSelectFinish = { finish -> viewModel.updateFinish(finish) },
                            onBack = { viewModel.navigateTo(AppScreen.AR_STUDIO) },
                            onApply = { viewModel.navigateTo(AppScreen.AR_STUDIO) }
                        )
                    }

                    // Screen 8: AutoFit Screen
                    AppScreen.AUTOFIT_SCREEN -> {
                        AutoFitScreen(
                            roomMeasurement = uiState.roomMeasurement,
                            unitSystem = uiState.unitSystem,
                            onBack = { viewModel.navigateTo(AppScreen.AR_STUDIO) },
                            onApply = { viewModel.applyAutoFit() }
                        )
                    }

                    // Screen 9: Open Wardrobe
                    AppScreen.OPEN_WARDROBE -> {
                        OpenWardrobeScreen(
                            currentConfig = uiState.currentConfig,
                            placement = uiState.placement,
                            deviceOrientation = deviceOrientation,
                            unitSystem = uiState.unitSystem,
                            onCloseDoors = { viewModel.navigateTo(AppScreen.AR_STUDIO) }
                        )
                    }

                    // Screen 10: Save Confirmation
                    AppScreen.SAVE_CONFIRMATION -> {
                        SaveConfirmationScreen(
                            onViewInSpaces = {
                                viewModel.selectTab(NavigationTab.SPACES)
                            },
                            onContinueDesigning = {
                                viewModel.navigateTo(AppScreen.AR_STUDIO)
                            }
                        )
                    }

                    // Screen 11: Share Details
                    AppScreen.SHARE_DETAILS -> {
                        ShareDetailsScreen(
                            currentConfig = uiState.currentConfig,
                            unitSystem = uiState.unitSystem,
                            onShareAction = { viewModel.shareWardrobe(context) },
                            onBack = { viewModel.navigateTo(AppScreen.AR_STUDIO) }
                        )
                    }

                    // Screen 12: Spaces Tab
                    AppScreen.SPACES_TAB -> {
                        SpacesScreen(
                            savedWardrobes = savedWardrobes,
                            onSelectWardrobe = { config ->
                                viewModel.loadCustomConfig(config)
                            },
                            onDeleteWardrobe = { id -> viewModel.deleteSavedDesign(id) }
                        )
                    }

                    // Screen 13: Explore Tab
                    AppScreen.EXPLORE_TAB -> {
                        ExploreScreen(
                            onSelectPreset = { preset ->
                                viewModel.selectPreset(preset)
                                viewModel.placeWardrobeInAR()
                            },
                            onSelectFinishStyle = { finish ->
                                viewModel.updateFinish(finish)
                                viewModel.placeWardrobeInAR()
                            }
                        )
                    }
                }
            }

            // Menu Drawer Sheet for Quick Navigation & Back-tracking
            if (uiState.isMenuSheetOpen) {
                VisionSpaceMenuSheet(
                    onDismiss = { viewModel.toggleMenuSheet(false) },
                    onNavigateHome = {
                        viewModel.toggleMenuSheet(false)
                        viewModel.navigateTo(AppScreen.HOME)
                    },
                    onOpenAutoFit = {
                        viewModel.toggleMenuSheet(false)
                        viewModel.openAutoFit()
                    },
                    onOpenInterior = {
                        viewModel.toggleMenuSheet(false)
                        viewModel.openInteriorConfig()
                    },
                    onOpenFinish = {
                        viewModel.toggleMenuSheet(false)
                        viewModel.openFinishSelection()
                    },
                    onOpenSpaces = {
                        viewModel.toggleMenuSheet(false)
                        viewModel.selectTab(NavigationTab.SPACES)
                    },
                    onOpenExplore = {
                        viewModel.toggleMenuSheet(false)
                        viewModel.selectTab(NavigationTab.EXPLORE)
                    },
                    onOpenBOM = {
                        viewModel.toggleMenuSheet(false)
                        viewModel.openBOMDialog()
                    },
                    unitSystem = uiState.unitSystem,
                    onToggleUnitSystem = {
                        viewModel.toggleUnitSystem()
                    }
                )
            }

            // Live Interactive BOM & Cost Breakdown Dialog (Indian Rupees)
            if (uiState.isBOMDialogOpen) {
                BOMReportDialog(
                    config = uiState.currentConfig,
                    initialUnitSystem = uiState.unitSystem,
                    onDimensionsChange = { w, h, d ->
                        viewModel.updateDimensions(w, h, d)
                    },
                    onSaveToVault = { customName ->
                        viewModel.saveCurrentDesign(customName)
                    },
                    onDismiss = {
                        viewModel.closeBOMDialog()
                    }
                )
            }
        }
    }
}
