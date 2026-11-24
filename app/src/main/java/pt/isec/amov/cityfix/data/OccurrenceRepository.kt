package pt.isec.amov.cityfix.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import pt.isec.amov.cityfix.model.Message
import pt.isec.amov.cityfix.model.Occurrence
import pt.isec.amov.cityfix.model.OccurrencesList
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OccurrenceRepository(private val context: Context) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val databaseFile by lazy { File(context.filesDir, "occurrences_database.json") }
    private var cachedList: OccurrencesList? = null

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val occurrencesCollection by lazy { db.collection("occurrences") }
    private val messagesCollection by lazy { db.collection("messages") }

    fun getOccurrenceById(id: String): Occurrence? = cachedList?.findById(id)

    fun observeOccurrences(): Flow<List<Occurrence>> = callbackFlow {
        val registration = occurrencesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("AMOV FIRESTORE", "Listener error", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val remoteList = snapshot.toObjects(Occurrence::class.java)
                val updated = OccurrencesList(remoteList)
                cachedList = updated
                databaseFile.writeText(json.encodeToString(remoteList))
                trySend(remoteList)
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeMessages(): Flow<Map<String, List<Message>>> = callbackFlow {
        val registration = messagesCollection
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AMOV FIRESTORE", "Messages listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val grouped = snapshot.toObjects(Message::class.java)
                        .groupBy { it.occurrenceId }
                    trySend(grouped)
                }
            }
        awaitClose { registration.remove() }
    }

    fun saveMessage(message: Message) {
        messagesCollection.document(message.id).set(message)
            .addOnFailureListener { e ->
                Log.e("AMOV FIRESTORE", "Error saving message ${message.id}", e)
            }
    }

    suspend fun loadOccurrences(): OccurrencesList = withContext(Dispatchers.IO) {
        try {
            val remoteList = fetchFromFirestore()
            val list = OccurrencesList(remoteList)
            databaseFile.writeText(json.encodeToString(remoteList))
            cachedList = list
            list
        } catch (e: Exception) {
            Log.e("AMOV FIRESTORE", "Falling back to local cache", e)
            val fallback = loadLocalCache()
            cachedList = fallback
            fallback
        }
    }

    suspend fun saveOccurrences(list: OccurrencesList) = withContext(Dispatchers.IO) {
        cachedList = list
        databaseFile.writeText(json.encodeToString(list.rawList()))
        list.rawList().forEach { occ ->
            occurrencesCollection.document(occ.id).set(occ)
                .addOnFailureListener { e ->
                    Log.e("AMOV FIRESTORE", "Error saving occurrence ${occ.id}", e)
                }
        }
    }

    private fun loadLocalCache(): OccurrencesList {
        cachedList?.let { return it }
        if (!databaseFile.exists()) return OccurrencesList()
        return try {
            OccurrencesList(json.decodeFromString<List<Occurrence>>(databaseFile.readText()))
        } catch (_: Exception) {
            OccurrencesList()
        }
    }

    private suspend fun fetchFromFirestore(): List<Occurrence> =
        suspendCancellableCoroutine { continuation ->
            occurrencesCollection.get()
                .addOnSuccessListener { result ->
                    if (continuation.isActive) {
                        continuation.resume(result.toObjects(Occurrence::class.java))
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
        }
}