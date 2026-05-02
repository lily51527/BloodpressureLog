package idv.wennyli.bloodpressurelog.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

    private fun recordsCollection() = firestore.collection(
        FirestorePaths.bloodPressures(BuildConfig.APP_ID, requireUserId())
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: error("User not authenticated")

    override fun observeRecords(): Flow<DataState<List<BloodPressureRecord>>> = callbackFlow {
        trySend(DataState.Loading)
        val subscription = recordsCollection()
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

    override suspend fun addRecord(record: BloodPressureRecord): DataState<Unit> =
        suspendCancellableCoroutine { continuation ->
            val now = System.currentTimeMillis()
            recordsCollection()
                .add(record.toMap(createdAt = now, updatedAt = now))
                .addOnSuccessListener { continuation.resume(DataState.Success(Unit)) }
                .addOnFailureListener { e ->
                    continuation.resume(DataState.Error(e, e.message ?: "Failed to add record"))
                }
        }

    override suspend fun updateRecord(record: BloodPressureRecord): DataState<Unit> =
        suspendCancellableCoroutine { continuation ->
            recordsCollection()
                .document(record.id)
                .set(record.toMap(updatedAt = System.currentTimeMillis()))
                .addOnSuccessListener { continuation.resume(DataState.Success(Unit)) }
                .addOnFailureListener { e ->
                    continuation.resume(DataState.Error(e, e.message ?: "Failed to update record"))
                }
        }

    override suspend fun deleteRecord(id: String): DataState<Unit> =
        suspendCancellableCoroutine { continuation ->
            recordsCollection()
                .document(id)
                .delete()
                .addOnSuccessListener { continuation.resume(DataState.Success(Unit)) }
                .addOnFailureListener { e ->
                    continuation.resume(DataState.Error(e, e.message ?: "Failed to delete record"))
                }
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

private fun BloodPressureRecord.toMap(
    createdAt: Long = this.createdAt,
    updatedAt: Long = this.updatedAt,
): Map<String, Any> = mapOf(
    "systolic" to systolic,
    "diastolic" to diastolic,
    "pulse" to pulse,
    "note" to note,
    "recordedAt" to recordedAt.toTimestamp(),
    "createdAt" to createdAt.toTimestamp(),
    "updatedAt" to updatedAt.toTimestamp(),
)

private fun Long.toTimestamp() = Timestamp(this / 1000, ((this % 1000) * 1_000_000).toInt())

private fun Timestamp.toEpochMillis() = seconds * 1000 + nanoseconds / 1_000_000
