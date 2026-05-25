package idv.wennyli.bloodpressurelog.ui.view.record

import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import assertk.assertions.isNull
import assertk.assertions.isTrue
import idv.wennyli.bloodpressurelog.MainDispatcherRule
import idv.wennyli.bloodpressurelog.R
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import idv.wennyli.bloodpressurelog.domain.usecase.SaveBloodPressureRecordUseCase
import idv.wennyli.bloodpressurelog.domain.usecase.SaveRecordResult
import idv.wennyli.bloodpressurelog.utils.ResourceProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class AddEditRecordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockRepository = mockk<BloodPressureRepository>()
    private val mockSaveRecordUseCase = mockk<SaveBloodPressureRecordUseCase>()
    private val mockResourceProvider = mockk<ResourceProvider>()

    @BeforeTest
    fun setUp() {
        every { mockResourceProvider.getString(R.string.add_edit_record_error_invalid_input) } returns "請輸入有效的正整數數值"
        every { mockResourceProvider.getString(R.string.add_edit_record_error_not_found) } returns "找不到紀錄"
    }

    private fun addModeViewModel() = AddEditRecordViewModel(
        repository = mockRepository,
        saveRecordUseCase = mockSaveRecordUseCase,
        resourceProvider = mockResourceProvider,
        savedStateHandle = SavedStateHandle(mapOf("recordId" to null)),
    )

    private fun editModeViewModel(recordId: String) = AddEditRecordViewModel(
        repository = mockRepository,
        saveRecordUseCase = mockSaveRecordUseCase,
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

    /** 有錯誤訊息時修改收縮壓，應清除先前的錯誤訊息。 */
    @Test
    fun `onSystolicChange clears errorMessage`() {
        val viewModel = addModeViewModel()
        viewModel.save()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("請輸入有效的正整數數值")

        viewModel.onSystolicChange("120")

        assertThat(viewModel.uiState.value.systolic).isEqualTo("120")
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    /** 有錯誤訊息時修改舒張壓，應清除先前的錯誤訊息。 */
    @Test
    fun `onDiastolicChange clears errorMessage`() {
        val viewModel = addModeViewModel()
        viewModel.save()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("請輸入有效的正整數數值")

        viewModel.onDiastolicChange("80")

        assertThat(viewModel.uiState.value.diastolic).isEqualTo("80")
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    /** 有錯誤訊息時修改脈搏，應清除先前的錯誤訊息。 */
    @Test
    fun `onPulseChange clears errorMessage`() {
        val viewModel = addModeViewModel()
        viewModel.save()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("請輸入有效的正整數數值")

        viewModel.onPulseChange("72")

        assertThat(viewModel.uiState.value.pulse).isEqualTo("72")
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    /** 使用者修改備註輸入時，應保留先前的錯誤訊息（備註欄不觸發錯誤清除）。 */
    @Test
    fun `onNoteChange does not clear errorMessage`() {
        val viewModel = addModeViewModel()
        viewModel.save()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("請輸入有效的正整數數值")

        viewModel.onNoteChange("備註")

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("請輸入有效的正整數數值")
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

    /** 收縮壓輸入為負數時，儲存應顯示輸入無效的錯誤訊息。 */
    @Test
    fun `save with negative systolic sets errorMessage`() {
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("-1")
        viewModel.onDiastolicChange("80")
        viewModel.onPulseChange("72")

        viewModel.save()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("請輸入有效的正整數數值")
    }

    /** 舒張壓輸入為 0 時，儲存應顯示輸入無效的錯誤訊息。 */
    @Test
    fun `save with zero diastolic sets errorMessage`() {
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("120")
        viewModel.onDiastolicChange("0")
        viewModel.onPulseChange("72")

        viewModel.save()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("請輸入有效的正整數數值")
    }

    /** 脈搏輸入非數字時，儲存應顯示輸入無效的錯誤訊息。 */
    @Test
    fun `save with invalid pulse sets errorMessage`() {
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("120")
        viewModel.onDiastolicChange("80")
        viewModel.onPulseChange("abc")

        viewModel.save()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("請輸入有效的正整數數值")
    }

    /** 驗證通過後，應將解析後的 Int 值傳給 SaveRecordUseCase。 */
    @Test
    fun `save passes parsed int values to saveRecordUseCase`() = runTest {
        coEvery { mockSaveRecordUseCase(any(), any(), any(), any(), any(), any()) } returns SaveRecordResult.Success
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("120")
        viewModel.onDiastolicChange("80")
        viewModel.onPulseChange("72")
        viewModel.onNoteChange("運動後")

        viewModel.save()

        coVerify {
            mockSaveRecordUseCase(
                recordId = null,
                systolic = 120,
                diastolic = 80,
                pulse = 72,
                note = "運動後",
                recordedAt = any(),
            )
        }
    }

    /** 新增模式儲存成功後，應發射 savedSuccessfully 事件並重置 isLoading。 */
    @Test
    fun `save success in add mode emits savedSuccessfully and resets isLoading`() = runTest {
        coEvery { mockSaveRecordUseCase(any(), any(), any(), any(), any(), any()) } returns SaveRecordResult.Success
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("120")
        viewModel.onDiastolicChange("80")
        viewModel.onPulseChange("72")

        viewModel.savedSuccessfully.test {
            viewModel.save()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    /** 新增模式儲存失敗時，應顯示錯誤訊息並清除載入狀態。 */
    @Test
    fun `save error in add mode sets errorMessage and resets isLoading`() = runTest {
        coEvery { mockSaveRecordUseCase(any(), any(), any(), any(), any(), any()) } returns SaveRecordResult.Error("Network error")
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("120")
        viewModel.onDiastolicChange("80")
        viewModel.onPulseChange("72")

        viewModel.save()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("Network error")
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    /** UseCase 回傳 Error(null) 時，應顯示 fallback 錯誤訊息。 */
    @Test
    fun `save error with null message shows fallback error`() = runTest {
        every { mockResourceProvider.getString(R.string.add_edit_record_error_save_failed) } returns "儲存失敗"
        coEvery { mockSaveRecordUseCase(any(), any(), any(), any(), any(), any()) } returns SaveRecordResult.Error(null)
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("120")
        viewModel.onDiastolicChange("80")
        viewModel.onPulseChange("72")

        viewModel.save()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("儲存失敗")
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

    /** 載入紀錄時例外的 message 為 null，應顯示 fallback 錯誤訊息。 */
    @Test
    fun `load record error with null exception message shows fallback error`() = runTest {
        every { mockResourceProvider.getString(R.string.add_edit_record_error_load_failed) } returns "載入失敗"
        coEvery { mockRepository.getRecord("record-1") } throws RuntimeException(null as String?)

        val viewModel = editModeViewModel("record-1")
        val state = viewModel.uiState.value

        assertThat(state.errorMessage).isEqualTo("載入失敗")
        assertThat(state.isLoading).isFalse()
    }

    /** 編輯模式儲存時，應以載入的紀錄值呼叫 SaveRecordUseCase。 */
    @Test
    fun `save in edit mode passes loaded record values to saveRecordUseCase`() = runTest {
        val record = BloodPressureRecord(
            id = "record-1",
            systolic = 130,
            diastolic = 85,
            pulse = 75,
            note = "",
            recordedAt = 1000L,
        )
        coEvery { mockRepository.getRecord("record-1") } returns record
        coEvery { mockSaveRecordUseCase(any(), any(), any(), any(), any(), any()) } returns SaveRecordResult.Success

        val viewModel = editModeViewModel("record-1")
        viewModel.save()

        coVerify {
            mockSaveRecordUseCase(
                recordId = "record-1",
                systolic = 130,
                diastolic = 85,
                pulse = 75,
                note = "",
                recordedAt = 1000L,
            )
        }
    }

    /** 編輯模式儲存失敗時，應顯示錯誤訊息並清除載入狀態。 */
    @Test
    fun `save error in edit mode sets errorMessage and resets isLoading`() = runTest {
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
        coEvery { mockSaveRecordUseCase(any(), any(), any(), any(), any(), any()) } returns SaveRecordResult.Error("Update failed")

        val viewModel = editModeViewModel("record-1")
        viewModel.save()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("Update failed")
        assertThat(viewModel.uiState.value.isLoading).isFalse()
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
        coEvery { mockSaveRecordUseCase(any(), any(), any(), any(), any(), any()) } returns SaveRecordResult.Success

        val viewModel = editModeViewModel("record-1")

        viewModel.savedSuccessfully.test {
            viewModel.save()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── resetForm ──

    /** resetForm 後，所有輸入欄位應回到空字串初始值。 */
    @Test
    fun `resetForm clears all input fields`() {
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("120")
        viewModel.onDiastolicChange("80")
        viewModel.onPulseChange("72")
        viewModel.onNoteChange("運動後量測")

        viewModel.resetForm()

        val state = viewModel.uiState.value
        assertThat(state.systolic).isEqualTo("")
        assertThat(state.diastolic).isEqualTo("")
        assertThat(state.pulse).isEqualTo("")
        assertThat(state.note).isEqualTo("")
    }

    /** resetForm 後，errorMessage 應清除為 null，isLoading 應歸零為 false。 */
    @Test
    fun `resetForm clears errorMessage and resets isLoading`() {
        val viewModel = addModeViewModel()
        viewModel.save() // 觸發 invalid input error，讓 errorMessage 非 null

        viewModel.resetForm()

        val state = viewModel.uiState.value
        assertThat(state.errorMessage).isNull()
        assertThat(state.isLoading).isFalse()
    }

    /** resetForm 後，recordedAt 應更新為當前時間（不等於重置前的舊值）。 */
    @Test
    fun `resetForm updates recordedAt to current time`() {
        val viewModel = addModeViewModel()
        viewModel.onRecordedAtChange(1000L) // 設定一個舊時間戳

        val beforeReset = System.currentTimeMillis()
        viewModel.resetForm()
        val afterReset = System.currentTimeMillis()

        val recordedAt = viewModel.uiState.value.recordedAt
        assertThat(recordedAt).isGreaterThan(beforeReset - 1)
        assertThat(recordedAt).isLessThan(afterReset + 1)
    }
}
