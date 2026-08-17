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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DoorStyle
import com.example.model.FinishType
import com.example.model.PresetCatalog
import com.example.model.WardrobeConfig
import com.example.model.WardrobePreset
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Screen 12: Spaces Tab (Saved Layouts Grid matching Screenshot 12)
 */
@Composable
fun SpacesScreen(
    savedWardrobes: List<WardrobeConfig>,
    onSelectWardrobe: (WardrobeConfig) -> Unit,
    onDeleteWardrobe: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filters = listOf("All", "Wardrobes")

    // Provide default mock spaces matching screenshot 12 if user list is empty
    val displayList = if (savedWardrobes.isNotEmpty()) {
        savedWardrobes
    } else {
        listOf(
            WardrobeConfig(id = "s1", name = "Bedroom Wardrobe", finish = FinishType.WALNUT, doorStyle = DoorStyle.SLIDING_DOOR, widthCm = 300f, heightCm = 240f, depthCm = 60f),
            WardrobeConfig(id = "s2", name = "Guest Room", finish = FinishType.OAK, doorStyle = DoorStyle.HINGED_DOOR, widthCm = 240f, heightCm = 240f, depthCm = 60f),
            WardrobeConfig(id = "s3", name = "Walk-in Wardrobe", finish = FinishType.WHITE, doorStyle = DoorStyle.OPEN_CONCEPT, widthCm = 320f, heightCm = 260f, depthCm = 60f),
            WardrobeConfig(id = "s4", name = "Minimal Wardrobe", finish = FinishType.GRAPHITE, doorStyle = DoorStyle.HINGED_DOOR, widthCm = 180f, heightCm = 220f, depthCm = 60f)
        )
    }

    val filteredList = displayList.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
    }

    val timestamps = listOf("Today, 10:24 AM", "Yesterday, 6:12 PM", "Mar 12, 2025", "Mar 8, 2025")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("spaces_screen")
    ) {
        // Header: "Spaces" + Search Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Spaces",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Pills: [ All (cyan active) ]  [ Wardrobes ]
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEachIndexed { index, filterName ->
                val isSelected = selectedFilterIndex == index
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) CyanPrimary else Color(0xFF111827))
                        .border(1.dp, if (isSelected) CyanNeon else Color(0x33FFFFFF), CircleShape)
                        .clickable { selectedFilterIndex = index }
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                        .testTag("spaces_filter_$filterName")
                ) {
                    Text(
                        text = filterName,
                        color = if (isSelected) Color.Black else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2x2 Grid of Saved Wardrobes
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(filteredList.take(6)) { wardrobe ->
                val idx = filteredList.indexOf(wardrobe)
                val timestamp = timestamps.getOrElse(idx % timestamps.size) { "Recent" }

                Card(
                    onClick = { onSelectWardrobe(wardrobe) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("saved_card_${wardrobe.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Thumbnail
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = wardrobe.finish.primaryColor,
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = wardrobe.name,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = timestamp,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen 13: Explore Tab (Wardrobe Styles & Finishes matching Screenshot 13)
 */
@Composable
fun ExploreScreen(
    onSelectPreset: (WardrobePreset) -> Unit,
    onSelectFinishStyle: (FinishType) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Wardrobe Styles", "Finishes")

    val styles = listOf(
        WardrobePreset(
            id = "hinged",
            name = "Hinged",
            subtitle = "Classic flush doors",
            category = "Classic",
            config = WardrobeConfig(name = "Hinged Wardrobe", doorStyle = DoorStyle.HINGED_DOOR, finish = FinishType.OAK)
        ),
        WardrobePreset(
            id = "sliding",
            name = "Sliding",
            subtitle = "Smooth glide panels",
            category = "Modern",
            config = WardrobeConfig(name = "Sliding Wardrobe", doorStyle = DoorStyle.SLIDING_DOOR, finish = FinishType.WALNUT)
        ),
        WardrobePreset(
            id = "mirror_sliding",
            name = "Mirror Sliding",
            subtitle = "Full-length reflection",
            category = "Luxury",
            config = WardrobeConfig(name = "Mirror Sliding Wardrobe", doorStyle = DoorStyle.MIRROR_SLIDING_DOOR, finish = FinishType.WHITE)
        ),
        WardrobePreset(
            id = "open",
            name = "Open",
            subtitle = "Boutique walk-in",
            category = "Contemporary",
            config = WardrobeConfig(name = "Open Wardrobe", doorStyle = DoorStyle.OPEN_CONCEPT, finish = FinishType.GRAPHITE)
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("explore_screen")
    ) {
        // Header
        Text(
            text = "Explore",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs: [ Wardrobe Styles (active) ]  [ Finishes ]
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = CyanNeon,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyanNeon,
                    height = 2.dp
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) CyanNeon else TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (selectedTab == 0) {
            // 2x2 Grid of Wardrobe Styles: Hinged, Sliding, Mirror Sliding, Open
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(styles) { preset ->
                    Card(
                        onClick = { onSelectPreset(preset) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("style_card_${preset.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            // Thumbnail
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewInAr,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = preset.name,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // Finishes Grid
            val finishes = FinishType.entries
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(finishes) { finish ->
                    Card(
                        onClick = { onSelectFinishStyle(finish) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("explore_finish_${finish.name.lowercase()}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(finish.primaryColor),
                                contentAlignment = Alignment.Center
                            ) {}

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = finish.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
