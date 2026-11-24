package pt.isec.amov.cityfix.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class AuthUser(
    val id: String,
    val email: String?,
    val isEmailVerified: Boolean = false
)

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: AuthUser?
        get() = firebaseAuth.currentUser?.toAuthUser()

    suspend fun signIn(email: String, password: String): AuthUser {
        await(firebaseAuth.signInWithEmailAndPassword(email.trim(), password))
        firebaseAuth.currentUser?.reload()?.let { await(it) }
        return firebaseAuth.currentUser?.toAuthUser()
            ?: error("Firebase sign-in completed without a user.")
    }

    suspend fun createAccount(email: String, password: String): AuthUser {
        await(firebaseAuth.createUserWithEmailAndPassword(email.trim(), password))
        firebaseAuth.currentUser?.sendEmailVerification()?.let { await(it) }
        return firebaseAuth.currentUser?.toAuthUser()
            ?: error("Firebase account creation completed without a user.")
    }

    suspend fun sendPasswordResetEmail(email: String) {
        await(firebaseAuth.sendPasswordResetEmail(email.trim()))
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        id = uid,
        email = email,
        isEmailVerified = isEmailVerified
    )

    private suspend fun await(task: Task<*>) {
        suspendCancellableCoroutine { continuation ->
            task.addOnCompleteListener { completedTask ->
                if (!continuation.isActive) return@addOnCompleteListener

                val exception = completedTask.exception
                if (completedTask.isSuccessful) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(
                        exception ?: IllegalStateException("Firebase request failed.")
                    )
                }
            }
        }
    }
}
