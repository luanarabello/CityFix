package pt.isec.amov.cityfix.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FileUtils {
    fun copyUriToInternalStorage(context: Context, sourceUri: Uri, isVideo: Boolean = false): String?{
        val folder = if (isVideo) "videos" else "images"
        val prefix = if (isVideo) "VID" else "IMG"
        val extension = if (isVideo) "mp4" else "jpg"

        return try {
            val dir = File(context.filesDir, folder).apply {
                if (!exists()) mkdirs()
            }
            val destinationFile = File(dir, "${prefix}_${UUID.randomUUID()}.$extension")
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            val path = destinationFile.absolutePath
            if (!isVideo) {
                ImageUtils.compressImage(path)
                ImageUtils.createThumbnail(context, path)
            }
            path
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createTempMediaFile(context: Context, isVideo: Boolean = false): Pair<File, Uri> {
        val tempFile: File
        val uri: Uri
        val prefix = if(isVideo) "VIDEO" else "CAPTURE"
        val extension = if(isVideo) "mp4" else "jpg"
        val folder = if(isVideo) "videos" else "images"

        val dir = File(context.filesDir, folder).apply {
            if (!exists()) mkdirs()
        }
        tempFile = File(dir, "${prefix}_${System.currentTimeMillis()}.${extension}")
        uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return Pair(tempFile, uri)
    }

    fun deleteFileFromPath(path: String?): Boolean{
        if(path.isNullOrBlank()) return false
        return try{
            val file = File(path)
            if(file.exists()){
                file.delete()
            }else{
                false
            }
        }catch (e: Exception){
            e.printStackTrace()
            false
        }
    }
}