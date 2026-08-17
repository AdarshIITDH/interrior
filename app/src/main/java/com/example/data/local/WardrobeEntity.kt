package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.DoorStyle
import com.example.model.FinishType
import com.example.model.LedLighting
import com.example.model.WardrobeConfig

@Entity(tableName = "saved_wardrobes")
data class WardrobeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val widthCm: Float,
    val heightCm: Float,
    val depthCm: Float,
    val finishName: String,
    val doorStyleName: String,
    val shelvesCount: Int,
    val hangingRailsCount: Int,
    val drawersCount: Int,
    val hasShoeRack: Boolean,
    val hasMirrorPanel: Boolean,
    val ledLightingName: String,
    val handleStyle: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): WardrobeConfig {
        val finish = try {
            FinishType.valueOf(finishName)
        } catch (e: Exception) {
            FinishType.MATTE_OBSIDIAN
        }
        val doorStyle = try {
            DoorStyle.valueOf(doorStyleName)
        } catch (e: Exception) {
            DoorStyle.DUAL_HINGED
        }
        val ledLighting = try {
            LedLighting.valueOf(ledLightingName)
        } catch (e: Exception) {
            LedLighting.CYAN_HOLOGRAPHIC
        }

        return WardrobeConfig(
            id = "saved_$id",
            name = name,
            widthCm = widthCm,
            heightCm = heightCm,
            depthCm = depthCm,
            finish = finish,
            doorStyle = doorStyle,
            shelvesCount = shelvesCount,
            hangingRailsCount = hangingRailsCount,
            drawersCount = drawersCount,
            hasShoeRack = hasShoeRack,
            hasMirrorPanel = hasMirrorPanel,
            ledLighting = ledLighting,
            handleStyle = handleStyle
        )
    }

    companion object {
        fun fromDomain(config: WardrobeConfig): WardrobeEntity {
            return WardrobeEntity(
                name = config.name,
                widthCm = config.widthCm,
                heightCm = config.heightCm,
                depthCm = config.depthCm,
                finishName = config.finish.name,
                doorStyleName = config.doorStyle.name,
                shelvesCount = config.shelvesCount,
                hangingRailsCount = config.hangingRailsCount,
                drawersCount = config.drawersCount,
                hasShoeRack = config.hasShoeRack,
                hasMirrorPanel = config.hasMirrorPanel,
                ledLightingName = config.ledLighting.name,
                handleStyle = config.handleStyle
            )
        }
    }
}
