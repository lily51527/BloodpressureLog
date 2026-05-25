package idv.wennyli.bloodpressurelog.ui.view.record

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import idv.wennyli.bloodpressurelog.MainDispatcherRule
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.data.repository.AuthRepository
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import idv.wennyli.bloodpressurelog.utils.ResourceProvider
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class RecordListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockRepository = mockk<BloodPressureRepository>()
    private val mockAuthRepository = mockk<AuthRepository>()
    private val mockResourceProvider = mockk<ResourceProvider>()
    private lateinit var viewModel: RecordListViewModel

    private val sampleRecords = listOf(
        BloodPressureRecord(id = "1", systolic = 120, diastolic = 80, pulse = 70, recordedAt = 3000L),
        BloodPressureRecord(id = "2", systolic = 130, diastolic = 85, pulse = 75, recordedAt = 1000L),
        BloodPressureRecord(id = "3", systolic = 110, diastolic = 70, pulse = 65, recordedAt = 2000L),
    )

    @BeforeTest
    fun setUp() {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Loading)
        every { mockResourceProvider.getString(R.string.error_delete_record_failed) } returns "刪除失敗，請稍後再試"
        viewModel = RecordListViewModel(mockRepository, mockAuthRepository, mockResourceProvider)
    }

    /** 初始狀態下，應顯示載入中且紀錄列表為空。 */
    @Test
    fun `initial state has isLoading true and empty records`() {
        val state = viewModel.uiState.value
        assertThat(state.isLoading).isTrue()
        assertThat(state.records).isEmpty()
        assertThat(state.errorMessage).isNull()
    }

    /** 成功取得紀錄後，列表應依記錄時間降冪排序顯示。 */
    @Test
    fun `success state populates records sorted by recordedAt descending`() {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(sampleRecords))

        viewModel = RecordListViewModel(mockRepository, mockAuthRepository, mockResourceProvider)

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
        assertThat(state.records.map { it.id }).containsExactly("1", "3", "2")
    }

    /** 取得紀錄失敗時，應顯示錯誤訊息並清除載入狀態。 */
    @Test
    fun `error state sets errorMessage and clears isLoading`() {
        val error = RuntimeException("Firestore error")
        every { mockRepository.observeRecords() } returns flowOf(DataState.Error(error, "Firestore error"))

        viewModel = RecordListViewModel(mockRepository, mockAuthRepository, mockResourceProvider)

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isEqualTo("Firestore error")
    }

    /** 資料載入中時，isLoading 應為 true 以顯示載入指示器。 */
    @Test
    fun `loading state sets isLoading true`() {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Loading)

        viewModel = RecordListViewModel(mockRepository, mockAuthRepository, mockResourceProvider)

        assertThat(viewModel.uiState.value.isLoading).isTrue()
    }

    /** 點擊新增按鈕時，應發射 null 導航事件以開啟新增紀錄畫面。 */
    @Test
    fun `onAddRecord emits null to navigateToAddEdit`() = runTest {
        viewModel.navigateToAddEdit.test {
            viewModel.onAddRecord()
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** 點擊編輯按鈕時，應發射對應 recordId 的導航事件以開啟編輯畫面。 */
    @Test
    fun `onEditRecord emits recordId to navigateToAddEdit`() = runTest {
        viewModel.navigateToAddEdit.test {
            viewModel.onEditRecord("record-123")
            assertThat(awaitItem()).isEqualTo("record-123")
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** 刪除紀錄時，應以正確的 id 呼叫 Repository 的 deleteRecord。 */
    @Test
    fun `deleteRecord calls repository deleteRecord with correct id`() = runTest {
        coEvery { mockRepository.deleteRecord(any()) } just Runs

        viewModel.deleteRecord("record-456")

        coVerify { mockRepository.deleteRecord("record-456") }
    }

    /** 刪除紀錄失敗時，應顯示刪除失敗的錯誤訊息。 */
    @Test
    fun `deleteRecord sets errorMessage on failure`() = runTest {
        coEvery { mockRepository.deleteRecord(any()) } throws RuntimeException("Firestore error")

        viewModel.deleteRecord("record-456")

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("刪除失敗，請稍後再試")
    }

    /** 刪除紀錄成功時，不應顯示任何錯誤訊息。 */
    @Test
    fun `deleteRecord does not set errorMessage on success`() = runTest {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(emptyList()))
        viewModel = RecordListViewModel(mockRepository, mockAuthRepository, mockResourceProvider)
        coEvery { mockRepository.deleteRecord(any()) } just Runs

        viewModel.deleteRecord("record-456")

        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    /** 使用者登出時，應呼叫 AuthRepository 的 signOut 完成登出流程。 */
    @Test
    fun `signOut calls authRepository signOut`() = runTest {
        coEvery { mockAuthRepository.signOut() } just Runs

        viewModel.signOut()

        coVerify { mockAuthRepository.signOut() }
    }

    /** 取得空列表時，應顯示無資料的空狀態而非載入中或錯誤。 */
    @Test
    fun `success with empty list shows empty state`() {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(emptyList()))

        viewModel = RecordListViewModel(mockRepository, mockAuthRepository, mockResourceProvider)

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.records).isEmpty()
        assertThat(state.errorMessage).isNull()
    }

    // ── Inverse：Error 後收到 Success，errorMessage 應被清除 ────────────────────

    /**
     * Flow 先發射 Error 再發射 Success 時，
     * Success 的處理邏輯應將 errorMessage 重置為 null。
     */
    @Test
    fun `success after error clears errorMessage`() {
        val error = RuntimeException("Firestore error")
        every { mockRepository.observeRecords() } returns flowOf(
            DataState.Error(error, "Firestore error"),
            DataState.Success(sampleRecords),
        )

        viewModel = RecordListViewModel(mockRepository, mockAuthRepository, mockResourceProvider)

        val state = viewModel.uiState.value
        assertThat(state.errorMessage).isNull()
        assertThat(state.isLoading).isFalse()
    }

    // ── Cardinality：恰好 1 筆紀錄 ─────────────────────────────────────────────

    /** 取得恰好 1 筆紀錄時，列表 size 應為 1，且正確顯示該筆資料。 */
    @Test
    fun `success with single record shows one item`() {
        val singleRecord = BloodPressureRecord(
            id = "only-1",
            systolic = 120,
            diastolic = 80,
            pulse = 70,
            recordedAt = 1000L,
        )
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(listOf(singleRecord)))

        viewModel = RecordListViewModel(mockRepository, mockAuthRepository, mockResourceProvider)

        val state = viewModel.uiState.value
        assertThat(state.records.size).isEqualTo(1)
        assertThat(state.records.first().id).isEqualTo("only-1")
    }
}
