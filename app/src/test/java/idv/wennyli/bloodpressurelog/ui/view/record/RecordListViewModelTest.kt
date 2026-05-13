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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class RecordListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

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

    @Test
    fun `initial state has isLoading true and empty records`() {
        val state = viewModel.uiState.value
        assertThat(state.isLoading).isTrue()
        assertThat(state.records).isEmpty()
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `success state populates records sorted by recordedAt descending`() {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(sampleRecords))

        viewModel = RecordListViewModel(mockRepository, mockAuthRepository, mockResourceProvider)

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
        assertThat(state.records.map { it.id }).containsExactly("1", "3", "2")
    }

    @Test
    fun `error state sets errorMessage and clears isLoading`() {
        val error = RuntimeException("Firestore error")
        every { mockRepository.observeRecords() } returns flowOf(DataState.Error(error, "Firestore error"))

        viewModel = RecordListViewModel(mockRepository, mockAuthRepository, mockResourceProvider)

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isEqualTo("Firestore error")
    }

    @Test
    fun `loading state sets isLoading true`() {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Loading)

        viewModel = RecordListViewModel(mockRepository, mockAuthRepository, mockResourceProvider)

        assertThat(viewModel.uiState.value.isLoading).isTrue()
    }

    @Test
    fun `onAddRecord emits null to navigateToAddEdit`() = runTest {
        viewModel.navigateToAddEdit.test {
            viewModel.onAddRecord()
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onEditRecord emits recordId to navigateToAddEdit`() = runTest {
        viewModel.navigateToAddEdit.test {
            viewModel.onEditRecord("record-123")
            assertThat(awaitItem()).isEqualTo("record-123")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteRecord calls repository deleteRecord with correct id`() = runTest {
        coEvery { mockRepository.deleteRecord(any()) } just Runs

        viewModel.deleteRecord("record-456")

        coVerify { mockRepository.deleteRecord("record-456") }
    }

    @Test
    fun `deleteRecord sets errorMessage on failure`() = runTest {
        coEvery { mockRepository.deleteRecord(any()) } throws RuntimeException("Firestore error")

        viewModel.deleteRecord("record-456")

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("刪除失敗，請稍後再試")
    }

    @Test
    fun `deleteRecord does not set errorMessage on success`() = runTest {
        every { mockRepository.observeRecords() } returns flowOf(DataState.Success(emptyList()))
        viewModel = RecordListViewModel(mockRepository, mockAuthRepository, mockResourceProvider)
        coEvery { mockRepository.deleteRecord(any()) } just Runs

        viewModel.deleteRecord("record-456")

        assertThat(viewModel.uiState.value.errorMessage).isNull()
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

        viewModel = RecordListViewModel(mockRepository, mockAuthRepository, mockResourceProvider)

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.records).isEmpty()
        assertThat(state.errorMessage).isNull()
    }
}
