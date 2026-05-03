package idv.wennyli.bloodpressurelog.data.repository

import app.cash.turbine.test
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.DataState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BloodPressureRepositoryImplTest {

    private val mockFirestore = mockk<FirebaseFirestore>()
    private val mockAuth = mockk<FirebaseAuth>()
    private val mockUser = mockk<FirebaseUser>()
    private val mockCollection = mockk<CollectionReference>()
    private val mockQuery = mockk<Query>()
    private val mockListenerRegistration = mockk<ListenerRegistration>(relaxed = true)
    private val mockDocumentRef = mockk<DocumentReference>()

    private lateinit var repository: BloodPressureRepositoryImpl

    @Before
    fun setUp() {
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "uid-123"
        every { mockFirestore.collection(any()) } returns mockCollection
        every { mockCollection.orderBy(any<String>(), any()) } returns mockQuery
        every { mockCollection.document(any()) } returns mockDocumentRef

        repository = BloodPressureRepositoryImpl(mockFirestore, mockAuth)
    }

    // region observeRecords

    @Test
    fun `observeRecords emits Loading then Success with mapped records`() = runTest {
        val snapshot = mockk<QuerySnapshot>()
        every { snapshot.documents } returns listOf(buildMockDocument())
        deliverSnapshot(snapshot, null)

        repository.observeRecords().test {
            assertTrue(awaitItem() is DataState.Loading)
            val success = awaitItem() as DataState.Success<*>
            @Suppress("UNCHECKED_CAST")
            val records = success.data as List<BloodPressureRecord>
            assertEquals(1, records.size)
            assertEquals("doc-1", records[0].id)
            assertEquals(120, records[0].systolic)
            assertEquals(80, records[0].diastolic)
            assertEquals(70, records[0].pulse)
            assertEquals("note", records[0].note)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeRecords emits Loading then empty Success when snapshot is empty`() = runTest {
        val snapshot = mockk<QuerySnapshot>()
        every { snapshot.documents } returns emptyList()
        deliverSnapshot(snapshot, null)

        repository.observeRecords().test {
            assertTrue(awaitItem() is DataState.Loading)
            val success = awaitItem() as DataState.Success<*>
            @Suppress("UNCHECKED_CAST")
            assertTrue((success.data as List<*>).isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeRecords skips documents with missing required fields`() = runTest {
        val snapshot = mockk<QuerySnapshot>()
        val invalidDoc = mockk<DocumentSnapshot>()
        every { invalidDoc.id } returns "invalid"
        every { invalidDoc.getLong("systolic") } returns null
        every { snapshot.documents } returns listOf(invalidDoc)
        deliverSnapshot(snapshot, null)

        repository.observeRecords().test {
            assertTrue(awaitItem() is DataState.Loading)
            val success = awaitItem() as DataState.Success<*>
            @Suppress("UNCHECKED_CAST")
            assertTrue((success.data as List<*>).isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeRecords emits Loading then Error on Firestore error`() = runTest {
        val exception = mockk<FirebaseFirestoreException>()
        every { exception.message } returns "Firestore error"
        deliverSnapshot(null, exception)

        repository.observeRecords().test {
            assertTrue(awaitItem() is DataState.Loading)
            val error = awaitItem() as DataState.Error
            assertEquals(exception, error.throwable)
            assertEquals("Firestore error", error.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeRecords removes listener when flow is cancelled`() = runTest {
        val snapshot = mockk<QuerySnapshot>()
        every { snapshot.documents } returns emptyList()
        deliverSnapshot(snapshot, null)

        repository.observeRecords().test {
            awaitItem()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        verify { mockListenerRegistration.remove() }
    }

    @Test
    fun `observeRecords queries correct Firestore path`() = runTest {
        val snapshot = mockk<QuerySnapshot>()
        every { snapshot.documents } returns emptyList()
        deliverSnapshot(snapshot, null)

        repository.observeRecords().test {
            cancelAndIgnoreRemainingEvents()
        }

        verify { mockFirestore.collection("artifacts/bloodpressurelog_debug/users/uid-123/bloodPressures") }
    }

    // endregion

    // region getRecord

    @Test
    fun `getRecord returns Success with record when document exists`() = runTest {
        val mockDoc = buildMockDocument("doc-1")
        val task = buildSuccessTask<DocumentSnapshot>(mockDoc)
        every { mockDocumentRef.get() } returns task

        val result = repository.getRecord("doc-1")

        assertTrue(result is DataState.Success)
        val record = (result as DataState.Success<*>).data as BloodPressureRecord?
        assertEquals("doc-1", record?.id)
        assertEquals(120, record?.systolic)
        assertEquals(80, record?.diastolic)
    }

    @Test
    fun `getRecord returns Success with null when document cannot be mapped`() = runTest {
        val invalidDoc = mockk<DocumentSnapshot>()
        every { invalidDoc.id } returns "invalid"
        every { invalidDoc.getLong("systolic") } returns null
        val task = buildSuccessTask<DocumentSnapshot>(invalidDoc)
        every { mockDocumentRef.get() } returns task

        val result = repository.getRecord("invalid")

        assertTrue(result is DataState.Success)
        assertNull((result as DataState.Success<*>).data)
    }

    @Test
    fun `getRecord returns Error on Firestore failure`() = runTest {
        val exception = RuntimeException("get failed")
        val task = buildFailureTask<DocumentSnapshot>(exception)
        every { mockDocumentRef.get() } returns task

        val result = repository.getRecord("doc-1")

        assertTrue(result is DataState.Error)
        assertEquals("get failed", (result as DataState.Error).message)
    }

    // endregion

    // region addRecord

    @Test
    fun `addRecord returns Success on Firestore success`() = runTest {
        val task = buildSuccessTask<DocumentReference>(mockDocumentRef)
        every { mockCollection.add(any()) } returns task

        val result = repository.addRecord(sampleRecord())

        assertTrue(result is DataState.Success)
    }

    @Test
    fun `addRecord returns Error on Firestore failure`() = runTest {
        val exception = RuntimeException("add failed")
        val task = buildFailureTask<DocumentReference>(exception)
        every { mockCollection.add(any()) } returns task

        val result = repository.addRecord(sampleRecord())

        assertTrue(result is DataState.Error)
        assertEquals("add failed", (result as DataState.Error).message)
    }

    // endregion

    // region updateRecord

    @Test
    fun `updateRecord returns Success on Firestore success`() = runTest {
        val task = buildSuccessTask<Void>(null)
        every { mockDocumentRef.set(any()) } returns task

        val result = repository.updateRecord(sampleRecord(id = "doc-1"))

        assertTrue(result is DataState.Success)
    }

    @Test
    fun `updateRecord returns Error on Firestore failure`() = runTest {
        val exception = RuntimeException("update failed")
        val task = buildFailureTask<Void>(exception)
        every { mockDocumentRef.set(any()) } returns task

        val result = repository.updateRecord(sampleRecord(id = "doc-1"))

        assertTrue(result is DataState.Error)
        assertEquals("update failed", (result as DataState.Error).message)
    }

    // endregion

    // region deleteRecord

    @Test
    fun `deleteRecord returns Success on Firestore success`() = runTest {
        val task = buildSuccessTask<Void>(null)
        every { mockDocumentRef.delete() } returns task

        val result = repository.deleteRecord("doc-1")

        assertTrue(result is DataState.Success)
    }

    @Test
    fun `deleteRecord returns Error on Firestore failure`() = runTest {
        val exception = RuntimeException("delete failed")
        val task = buildFailureTask<Void>(exception)
        every { mockDocumentRef.delete() } returns task

        val result = repository.deleteRecord("doc-1")

        assertTrue(result is DataState.Error)
        assertEquals("delete failed", (result as DataState.Error).message)
    }

    // endregion

    // region helpers

    private fun deliverSnapshot(snapshot: QuerySnapshot?, error: FirebaseFirestoreException?) {
        every { mockQuery.addSnapshotListener(any<EventListener<QuerySnapshot>>()) } answers {
            firstArg<EventListener<QuerySnapshot>>().onEvent(snapshot, error)
            mockListenerRegistration
        }
    }

    private fun buildMockDocument(id: String = "doc-1"): DocumentSnapshot {
        val timestamp = Timestamp(1_000_000L, 0)
        return mockk<DocumentSnapshot>().apply {
            every { this@apply.id } returns id
            every { getLong("systolic") } returns 120L
            every { getLong("diastolic") } returns 80L
            every { getLong("pulse") } returns 70L
            every { getString("note") } returns "note"
            every { getTimestamp("recordedAt") } returns timestamp
            every { getTimestamp("createdAt") } returns timestamp
            every { getTimestamp("updatedAt") } returns timestamp
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> buildSuccessTask(value: Any?): Task<T> = mockk<Task<T>>().apply {
        every { addOnSuccessListener(any()) } answers {
            (firstArg() as OnSuccessListener<Any?>).onSuccess(value)
            this@apply
        }
        every { addOnFailureListener(any()) } returns this@apply
    }

    private fun <T> buildFailureTask(exception: Exception): Task<T> = mockk<Task<T>>().apply {
        every { addOnSuccessListener(any()) } returns this@apply
        every { addOnFailureListener(any()) } answers {
            firstArg<OnFailureListener>().onFailure(exception)
            this@apply
        }
    }

    private fun sampleRecord(id: String = "") = BloodPressureRecord(
        id = id,
        systolic = 120,
        diastolic = 80,
        pulse = 70,
        recordedAt = 1_000_000_000L,
    )

    // endregion
}
