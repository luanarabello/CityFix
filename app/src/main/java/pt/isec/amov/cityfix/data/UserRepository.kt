package pt.isec.amov.cityfix.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.model.UsersList
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserRepository(private val context: Context) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val databaseFile by lazy { File(context.filesDir, "users_database.json") }
    private var cachedList: UsersList? = null

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val usersCollection by lazy { db.collection("users") }

    fun getUserById(id: String): User? = cachedList?.findById(id)

    private fun defaultAdminUser(): User = User(
        id = "admin_01",
        name = "Operador CityFix",
        email = "admin@isec.pt",
        phone = "239790200",
        nif = "500000000",
        profileType = UserRole.OPERADOR,
        active = true
    )

    suspend fun loadUsers(): UsersList = withContext(Dispatchers.IO) {
        try {
            val remoteList = fetchFromFirestore()
            val list = if (remoteList.isEmpty()) {
                val admin = defaultAdminUser()
                usersCollection.document(admin.id).set(admin)
                UsersList(listOf(admin))
            } else {
                UsersList(remoteList)
            }
            databaseFile.writeText(json.encodeToString(list.rawList()))
            cachedList = list
            list
        } catch (e: Exception) {
            Log.e("AMOV FIRESTORE", "Users falling back to local cache", e)
            val fallback = loadLocalCache()
            cachedList = fallback
            fallback
        }
    }

    suspend fun saveUsers(list: UsersList) = withContext(Dispatchers.IO) {
        cachedList = list
        databaseFile.writeText(json.encodeToString(list.rawList()))
        list.rawList().forEach { user ->
            try {
                saveUserToFirestore(user)
            } catch (e: Exception) {
                Log.e("AMOV FIRESTORE", "Error saving user ${user.id}", e)
            }
        }
    }

    private suspend fun saveUserToFirestore(user: User) =
        suspendCancellableCoroutine { continuation ->
            usersCollection.document(user.id).set(user)
                .addOnSuccessListener {
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
        }

    private fun loadLocalCache(): UsersList {
        cachedList?.let { return it }
        if (!databaseFile.exists()) return UsersList(listOf(defaultAdminUser()))
        return try {
            UsersList(json.decodeFromString<List<User>>(databaseFile.readText()))
        } catch (_: Exception) {
            UsersList(listOf(defaultAdminUser()))
        }
    }

    private suspend fun fetchFromFirestore(): List<User> =
        suspendCancellableCoroutine { continuation ->
            usersCollection.get()
                .addOnSuccessListener { result ->
                    if (continuation.isActive) {
                        continuation.resume(result.toObjects(User::class.java))
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
        }
}