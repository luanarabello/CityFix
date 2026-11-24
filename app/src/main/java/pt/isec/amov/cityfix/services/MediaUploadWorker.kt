package pt.isec.amov.cityfix.services

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import pt.isec.amov.cityfix.CityFixApp
import pt.isec.amov.cityfix.utils.StorageUtils

class MediaUploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val occurrenceId = inputData.getString(KEY_OCCURRENCE_ID) ?: return Result.failure()
        val repository = (applicationContext as CityFixApp).occurrenceRepository

        val list = repository.loadOccurrences()
        val occurrence = list.findById(occurrenceId) ?: return Result.success()

        val uploadedImages = StorageUtils.uploadFilesIfLocal(
            occurrence.imagePaths,
            "occurrences/images"
        )
        val uploadedVideos = StorageUtils.uploadFilesIfLocal(
            occurrence.videoPaths,
            "occurrences/videos"
        )
        val uploadedResImages = StorageUtils.uploadFilesIfLocal(
            occurrence.resolutionImagePaths,
            "occurrences/resolutions/images"
        )
        val uploadedResVideos = StorageUtils.uploadFilesIfLocal(
            occurrence.resolutionVideoPaths,
            "occurrences/resolutions/videos"
        )

        val updated = occurrence.copy(
            imagePaths = uploadedImages,
            videoPaths = uploadedVideos,
            resolutionImagePaths = uploadedResImages,
            resolutionVideoPaths = uploadedResVideos
        )
        if (updated != occurrence) {
            list.upsertOccurrence(updated)
            repository.saveOccurrences(list)
        }

        val allUploaded = (uploadedImages + uploadedVideos + uploadedResImages + uploadedResVideos)
            .all { it.startsWith("http://") || it.startsWith("https://") }

        return if (allUploaded) Result.success() else Result.retry()
    }

    companion object {
        private const val KEY_OCCURRENCE_ID = "occurrence_id"

        fun enqueue(context: Context, occurrenceId: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<MediaUploadWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_OCCURRENCE_ID to occurrenceId))
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                "media_upload_$occurrenceId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
