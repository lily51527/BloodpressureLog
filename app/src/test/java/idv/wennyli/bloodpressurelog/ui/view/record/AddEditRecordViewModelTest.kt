package idv.wennyli.bloodpressurelog.ui.view.record

import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import idv.wennyli.bloodpressurelog.MainDispatcherRule
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditRecordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val mockRepository = mockk<BloodPressureRepository>()
    private val mockResourceProvider = mockk<ResourceProvider>()

    @BeforeTest
    fun setUp() {
        every { mockResourceProvider.getString(R.string.error_record_invalid_input) } returns "請輸入有效的正整數數值"
        every { mockResourceProvider.getString(R.string.error_record_not_found) } returns "找不到紀錄"
    }

    private fun addModeViewModel() = AddEditRecordViewModel(
        repository = mockRepository,
        resourceProvider = mockResourceProvider,
        savedStateHandle = SavedStateHandle(mapOf("recordId" to null)),
    )

    private fun editModeViewModel(recordId: String) = AddEditRecordViewModel(
        repository = mockRepository,
        resourceProvider = mockResourceProvider,
        savedStateHandle = SavedStateHandle(mapOf("recordId" to recordId)),
    )

    // ── Add mode ──

    /** 新增模式下，初始狀態所有輸入欄位應為空且 isEditMode 為 false。 */
    @Test
    fun `add mode initial state has empty fields and isEditMode false`() {
        val viewModel = addModeViewModel()
        val state = viewModel.uiState.value

        assertThat(state.systolic).isEqualTo("")
        assertThat(state.diastolic).isEqualTo("")
        assertThat(state.pulse).isEqualTo("")
        assertThat(state.note).isEqualTo("")
        assertThat(state.isEditMode).isFalse()
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    /** 使用者修改收縮壓輸入時，應更新值並清除先前的錯誤訊息。 */
    @Test
    fun `onSystolicChange updates systolic and clears errorMessage`() {
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("120")
        assertThat(viewModel.uiState.value.systolic).isEqualTo("120")
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    /** 使用者修改舒張壓輸入時，應正確更新對應欄位值。 */
    @Test
    fun `onDiastolicChange updates diastolic`() {
        val viewModel = addModeViewModel()
        viewModel.onDiastolicChange("80")
        assertThat(viewModel.uiState.value.diastolic).isEqualTo("80")
    }

    /** 使用者修改脈搏輸入時，應正確更新對應欄位值。 */
    @Test
    fun `onPulseChange updates pulse`() {
        val viewModel = addModeViewModel()
        viewModel.onPulseChange("72")
        assertThat(viewModel.uiState.value.pulse).isEqualTo("72")
    }

    /** 使用者修改備註輸入時，應正確更新對應欄位值。 */
    @Test
    fun `onNoteChange updates note`() {
        val viewModel = addModeViewModel()
        viewModel.onNoteChange("運動後")
        assertThat(viewModel.uiState.value.note).isEqualTo("運動後")
    }

    /** 使用者選擇記錄時間時，應正確更新 recordedAt 欄位。 */
    @Test
    fun `onRecordedAtChange updates recordedAt`() {
        val viewModel = addModeViewModel()
        viewModel.onRecordedAtChange(123456789L)
        assertThat(viewModel.uiState.value.recordedAt).isEqualTo(123456789L)
    }

    /** 收縮壓輸入非數字時，儲存應顯示輸入無效的錯誤訊息。 */
    @Test
    fun `save with invalid systolic sets errorMessage`() {
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("abc")
        viewModel.onDiastolicChange("80")
        viewModel.onPulseChange("72")

        viewModel.save()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("請輸入有效的正整數數值")
    }

    /** 收縮壓輸入為 0 時，儲存應顯示輸入無效的錯誤訊息。 */
    @Test
    fun `save with zero systolic sets errorMessage`() {
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("0")
        viewModel.onDiastolicChange("80")
        viewModel.onPulseChange("72")

        viewModel.save()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("請輸入有效的正整數數值")
    }

    /** 所有輸入欄位為空時，儲存應顯示輸入無效的錯誤訊息。 */
    @Test
    fun `save with empty fields sets errorMessage`() {
        val viewModel = addModeViewModel()

        viewModel.save()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("請輸入有效的正整數數值")
    }

    /** 新增模式儲存成功後，應發射 savedSuccessfully 事件通知 UI 操作完成。 */
    @Test
    fun `save success in add mode emits savedSuccessfully`() = runTest {
        coEvery { mockRepository.addRecord(any()) } just Runs
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("120")
        viewModel.onDiastolicChange("80")
        viewModel.onPulseChange("72")

        viewModel.savedSuccessfully.test {
            viewModel.save()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** 新增模式儲存失敗時，應顯示錯誤訊息並清除載入狀態。 */
    @Test
    fun `save error in add mode sets errorMessage`() = runTest {
        coEvery { mockRepository.addRecord(any()) } throws RuntimeException("Network error")
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("120")
        viewModel.onDiastolicChange("80")
        viewModel.onPulseChange("72")

        viewModel.save()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("Network error")
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    // ── Edit mode ──

    /** 編輯模式啟動時，應從 Repository 載入紀錄並填入所有欄位。 */
    @Test
    fun `edit mode loads record from repository`() = runTest {
        val record = BloodPressureRecord(
            id = "record-1",
            systolic = 130,
            diastolic = 85,
            pulse = 75,
            note = "after exercise",
            recordedAt = 1000L,
            createdAt = 500L,
        )
        coEvery { mockRepository.getRecord("record-1") } returns record

        val viewModel = editModeViewModel("record-1")
        val state = viewModel.uiState.value

        assertThat(state.systolic).isEqualTo("130")
        assertThat(state.diastolic).isEqualTo("85")
        assertThat(state.pulse).isEqualTo("75")
        assertThat(state.note).isEqualTo("after exercise")
        assertThat(state.recordedAt).isEqualTo(1000L)
        assertThat(state.originalCreatedAt).isEqualTo(500L)
        assertThat(state.isEditMode).isTrue()
        assertThat(state.isLoading).isFalse()
    }

    /** 編輯模式找不到指定紀錄時，應顯示找不到紀錄的錯誤訊息。 */
    @Test
    fun `edit mode shows error when record is not found`() = runTest {
        coEvery { mockRepository.getRecord("missing-id") } returns null

        val viewModel = editModeViewModel("missing-id")
        val state = viewModel.uiState.value

        assertThat(state.errorMessage).isEqualTo("找不到紀錄")
        assertThat(state.isLoading).isFalse()
    }

    /** 編輯模式讀取紀錄失敗時，應顯示對應的錯誤訊息。 */
    @Test
    fun `edit mode shows error when getRecord fails`() = runTest {
        coEvery { mockRepository.getRecord("record-1") } throws RuntimeException("Network error")

        val viewModel = editModeViewModel("record-1")
        val state = viewModel.uiState.value

        assertThat(state.errorMessage).isEqualTo("Network error")
        assertThat(state.isLoading).isFalse()
    }

    /** 編輯模式儲存時，應保留原始 createdAt 並呼叫 updateRecord 更新紀錄。 */
    @Test
    fun `save in edit mode calls updateRecord with preserved createdAt`() = runTest {
        val record = BloodPressureRecord(
            id = "record-1",
            systolic = 130,
            diastolic = 85,
            pulse = 75,
            note = "",
            recordedAt = 1000L,
            createdAt = 500L,
        )
        coEvery { mockRepository.getRecord("record-1") } returns record
        coEvery { mockRepository.updateRecord(any()) } just Runs

        val viewModel = editModeViewModel("record-1")
        viewModel.save()

        coVerify {
            mockRepository.updateRecord(
                match { it.id == "record-1" && it.createdAt == 500L && it.systolic == 130 },
            )
        }
    }

    /** 編輯模式儲存成功後，應發射 savedSuccessfully 事件通知 UI 操作完成。 */
    @Test
    fun `save success in edit mode emits savedSuccessfully`() = runTest {
        val record = BloodPressureRecord(
            id = "record-1",
            systolic = 130,
            diastolic = 85,
            pulse = 75,
            note = "",
            recordedAt = 1000L,
        )
        coEvery { mockRepository.getRecord("record-1") } returns record
        coEvery { mockRepository.updateRecord(any()) } just Runs

        val viewModel = editModeViewModel("record-1")

        viewModel.savedSuccessfully.test {
            viewModel.save()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
