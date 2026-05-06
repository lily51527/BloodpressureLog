package idv.wennyli.bloodpressurelog.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.utils.toChineseAuthErrorMessage
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
                    continuation.resume(DataState.Error(e, e.toChineseAuthErrorMessage()))
                }
        }

    override suspend fun signInAnonymously(): DataState<Unit> =
        suspendCancellableCoroutine { continuation ->
            auth.signInAnonymously()
                .addOnSuccessListener { continuation.resume(DataState.Success(Unit)) }
                .addOnFailureListener { e ->
                    continuation.resume(DataState.Error(e, e.toChineseAuthErrorMessage()))
                }
        }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun registerWithEmail(email: String, password: String): DataState<Unit> =
        suspendCancellableCoroutine { continuation ->
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    auth.currentUser?.sendEmailVerification()
                    continuation.resume(DataState.Success(Unit))
                }
                .addOnFailureListener { e ->
                    continuation.resume(DataState.Error(e, e.toChineseAuthErrorMessage()))
                }
        }

    override suspend fun sendEmailVerification(): DataState<Unit> {
        val user = auth.currentUser
            ?: return DataState.Error(IllegalStateException("No user"), "操作失敗，請稍後再試")
        return suspendCancellableCoroutine { continuation ->
            user.sendEmailVerification()
                .addOnSuccessListener { continuation.resume(DataState.Success(Unit)) }
                .addOnFailureListener { e ->
                    continuation.resume(
                        DataState.Error(
                            e,
                            e.toChineseAuthErrorMessage()
                        )
                    )
                }
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): DataState<Unit> =
        suspendCancellableCoroutine { continuation ->
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener { continuation.resume(DataState.Success(Unit)) }
                .addOnFailureListener { e ->
                    continuation.resume(
                        DataState.Error(
                            e,
                            e.toChineseAuthErrorMessage()
                        )
                    )
                }
        }
}
