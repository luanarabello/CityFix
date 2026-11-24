package pt.isec.amov.cityfix.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImageUtils {

    /**
     * Comprime e redimensiona a imagem no próprio caminho [filePath].
     */
    fun compressImage(
        filePath: String,
        maxDimension: Int = 1280,
        quality: Int = 80
    ): Boolean {
        return saveResizedJpeg(
            sourcePath = filePath,
            destinationFile = File(filePath),
            maxDimension = maxDimension,
            quality = quality
        )
    }

    /**
     * Gera uma miniatura (thumbnail) em filesDir/thumbs/ e devolve o caminho.
     */
    fun createThumbnail(
        context: Context,
        imagePath: String,
        maxDimension: Int = 256,
        quality: Int = 70
    ): String? {
        val thumbsDir = File(context.filesDir, "thumbs").apply {
            if (!exists()) mkdirs()
        }
        val sourceFile = File(imagePath)
        val thumbFile = File(thumbsDir, sourceFile.name)

        return if (saveResizedJpeg(imagePath, thumbFile, maxDimension, quality)) {
            thumbFile.absolutePath
        } else {
            null
        }
    }

    /**
     * Devolve o caminho da miniatura se existir, ou o caminho original como fallback.
     */
    fun getThumbnailOrOriginal(context: Context, imagePath: String): String {
        val thumbFile = File(File(context.filesDir, "thumbs"), File(imagePath).name)
        return if (thumbFile.exists()) thumbFile.absolutePath else imagePath
    }

    private fun saveResizedJpeg(
        sourcePath: String,
        destinationFile: File,
        maxDimension: Int,
        quality: Int
    ): Boolean {
        return try {
            val sampled = decodeSampledBitmap(sourcePath, maxDimension) ?: return false
            val scaled = scaleToMaxDimension(sampled, maxDimension)
            if (scaled != sampled) {
                sampled.recycle()
            }

            FileOutputStream(destinationFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            scaled.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun decodeSampledBitmap(filePath: String, maxDimension: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)

        options.inSampleSize = calculateInSampleSize(options, maxDimension)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(filePath, options)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        maxDimension: Int
    ): Int {
        val largestSide = max(options.outWidth, options.outHeight)
        var inSampleSize = 1
        while (largestSide / (inSampleSize * 2) >= maxDimension) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun scaleToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largestSide = max(bitmap.width, bitmap.height)
        if (largestSide <= maxDimension) return bitmap

        val ratio = maxDimension.toFloat() / largestSide.toFloat()
        val newWidth = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
