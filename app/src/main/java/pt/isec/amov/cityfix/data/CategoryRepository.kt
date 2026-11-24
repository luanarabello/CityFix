package pt.isec.amov.cityfix.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import pt.isec.amov.cityfix.model.CategoriesList
import pt.isec.amov.cityfix.model.Category
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CategoryRepository(private val context: Context) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val databaseFile by lazy { File(context.filesDir, "categories_database.json") }
    private var cachedList: CategoriesList? = null

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val categoriesCollection by lazy { db.collection("categories") }

    fun getCategoryById(id: String): Category? = cachedList?.findById(id)

    suspend fun loadCategories(): CategoriesList = withContext(Dispatchers.IO) {
        try {
            val remoteList = fetchFromFirestore()
            val list = CategoriesList(remoteList)
            databaseFile.writeText(json.encodeToString(remoteList))
            cachedList = list
            list
        } catch (e: Exception) {
            Log.e("AMOV FIRESTORE", "Categories falling back to local cache", e)
            val fallback = loadLocalCache()
            cachedList = fallback
            fallback
        }
    }

    suspend fun saveCategories(list: CategoriesList) = withContext(Dispatchers.IO) {
        cachedList = list
        databaseFile.writeText(json.encodeToString(list.rawList()))
        list.rawList().forEach { cat ->
            categoriesCollection.document(cat.id).set(cat)
                .addOnFailureListener { e ->
                    Log.e("AMOV FIRESTORE", "Error saving category ${cat.id}", e)
                }
        }
    }

    private fun loadLocalCache(): CategoriesList {
        cachedList?.let { return it }
        if (!databaseFile.exists()) return CategoriesList()
        return try {
            CategoriesList(json.decodeFromString<List<Category>>(databaseFile.readText()))
        } catch (_: Exception) {
            CategoriesList()
        }
    }

    private suspend fun fetchFromFirestore(): List<Category> =
        suspendCancellableCoroutine { continuation ->
            categoriesCollection.get()
                .addOnSuccessListener { result ->
                    if (continuation.isActive) {
                        continuation.resume(result.toObjects(Category::class.java))
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
        }
}