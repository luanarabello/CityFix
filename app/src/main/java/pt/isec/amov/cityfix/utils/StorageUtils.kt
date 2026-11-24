package pt.isec.amov.cityfix.utils

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object StorageUtils {

    /**
     * Uploads a local file to Firebase Storage and returns its download URL.
     * If [pathOrUrl] is already an HTTP(S) URL or upload fails, returns [pathOrUrl] as fallback.
     */
    suspend fun uploadFileIfLocal(
        pathOrUrl: String,
        storageFolder: String
    ): String = withContext(Dispatchers.IO) {
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return@withContext pathOrUrl
        }
        val file = File(pathOrUrl)
        if (!file.exists()) {
            return@withContext pathOrUrl
        }
        try {
            val remoteRef = FirebaseStorage.getInstance().reference
                .child("$storageFolder/${UUID.randomUUID()}_${file.name}")
            uploadAndGetUrl(remoteRef, Uri.fromFile(file))
        } catch (e: Exception) {
            Log.e("AMOV STORAGE", "Upload failed for $pathOrUrl; keeping local path", e)
            pathOrUrl
        }
    }

    suspend fun uploadFilesIfLocal(
        paths: List<String>,
        storageFolder: String
    ): List<String> = paths.map { uploadFileIfLocal(it, storageFolder) }

    private suspend fun uploadAndGetUrl(
        ref: com.google.firebase.storage.StorageReference,
        uri: Uri
    ): String = suspendCancellableCoroutine { continuation ->
        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        if (continuation.isActive) {
                            continuation.resume(downloadUri.toString())
                        }
                    }
                    .addOnFailureListener { exception ->
                        if (continuation.isActive) {
                            continuation.resumeWithException(exception)
                        }
                    }
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }
    }
}
