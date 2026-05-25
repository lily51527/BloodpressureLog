package idv.wennyli.bloodpressurelog.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.data.model.AuthException
import idv.wennyli.bloodpressurelog.utils.ResourceProvider
import idv.wennyli.bloodpressurelog.utils.toAuthErrorStringRes
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val resourceProvider: ResourceProvider,
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override val authStateChanges: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String) {
        try {
            auth.signInWithEmailAndPassword(email, password).await()
        } catch (e: Exception) {
            Timber.e(e, "[AuthRepositoryImpl] signInWithEmail failed")
            throw AuthException(resourceProvider.getString(e.toAuthErrorStringRes()), e)
        }
    }

    override suspend fun signInAnonymously() {
        try {
            auth.signInAnonymously().await()
        } catch (e: Exception) {
            Timber.e(e, "[AuthRepositoryImpl] signInAnonymously failed")
            throw AuthException(resourceProvider.getString(e.toAuthErrorStringRes()), e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun registerWithEmail(email: String, password: String) {
        try {
            auth.createUserWithEmailAndPassword(email, password).await()
            auth.currentUser?.sendEmailVerification()
        } catch (e: Exception) {
            Timber.e(e, "[AuthRepositoryImpl] registerWithEmail failed")
            throw AuthException(resourceProvider.getString(e.toAuthErrorStringRes()), e)
        }
    }

    override suspend fun sendEmailVerification() {
        val user = auth.currentUser
            ?: throw AuthException(resourceProvider.getString(R.string.error_auth_generic))
        try {
            user.sendEmailVerification().await()
        } catch (e: Exception) {
            Timber.e(e, "[AuthRepositoryImpl] sendEmailVerification failed")
            throw AuthException(resourceProvider.getString(e.toAuthErrorStringRes()), e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        try {
            auth.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            Timber.e(e, "[AuthRepositoryImpl] sendPasswordResetEmail failed")
            throw AuthException(resourceProvider.getString(e.toAuthErrorStringRes()), e)
        }
    }
}
