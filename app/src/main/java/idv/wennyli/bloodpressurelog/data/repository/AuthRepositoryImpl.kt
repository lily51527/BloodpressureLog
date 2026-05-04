package idv.wennyli.bloodpressurelog.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import idv.wennyli.bloodpressurelog.data.model.DataState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override val authStateChanges: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String): DataState<Unit> =
        suspendCancellableCoroutine { continuation ->
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { continuation.resume(DataState.Success(Unit)) }
                .addOnFailureListener { e ->
                    continuation.resume(DataState.Error(e, e.message ?: "Sign in failed"))
                }
        }

    override suspend fun signInAnonymously(): DataState<Unit> =
        suspendCancellableCoroutine { continuation ->
            auth.signInAnonymously()
                .addOnSuccessListener { continuation.resume(DataState.Success(Unit)) }
                .addOnFailureListener { e ->
                    continuation.resume(DataState.Error(e, e.message ?: "Anonymous sign in failed"))
                }
        }

    override suspend fun signOut() {
        auth.signOut()
    }
}
