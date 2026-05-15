package idv.wennyli.bloodpressurelog.data.repository

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isEmpty
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class BloodPressureRepositoryImplTest {

    private val mockFirestore = mockk<FirebaseFirestore>()
    private val mockAuth = mockk<FirebaseAuth>()
    private val mockUser = mockk<FirebaseUser>()
    private val mockCollection = mockk<CollectionReference>()
    private val mockQuery = mockk<Query>()
    private val mockListenerRegistration = mockk<ListenerRegistration>(relaxed = true)
    private val mockDocumentRef = mockk<DocumentReference>()

    private lateinit var repository: BloodPressureRepositoryImpl

    @BeforeTest
    fun setUp() {
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "uid-123"
        every { mockFirestore.collection(any()) } returns mockCollection
        every { mockCollection.orderBy(any<String>(), any()) } returns mockQuery
        every { mockCollection.document(any()) } returns mockDocumentRef

        repository = BloodPressureRepositoryImpl(mockFirestore, mockAuth)
    }

    // region observeRecords

    /** observeRecords 應先發射 Loading，再發射包含正確轉換紀錄的 Success。 */
    @Test
    fun `observeRecords emits Loading then Success with mapped records`() = runTest {
        val snapshot = mockk<QuerySnapshot>()
        every { snapshot.documents } returns listOf(buildMockDocument())
        deliverSnapshot(snapshot, null)

        repository.observeRecords().test {
            assertThat(awaitItem()).isInstanceOf(DataState.Loading::class)
            val success = awaitItem() as DataState.Success<*>
            @Suppress("UNCHECKED_CAST")
            val records = success.data as List<BloodPressureRecord>
            assertThat(records).hasSize(1)
            assertThat(records[0].id).isEqualTo("doc-1")
            assertThat(records[0].systolic).isEqualTo(120)
            assertThat(records[0].diastolic).isEqualTo(80)
            assertThat(records[0].pulse).isEqualTo(70)
            assertThat(records[0].note).isEqualTo("note")
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Firestore 快照為空時，observeRecords 應發射空列表的 Success 狀態。 */
    @Test
    fun `observeRecords emits Loading then empty Success when snapshot is empty`() = runTest {
        val snapshot = mockk<QuerySnapshot>()
        every { snapshot.documents } returns emptyList()
        deliverSnapshot(snapshot, null)

        repository.observeRecords().test {
            assertThat(awaitItem()).isInstanceOf(DataState.Loading::class)
            val success = awaitItem() as DataState.Success<*>
            @Suppress("UNCHECKED_CAST")
            assertThat(success.data as List<*>).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** 缺少必要欄位的 Firestore 文件，observeRecords 應略過並回傳空列表。 */
    @Test
    fun `observeRecords skips documents with missing required fields`() = runTest {
        val snapshot = mockk<QuerySnapshot>()
        val invalidDoc = mockk<DocumentSnapshot>()
        every { invalidDoc.id } returns "invalid"
        every { invalidDoc.getLong("systolic") } returns null
        every { snapshot.documents } returns listOf(invalidDoc)
        deliverSnapshot(snapshot, null)

        repository.observeRecords().test {
            assertThat(awaitItem()).isInstanceOf(DataState.Loading::class)
            val success = awaitItem() as DataState.Success<*>
            @Suppress("UNCHECKED_CAST")
            assertThat(success.data as List<*>).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Firestore 發生錯誤時，observeRecords 應在 Loading 後發射 Error 狀態並附帶錯誤資訊。 */
    @Test
    fun `observeRecords emits Loading then Error on Firestore error`() = runTest {
        val exception = mockk<FirebaseFirestoreException>()
        every { exception.message } returns "Firestore error"
        deliverSnapshot(null, exception)

        repository.observeRecords().test {
            assertThat(awaitItem()).isInstanceOf(DataState.Loading::class)
            val error = awaitItem() as DataState.Error
            assertThat(error.throwable).isEqualTo(exception)
            assertThat(error.message).isEqualTo("Firestore error")
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Flow 被取消時，observeRecords 應移除 Firestore 監聽器以避免資源洩漏。 */
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

    /** observeRecords 應查詢正確的 Firestore 集合路徑以存取對應使用者的紀錄。 */
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

    /** 文件存在時，getRecord 應正確取得並回傳轉換後的血壓紀錄。 */
    @Test
    fun `getRecord returns record when document exists`() = runTest {
        val mockDoc = buildMockDocument("doc-1")
        val task = buildSuccessTask<DocumentSnapshot>(mockDoc)
        every { mockDocumentRef.get() } returns task

        val result = repository.getRecord("doc-1")

        assertThat(result?.id).isEqualTo("doc-1")
        assertThat(result?.systolic).isEqualTo(120)
        assertThat(result?.diastolic).isEqualTo(80)
    }

    /** 文件欄位不完整無法轉換時，getRecord 應回傳 null。 */
    @Test
    fun `getRecord returns null when document cannot be mapped`() = runTest {
        val invalidDoc = mockk<DocumentSnapshot>()
        every { invalidDoc.id } returns "invalid"
        every { invalidDoc.getLong("systolic") } returns null
        val task = buildSuccessTask<DocumentSnapshot>(invalidDoc)
        every { mockDocumentRef.get() } returns task

        val result = repository.getRecord("invalid")

        assertThat(result).isNull()
    }

    /** Firestore 讀取失敗時，getRecord 應拋出對應的例外。 */
    @Test
    fun `getRecord throws on Firestore failure`() = runTest {
        val exception = RuntimeException("get failed")
        val task = buildFailureTask<DocumentSnapshot>(exception)
        every { mockDocumentRef.get() } returns task

        val thrown = runCatching { repository.getRecord("doc-1") }.exceptionOrNull()
        assertThat(thrown?.message).isEqualTo("get failed")
    }

    // endregion

    // region addRecord

    /** Firestore 新增成功時，addRecord 應正常完成而不拋出例外。 */
    @Test
    fun `addRecord completes on Firestore success`() = runTest {
        val task = buildSuccessTask<DocumentReference>(mockDocumentRef)
        every { mockCollection.add(any()) } returns task

        repository.addRecord(sampleRecord())
    }

    /** Firestore 新增失敗時，addRecord 應拋出對應的例外。 */
    @Test
    fun `addRecord throws on Firestore failure`() = runTest {
        val exception = RuntimeException("add failed")
        val task = buildFailureTask<DocumentReference>(exception)
        every { mockCollection.add(any()) } returns task

        val thrown = runCatching { repository.addRecord(sampleRecord()) }.exceptionOrNull()
        assertThat(thrown?.message).isEqualTo("add failed")
    }

    // endregion

    // region updateRecord

    /** Firestore 更新成功時，updateRecord 應正常完成而不拋出例外。 */
    @Test
    fun `updateRecord completes on Firestore success`() = runTest {
        val task = buildSuccessTask<Void>(null)
        every { mockDocumentRef.set(any(), any()) } returns task

        repository.updateRecord(sampleRecord(id = "doc-1"))
    }

    /** Firestore 更新失敗時，updateRecord 應拋出對應的例外。 */
    @Test
    fun `updateRecord throws on Firestore failure`() = runTest {
        val exception = RuntimeException("update failed")
        val task = buildFailureTask<Void>(exception)
        every { mockDocumentRef.set(any(), any()) } returns task

        val thrown = runCatching { repository.updateRecord(sampleRecord(id = "doc-1")) }.exceptionOrNull()
        assertThat(thrown?.message).isEqualTo("update failed")
    }

    // endregion

    // region unauthenticated

    /** 使用者未登入時，observeRecords 應立即發射 Error 而非嘗試存取 Firestore。 */
    @Test
    fun `observeRecords emits Error immediately when user is null`() = runTest {
        every { mockAuth.currentUser } returns null

        repository.observeRecords().test {
            val error = awaitItem() as DataState.Error
            assertThat(error.message).isEqualTo("請重新登入")
            awaitComplete()
        }
    }

    /** 使用者未登入時，getRecord 應拋出例外要求重新登入。 */
    @Test
    fun `getRecord throws when user is null`() = runTest {
        every { mockAuth.currentUser } returns null

        val thrown = runCatching { repository.getRecord("doc-1") }.exceptionOrNull()
        assertThat(thrown?.message).isEqualTo("請重新登入")
    }

    /** 使用者未登入時，addRecord 應拋出例外要求重新登入。 */
    @Test
    fun `addRecord throws when user is null`() = runTest {
        every { mockAuth.currentUser } returns null

        val thrown = runCatching { repository.addRecord(sampleRecord()) }.exceptionOrNull()
        assertThat(thrown?.message).isEqualTo("請重新登入")
    }

    /** 使用者未登入時，updateRecord 應拋出例外要求重新登入。 */
    @Test
    fun `updateRecord throws when user is null`() = runTest {
        every { mockAuth.currentUser } returns null

        val thrown = runCatching { repository.updateRecord(sampleRecord(id = "doc-1")) }.exceptionOrNull()
        assertThat(thrown?.message).isEqualTo("請重新登入")
    }

    /** 使用者未登入時，deleteRecord 應拋出例外要求重新登入。 */
    @Test
    fun `deleteRecord throws when user is null`() = runTest {
        every { mockAuth.currentUser } returns null

        val thrown = runCatching { repository.deleteRecord("doc-1") }.exceptionOrNull()
        assertThat(thrown?.message).isEqualTo("請重新登入")
    }

    // endregion

    // region deleteRecord

    /** Firestore 刪除成功時，deleteRecord 應正常完成而不拋出例外。 */
    @Test
    fun `deleteRecord completes on Firestore success`() = runTest {
        val task = buildSuccessTask<Void>(null)
        every { mockDocumentRef.delete() } returns task

        repository.deleteRecord("doc-1")
    }

    /** Firestore 刪除失敗時，deleteRecord 應拋出對應的例外。 */
    @Test
    fun `deleteRecord throws on Firestore failure`() = runTest {
        val exception = RuntimeException("delete failed")
        val task = buildFailureTask<Void>(exception)
        every { mockDocumentRef.delete() } returns task

        val thrown = runCatching { repository.deleteRecord("doc-1") }.exceptionOrNull()
        assertThat(thrown?.message).isEqualTo("delete failed")
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

    // 模擬一個「已成功完成」的 Firebase Task。
    // await() 進入時先檢查 isComplete（fast-path）：
    //   isComplete=true  → 不掛起，直接同步回傳
    //   exception=null   → 沒有例外，表示成功
    //   isCanceled=false → 排除「Task 被取消」的情況，確認是真正成功
    //   result=value     → await() 的回傳值
    @Suppress("UNCHECKED_CAST")
    private fun <T> buildSuccessTask(value: Any?): Task<T> = mockk<Task<T>>().apply {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { exception } returns null
        every { result } returns value as T?
    }

    // 模擬一個「已失敗完成」的 Firebase Task。
    // await() 進入時：
    //   isComplete=true  → 不掛起，直接同步回傳
    //   exception≠null   → 有例外，await() 直接拋出，不再檢查 isCanceled
    private fun <T> buildFailureTask(exception: Exception): Task<T> = mockk<Task<T>>().apply {
        every { isComplete } returns true
        every { this@apply.exception } returns exception
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
