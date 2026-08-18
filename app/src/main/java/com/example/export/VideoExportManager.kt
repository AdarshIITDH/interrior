package com.example.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import com.example.model.BOMCalculator
import com.example.model.WardrobeProject
import com.example.spatial.TechnicalDrawingEngine
import com.example.spatial.TechnicalDrawingViewType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-performance hardware video encoding service for VisionSpace.
 * Produces Site Presentation MP4 videos (8-12 seconds) without requiring external video libraries.
 */
object VideoExportManager {

    private const val VIDEO_MIME = "video/avc"
    private const val FRAME_RATE = 30
    private const val I_FRAME_INTERVAL = 1

    /**
     * Generates a smooth 10-second Presentation MP4 clip using Android MediaCodec hardware encoder.
     */
    suspend fun generatePresentationVideo(
        context: Context,
        project: WardrobeProject,
        sitePhotoBitmap: Bitmap? = null,
        onProgress: (progressFraction: Float, status: String) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val width = 1280
        val height = 720
        val bitRate = 4_000_000 // 4 Mbps
        val totalDurationSec = 10
        val totalFrames = totalDurationSec * FRAME_RATE

        val sanitizedName = project.name.replace("\\s+".toRegex(), "_").replace("[^a-zA-Z0-9_-]".toRegex(), "")
        val outputFile = File(context.cacheDir, "VisionSpace_${sanitizedName}_Presentation.mp4")
        if (outputFile.exists()) outputFile.delete()

        // Prepare Base Bitmaps
        val baseSiteBmp = sitePhotoBitmap ?: TechnicalDrawingEngine.generateDrawingBitmap(
            project = project,
            viewType = TechnicalDrawingViewType.FRONT_ELEVATION,
            widthPx = width,
            heightPx = height,
            isDarkTheme = true
        )
        val scaledSiteBmp = Bitmap.createScaledBitmap(baseSiteBmp, width, height, true)

        val intBmp = TechnicalDrawingEngine.generateDrawingBitmap(
            project = project,
            viewType = TechnicalDrawingViewType.INTERIOR_ELEVATION,
            widthPx = width,
            heightPx = height,
            isDarkTheme = true
        )
        val scaledIntBmp = Bitmap.createScaledBitmap(intBmp, width, height, true)

        val format = MediaFormat.createVideoFormat(VIDEO_MIME, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
        }

        val encoder = MediaCodec.createEncoderByType(VIDEO_MIME)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface: Surface = encoder.createInputSurface()
        encoder.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()

        val bom = BOMCalculator.calculateBOM(project.wardrobeConfig)
        val costFormatted = PdfExportService.formatInr(bom.totalCost)

        try {
            for (frameIndex in 0 until totalFrames) {
                val timeSec = frameIndex.toFloat() / FRAME_RATE
                val progress = frameIndex.toFloat() / totalFrames
                onProgress(progress, "Rendering Frame $frameIndex/$totalFrames (${(timeSec).toInt()}s)...")

                // Lock Canvas on Input Surface
                val canvas: Canvas = inputSurface.lockHardwareCanvas()

                // Render Animated Presentation Frame
                renderAnimatedVideoFrame(
                    canvas = canvas,
                    width = width,
                    height = height,
                    timeSec = timeSec,
                    project = project,
                    siteBmp = scaledSiteBmp,
                    intBmp = scaledIntBmp,
                    costFormatted = costFormatted
                )

                inputSurface.unlockCanvasAndPost(canvas)

                // Drain Encoder
                while (true) {
                    val status = encoder.dequeueOutputBuffer(bufferInfo, 0)
                    if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) throw RuntimeException("Format changed twice")
                        val newFormat = encoder.outputFormat
                        trackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (status >= 0) {
                        val encodedData = encoder.getOutputBuffer(status)
                            ?: throw RuntimeException("Encoder buffer was null")

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }

                        if (bufferInfo.size != 0) {
                            if (!muxerStarted) throw RuntimeException("Muxer hasn't started")
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)

                            bufferInfo.presentationTimeUs = (frameIndex * 1_000_000L / FRAME_RATE)
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }

                        encoder.releaseOutputBuffer(status, false)
                    }
                }
            }

            // Signal End of Stream
            encoder.signalEndOfInputStream()

            // Drain remaining
            var isEos = false
            while (!isEos) {
                val status = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (status >= 0) {
                    val encodedData = encoder.getOutputBuffer(status)
                    if (encodedData != null && bufferInfo.size != 0 && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isEos = true
                    }
                    encoder.releaseOutputBuffer(status, false)
                } else if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
            }
        } finally {
            try {
                encoder.stop()
                encoder.release()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
            if (muxerStarted) {
                try {
                    muxer.stop()
                    muxer.release()
                } catch (e: Exception) {
                    // Ignore muxer release error
                }
            }
        }

        outputFile
    }

    private fun renderAnimatedVideoFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        timeSec: Float,
        project: WardrobeProject,
        siteBmp: Bitmap,
        intBmp: Bitmap,
        costFormatted: String
    ) {
        canvas.drawColor(Color.rgb(15, 23, 42))

        // Phase 1 (0..3 sec): Gentle Pan & Zoom on Site
        // Phase 2 (3..6 sec): Door Transition reveal
        // Phase 3 (6..8 sec): Dimension Overlays appear
        // Phase 4 (8..10 sec): Project Title & Cost Card

        val zoomFactor = 1.0f + (timeSec * 0.015f)
        val panX = -((zoomFactor - 1.0f) * width * 0.5f)
        val panY = -((zoomFactor - 1.0f) * height * 0.5f)

        canvas.save()
        canvas.translate(panX, panY)
        canvas.scale(zoomFactor, zoomFactor)

        val srcRect = Rect(0, 0, siteBmp.width, siteBmp.height)
        val destRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        if (timeSec < 3.5f) {
            canvas.drawBitmap(siteBmp, srcRect, destRect, null)
        } else if (timeSec < 6.5f) {
            // Dissolve from Exterior to Interior
            val interiorAlpha = ((timeSec - 3.5f) / 1.5f).coerceIn(0f, 1f)
            canvas.drawBitmap(siteBmp, srcRect, destRect, null)
            val p = Paint().apply { alpha = (interiorAlpha * 255).toInt() }
            canvas.drawBitmap(intBmp, srcRect, destRect, p)
        } else {
            canvas.drawBitmap(intBmp, srcRect, destRect, null)
        }

        canvas.restore()

        // Watermark / Brand Header
        val brandPaint = Paint().apply {
            color = Color.rgb(0, 240, 255)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(8f, 0f, 0f, Color.BLACK)
        }
        canvas.drawText("VISIONSPACE", 40f, 50f, brandPaint)

        // Phase 3 (6..10 sec): Animated Dimension Overlay
        if (timeSec >= 5.5f) {
            val dimAlpha = ((timeSec - 5.5f) / 1.0f).coerceIn(0f, 1f)
            val dimBoxPaint = Paint().apply {
                color = Color.argb((dimAlpha * 180).toInt(), 15, 23, 42)
                style = Paint.Style.FILL
            }
            val dimTextPaint = Paint().apply {
                color = Color.argb((dimAlpha * 255).toInt(), 255, 255, 255)
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val pillRect = RectF(width / 2f - 180f, height - 120f, width / 2f + 180f, height - 60f)
            canvas.drawRoundRect(pillRect, 30f, 30f, dimBoxPaint)
            canvas.drawText(project.formattedOverallDimensionsFtIn, width / 2f - 140f, height - 82f, dimTextPaint)
        }

        // Phase 4 (7.5..10 sec): Project Title & Cost Card Reveal
        if (timeSec >= 7.0f) {
            val cardAlpha = ((timeSec - 7.0f) / 1.0f).coerceIn(0f, 1f)
            val cardBg = Paint().apply {
                color = Color.argb((cardAlpha * 220).toInt(), 15, 23, 42)
                style = Paint.Style.FILL
            }
            val titlePaint = Paint().apply {
                color = Color.argb((cardAlpha * 255).toInt(), 255, 255, 255)
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val costPaint = Paint().apply {
                color = Color.argb((cardAlpha * 255).toInt(), 52, 211, 153)
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val cardRect = RectF(40f, height - 150f, 420f, height - 40f)
            canvas.drawRoundRect(cardRect, 16f, 16f, cardBg)
            canvas.drawText(project.name, 60f, height - 105f, titlePaint)
            canvas.drawText("Est. $costFormatted", 60f, height - 65f, costPaint)
        }
    }
}
