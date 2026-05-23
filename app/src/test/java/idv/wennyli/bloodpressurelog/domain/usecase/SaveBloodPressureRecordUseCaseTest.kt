package idv.wennyli.bloodpressurelog.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class SaveBloodPressureRecordUseCaseTest {

    private val mockRepository = mockk<BloodPressureRepository>()
    private val useCase = SaveBloodPressureRecordUseCase(mockRepository)

    // ── 新增模式（recordId == null）──

    /** 新增模式下儲存成功，應回傳 Success 並呼叫 addRecord。 */
    @Test
    fun `add mode calls addRecord with correct values and returns Success`() = runTest {
        coEvery { mockRepository.addRecord(any()) } just Runs

        val result = useCase(
            recordId = null,
            systolic = 120,
            diastolic = 80,
            pulse = 72,
            note = "運動後",
            recordedAt = 1000L,
        )

        assertThat(result).isInstanceOf(SaveRecordResult.Success::class)
        coVerify {
            mockRepository.addRecord(
                match { it.systolic == 120 && it.diastolic == 80 && it.pulse == 72 && it.note == "運動後" && it.recordedAt == 1000L },
            )
        }
    }

    /** 新增模式下 Repository 拋出例外，應回傳帶有錯誤訊息的 Error。 */
    @Test
    fun `add mode returns Error when repository throws`() = runTest {
        coEvery { mockRepository.addRecord(any()) } throws RuntimeException("Network error")

        val result = useCase(
            recordId = null,
            systolic = 120,
            diastolic = 80,
            pulse = 72,
            note = "",
            recordedAt = 1000L,
        )

        assertThat(result).isInstanceOf(SaveRecordResult.Error::class)
        assertThat((result as SaveRecordResult.Error).message).isEqualTo("Network error")
    }

    /** 新增模式下例外的 message 為 null，Error.message 應為 null。 */
    @Test
    fun `add mode returns Error with null message when exception has no message`() = runTest {
        coEvery { mockRepository.addRecord(any()) } throws RuntimeException(null as String?)

        val result = useCase(
            recordId = null,
            systolic = 120,
            diastolic = 80,
            pulse = 72,
            note = "",
            recordedAt = 1000L,
        )

        assertThat((result as SaveRecordResult.Error).message).isNull()
    }

    // ── 編輯模式（recordId != null）──

    /** 編輯模式下儲存成功，應回傳 Success 並以正確參數呼叫 updateRecord。 */
    @Test
    fun `edit mode calls updateRecord with correct values and returns Success`() = runTest {
        coEvery { mockRepository.updateRecord(any()) } just Runs

        val result = useCase(
            recordId = "record-1",
            systolic = 130,
            diastolic = 85,
            pulse = 75,
            note = "after exercise",
            recordedAt = 1000L,
        )

        assertThat(result).isInstanceOf(SaveRecordResult.Success::class)
        coVerify {
            mockRepository.updateRecord(
                match {
                    it.id == "record-1" &&
                        it.systolic == 130 &&
                        it.diastolic == 85 &&
                        it.pulse == 75 &&
                        it.note == "after exercise" &&
                        it.recordedAt == 1000L
                },
            )
        }
    }

    /** 編輯模式下 Repository 拋出例外，應回傳帶有錯誤訊息的 Error。 */
    @Test
    fun `edit mode returns Error when repository throws`() = runTest {
        coEvery { mockRepository.updateRecord(any()) } throws RuntimeException("Update failed")

        val result = useCase(
            recordId = "record-1",
            systolic = 130,
            diastolic = 85,
            pulse = 75,
            note = "",
            recordedAt = 1000L,
        )

        assertThat(result).isInstanceOf(SaveRecordResult.Error::class)
        assertThat((result as SaveRecordResult.Error).message).isEqualTo("Update failed")
    }
}
