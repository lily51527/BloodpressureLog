package idv.wennyli.bloodpressurelog.data.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: FirebaseUser?
    val authStateChanges: Flow<FirebaseUser?>
    suspend fun signInWithEmail(email: String, password: String)
    suspend fun signInAnonymously()
    suspend fun signOut()
    suspend fun registerWithEmail(email: String, password: String)
    suspend fun sendEmailVerification()
    suspend fun sendPasswordResetEmail(email: String)
}
