package idv.wennyli.bloodpressurelog.data.repository

import com.google.firebase.auth.FirebaseUser
import idv.wennyli.bloodpressurelog.data.model.DataState

interface AuthRepository {
    val currentUser: FirebaseUser?
    suspend fun signInWithEmail(email: String, password: String): DataState<Unit>
    suspend fun signInAnonymously(): DataState<Unit>
    suspend fun signOut()
}
