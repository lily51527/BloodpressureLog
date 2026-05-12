package idv.wennyli.bloodpressurelog.ui.view.record

import app.cash.turbine.test
import idv.wennyli.bloodpressurelog.MainDispatcherRule
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.DataState
import idv.wennyli.bloodpressurelog.data.repository.AuthRepository
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class RecordListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val mockRepository = mockk<BloodPressureRepository>()
    private val mockAuthRepository = mockk<AuthRepository>()
    private lateinit var viewModel: RecordListViewModel

    private val sampleRecords = listOf(
        BloodPressureRecord(id = "1", systolic = 120, diastolic = 80, pulse = 70, recordedAt = 3000L),
        BloodPressureRecord(id = "2", systolic = 130, diastolic = 85, pulse = 75, recordedAt = 1000L),
        BloodPressureRecord(id = "3", systolic = 110, diastolic = 70, pulse = 65, recordedAt = 2000L),
    )

    @Before
    fun setUp() {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Loading)
        viewModel = RecordListViewModel(mockRepository, mockAuthRepository)
    }

    @Test
    fun `initial state has isLoading true and empty records`() {
        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
        assertTrue(state.records.isEmpty())
        assertNull(state.errorMessage)
    }

    @Test
    fun `success state populates records sorted by recordedAt descending`() {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(sampleRecords))

        viewModel = RecordListViewModel(mockRepository, mockAuthRepository)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(listOf("1", "3", "2"), state.records.map { it.id })
    }

    @Test
    fun `error state sets errorMessage and clears isLoading`() {
        val error = RuntimeException("Firestore error")
        every { mockRepository.observeRecords() } returns flowOf(DataState.Error(error, "Firestore error"))

        viewModel = RecordListViewModel(mockRepository, mockAuthRepository)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Firestore error", state.errorMessage)
    }

    @Test
    fun `loading state sets isLoading true`() {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Loading)

        viewModel = RecordListViewModel(mockRepository, mockAuthRepository)

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onAddRecord emits null to navigateToAddEdit`() = runTest {
        viewModel.navigateToAddEdit.test {
            viewModel.onAddRecord()
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onEditRecord emits recordId to navigateToAddEdit`() = runTest {
        viewModel.navigateToAddEdit.test {
            viewModel.onEditRecord("record-123")
            assertEquals("record-123", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteRecord calls repository deleteRecord with correct id`() = runTest {
        coEvery { mockRepository.deleteRecord(any()) } just Runs

        viewModel.deleteRecord("record-456", "刪除失敗，請稍後再試")

        coVerify { mockRepository.deleteRecord("record-456") }
    }

    @Test
    fun `deleteRecord sets errorMessage on failure`() = runTest {
        coEvery { mockRepository.deleteRecord(any()) } throws RuntimeException("Firestore error")

        viewModel.deleteRecord("record-456", "刪除失敗，請稍後再試")

        assertEquals("刪除失敗，請稍後再試", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `deleteRecord does not set errorMessage on success`() = runTest {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(emptyList()))
        viewModel = RecordListViewModel(mockRepository, mockAuthRepository)
        coEvery { mockRepository.deleteRecord(any()) } just Runs

        viewModel.deleteRecord("record-456", "刪除失敗，請稍後再試")

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `signOut calls authRepository signOut`() = runTest {
        coEvery { mockAuthRepository.signOut() } just Runs

        viewModel.signOut()

        coVerify { mockAuthRepository.signOut() }
    }

    @Test
    fun `success with empty list shows empty state`() {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(emptyList()))

        viewModel = RecordListViewModel(mockRepository, mockAuthRepository)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.records.isEmpty())
        assertNull(state.errorMessage)
    }
}
