package idv.wennyli.bloodpressurelog.ui.view.record

import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import idv.wennyli.bloodpressurelog.MainDispatcherRule
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditRecordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val mockRepository = mockk<BloodPressureRepository>()

    private fun addModeViewModel() = AddEditRecordViewModel(
        repository = mockRepository,
        savedStateHandle = SavedStateHandle(mapOf("recordId" to null)),
    )

    private fun editModeViewModel(recordId: String) = AddEditRecordViewModel(
        repository = mockRepository,
        savedStateHandle = SavedStateHandle(mapOf("recordId" to recordId)),
    )

    @Before
    fun setUp() {
        coEvery { mockRepository.getRecord(any()) } returns DataState.Loading
    }

    // ── Add mode ──

    @Test
    fun `add mode initial state has empty fields and isEditMode false`() {
        val viewModel = addModeViewModel()
        val state = viewModel.uiState.value

        assertEquals("", state.systolic)
        assertEquals("", state.diastolic)
        assertEquals("", state.pulse)
        assertEquals("", state.note)
        assertFalse(state.isEditMode)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onSystolicChange updates systolic and clears errorMessage`() {
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("120")
        assertEquals("120", viewModel.uiState.value.systolic)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onDiastolicChange updates diastolic`() {
        val viewModel = addModeViewModel()
        viewModel.onDiastolicChange("80")
        assertEquals("80", viewModel.uiState.value.diastolic)
    }

    @Test
    fun `onPulseChange updates pulse`() {
        val viewModel = addModeViewModel()
        viewModel.onPulseChange("72")
        assertEquals("72", viewModel.uiState.value.pulse)
    }

    @Test
    fun `onNoteChange updates note`() {
        val viewModel = addModeViewModel()
        viewModel.onNoteChange("運動後")
        assertEquals("運動後", viewModel.uiState.value.note)
    }

    @Test
    fun `onRecordedAtChange updates recordedAt`() {
        val viewModel = addModeViewModel()
        viewModel.onRecordedAtChange(123456789L)
        assertEquals(123456789L, viewModel.uiState.value.recordedAt)
    }

    @Test
    fun `save with invalid systolic sets errorMessage`() {
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("abc")
        viewModel.onDiastolicChange("80")
        viewModel.onPulseChange("72")

        viewModel.save()

        assertEquals("請輸入有效的正整數數值", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `save with zero systolic sets errorMessage`() {
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("0")
        viewModel.onDiastolicChange("80")
        viewModel.onPulseChange("72")

        viewModel.save()

        assertEquals("請輸入有效的正整數數值", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `save with empty fields sets errorMessage`() {
        val viewModel = addModeViewModel()

        viewModel.save()

        assertEquals("請輸入有效的正整數數值", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `save success in add mode emits savedSuccessfully`() = runTest {
        coEvery { mockRepository.addRecord(any()) } returns DataState.Success(Unit)
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

    @Test
    fun `save error in add mode sets errorMessage`() = runTest {
        coEvery { mockRepository.addRecord(any()) } returns
            DataState.Error(RuntimeException("Network error"), "Network error")
        val viewModel = addModeViewModel()
        viewModel.onSystolicChange("120")
        viewModel.onDiastolicChange("80")
        viewModel.onPulseChange("72")

        viewModel.save()

        assertEquals("Network error", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ── Edit mode ──

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
        coEvery { mockRepository.getRecord("record-1") } returns DataState.Success(record)

        val viewModel = editModeViewModel("record-1")
        val state = viewModel.uiState.value

        assertEquals("130", state.systolic)
        assertEquals("85", state.diastolic)
        assertEquals("75", state.pulse)
        assertEquals("after exercise", state.note)
        assertEquals(1000L, state.recordedAt)
        assertEquals(500L, state.originalCreatedAt)
        assertTrue(state.isEditMode)
        assertFalse(state.isLoading)
    }

    @Test
    fun `edit mode shows error when record is not found`() = runTest {
        coEvery { mockRepository.getRecord("missing-id") } returns DataState.Success(null)

        val viewModel = editModeViewModel("missing-id")
        val state = viewModel.uiState.value

        assertEquals("找不到紀錄", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `edit mode shows error when getRecord fails`() = runTest {
        coEvery { mockRepository.getRecord("record-1") } returns
            DataState.Error(RuntimeException("Network error"), "Network error")

        val viewModel = editModeViewModel("record-1")
        val state = viewModel.uiState.value

        assertEquals("Network error", state.errorMessage)
        assertFalse(state.isLoading)
    }

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
        coEvery { mockRepository.getRecord("record-1") } returns DataState.Success(record)
        coEvery { mockRepository.updateRecord(any()) } returns DataState.Success(Unit)

        val viewModel = editModeViewModel("record-1")
        viewModel.save()

        coVerify {
            mockRepository.updateRecord(
                match { it.id == "record-1" && it.createdAt == 500L && it.systolic == 130 },
            )
        }
    }

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
        coEvery { mockRepository.getRecord("record-1") } returns DataState.Success(record)
        coEvery { mockRepository.updateRecord(any()) } returns DataState.Success(Unit)

        val viewModel = editModeViewModel("record-1")

        viewModel.savedSuccessfully.test {
            viewModel.save()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
