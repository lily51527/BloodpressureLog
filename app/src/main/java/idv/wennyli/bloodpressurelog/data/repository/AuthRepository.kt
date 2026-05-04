package idv.wennyli.bloodpressurelog.data.repository

import com.google.firebase.auth.FirebaseUser
import idv.wennyli.bloodpressurelog.data.model.DataState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: FirebaseUser?
    val authStateChanges: Flow<FirebaseUser?>
    suspend fun signInWithEmail(email: String, password: String): DataState<Unit>
    suspend fun signInAnonymously(): DataState<Unit>
    suspend fun signOut()
}
