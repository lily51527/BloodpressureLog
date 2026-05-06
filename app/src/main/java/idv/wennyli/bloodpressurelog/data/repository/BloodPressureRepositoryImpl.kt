package idv.wennyli.bloodpressurelog.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import idv.wennyli.bloodpressurelog.BuildConfig
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.utils.FirestorePaths
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class BloodPressureRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : BloodPressureRepository {

    private fun recordsCollection() = auth.currentUser?.uid?.let { uid ->
        firestore.collection(FirestorePaths.bloodPressures(BuildConfig.APP_ID, uid))
    }

    override fun observeRecords(): Flow<DataState<List<BloodPressureRecord>>> = callbackFlow {
        val collection = recordsCollection()
        if (collection == null) {
            trySend(
                DataState.Error(
                    IllegalStateException("User not authenticated"),
                    NOT_AUTHENTICATED_MSG
                )
            )
            close()
            return@callbackFlow
        }
        trySend(DataState.Loading)
        val subscription = collection
            .orderBy("recordedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DataState.Error(error, error.message ?: "Unknown error"))
                    return@addSnapshotListener
                }
                val records = snapshot?.documents?.mapNotNull { it.toBloodPressureRecord() }
                    ?: emptyList()
                trySend(DataState.Success(records))
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun getRecord(id: String): DataState<BloodPressureRecord?> {
        val collection = recordsCollection()
            ?: return DataState.Error(
                IllegalStateException("User not authenticated"),
                NOT_AUTHENTICATED_MSG
            )
        return suspendCancellableCoroutine { continuation ->
            collection.document(id)
                .get()
                .addOnSuccessListener { document ->
                    continuation.resume(DataState.Success(document.toBloodPressureRecord()))
                }
                .addOnFailureListener { e ->
                    continuation.resume(DataState.Error(e, e.message ?: "Failed to get record"))
                }
        }
    }

    override suspend fun addRecord(record: BloodPressureRecord): DataState<Unit> {
        val collection = recordsCollection()
            ?: return DataState.Error(
                IllegalStateException("User not authenticated"),
                NOT_AUTHENTICATED_MSG
            )
        val now = System.currentTimeMillis()
        return suspendCancellableCoroutine { continuation ->
            collection.add(record.toAddMap(createdAt = now, updatedAt = now))
                .addOnSuccessListener { continuation.resume(DataState.Success(Unit)) }
                .addOnFailureListener { e ->
                    continuation.resume(DataState.Error(e, e.message ?: "Failed to add record"))
                }
        }
    }

    override suspend fun updateRecord(record: BloodPressureRecord): DataState<Unit> {
        val collection = recordsCollection()
            ?: return DataState.Error(
                IllegalStateException("User not authenticated"),
                NOT_AUTHENTICATED_MSG
            )
        return suspendCancellableCoroutine { continuation ->
            collection.document(record.id)
                .set(record.toUpdateMap(updatedAt = System.currentTimeMillis()), SetOptions.merge())
                .addOnSuccessListener { continuation.resume(DataState.Success(Unit)) }
                .addOnFailureListener { e ->
                    continuation.resume(DataState.Error(e, e.message ?: "Failed to update record"))
                }
        }
    }

    override suspend fun deleteRecord(id: String): DataState<Unit> {
        val collection = recordsCollection()
            ?: return DataState.Error(
                IllegalStateException("User not authenticated"),
                NOT_AUTHENTICATED_MSG
            )
        return suspendCancellableCoroutine { continuation ->
            collection.document(id)
                .delete()
                .addOnSuccessListener { continuation.resume(DataState.Success(Unit)) }
                .addOnFailureListener { e ->
                    continuation.resume(DataState.Error(e, e.message ?: "Failed to delete record"))
                }
        }
    }

    companion object {
        private const val NOT_AUTHENTICATED_MSG = "請重新登入"
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toBloodPressureRecord(): BloodPressureRecord? =
    try {
        BloodPressureRecord(
            id = id,
            systolic = getLong("systolic")?.toInt() ?: return null,
            diastolic = getLong("diastolic")?.toInt() ?: return null,
            pulse = getLong("pulse")?.toInt() ?: return null,
            note = getString("note") ?: "",
            recordedAt = getTimestamp("recordedAt")?.toEpochMillis() ?: return null,
            createdAt = getTimestamp("createdAt")?.toEpochMillis() ?: System.currentTimeMillis(),
            updatedAt = getTimestamp("updatedAt")?.toEpochMillis() ?: System.currentTimeMillis(),
        )
    } catch (e: Exception) {
        null
    }

private fun BloodPressureRecord.toAddMap(createdAt: Long, updatedAt: Long): Map<String, Any> =
    mapOf(
        "systolic" to systolic,
        "diastolic" to diastolic,
        "pulse" to pulse,
        "note" to note,
        "recordedAt" to recordedAt.toTimestamp(),
        "createdAt" to createdAt.toTimestamp(),
        "updatedAt" to updatedAt.toTimestamp(),
    )

private fun BloodPressureRecord.toUpdateMap(updatedAt: Long): Map<String, Any> = mapOf(
    "systolic" to systolic,
    "diastolic" to diastolic,
    "pulse" to pulse,
    "note" to note,
    "recordedAt" to recordedAt.toTimestamp(),
    "updatedAt" to updatedAt.toTimestamp(),
)

private fun Long.toTimestamp() = Timestamp(this / 1000, ((this % 1000) * 1_000_000).toInt())

private fun Timestamp.toEpochMillis() = seconds * 1000 + nanoseconds / 1_000_000
