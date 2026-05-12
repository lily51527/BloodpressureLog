package idv.wennyli.bloodpressurelog.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.qualifiers.ApplicationContext
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.data.model.AuthException
import idv.wennyli.bloodpressurelog.utils.toAuthErrorStringRes
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context,
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override val authStateChanges: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String) =
        suspendCancellableCoroutine<Unit> { continuation ->
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { continuation.resume(Unit) }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(
                        AuthException(context.getString(e.toAuthErrorStringRes()), e)
                    )
                }
        }

    override suspend fun signInAnonymously() =
        suspendCancellableCoroutine<Unit> { continuation ->
            auth.signInAnonymously()
                .addOnSuccessListener { continuation.resume(Unit) }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(
                        AuthException(context.getString(e.toAuthErrorStringRes()), e)
                    )
                }
        }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun registerWithEmail(email: String, password: String) =
        suspendCancellableCoroutine<Unit> { continuation ->
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    auth.currentUser?.sendEmailVerification()
                    continuation.resume(Unit)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(
                        AuthException(context.getString(e.toAuthErrorStringRes()), e)
                    )
                }
        }

    override suspend fun sendEmailVerification() {
        val user = auth.currentUser
            ?: throw AuthException(context.getString(R.string.error_auth_generic))
        suspendCancellableCoroutine<Unit> { continuation ->
            user.sendEmailVerification()
                .addOnSuccessListener { continuation.resume(Unit) }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(
                        AuthException(context.getString(e.toAuthErrorStringRes()), e)
                    )
                }
        }
    }

    override suspend fun sendPasswordResetEmail(email: String) =
        suspendCancellableCoroutine<Unit> { continuation ->
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener { continuation.resume(Unit) }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(
                        AuthException(context.getString(e.toAuthErrorStringRes()), e)
                    )
                }
        }
}
