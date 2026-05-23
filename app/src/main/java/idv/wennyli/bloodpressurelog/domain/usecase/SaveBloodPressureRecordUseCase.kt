package idv.wennyli.bloodpressurelog.domain.usecase

import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import javax.inject.Inject

sealed interface SaveRecordResult {
    data object Success : SaveRecordResult
    data class Error(val message: String?) : SaveRecordResult
}

/**
 * 儲存血壓紀錄的 UseCase，負責以下業務邏輯：
 * - 依據 [recordId] 是否為 null 決定呼叫新增（[BloodPressureRepository.addRecord]）
 *   或更新（[BloodPressureRepository.updateRecord]）
 * - 編輯模式下保留原始 [originalCreatedAt]
 * - 捕捉 Repository 拋出的例外，轉換為 [SaveRecordResult.Error]
 */
class SaveBloodPressureRecordUseCase @Inject constructor(
    private val repository: BloodPressureRepository,
) {
    suspend operator fun invoke(
        recordId: String?,
        systolic: Int,
        diastolic: Int,
        pulse: Int,
        note: String,
        recordedAt: Long,
        originalCreatedAt: Long,
    ): SaveRecordResult = try {
        if (recordId != null) {
            repository.updateRecord(
                BloodPressureRecord(
                    id = recordId,
                    systolic = systolic,
                    diastolic = diastolic,
                    pulse = pulse,
                    note = note,
                    recordedAt = recordedAt,
                    createdAt = originalCreatedAt,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        } else {
            repository.addRecord(
                BloodPressureRecord(
                    systolic = systolic,
                    diastolic = diastolic,
                    pulse = pulse,
                    note = note,
                    recordedAt = recordedAt,
                ),
            )
        }
        SaveRecordResult.Success
    } catch (e: Exception) {
        SaveRecordResult.Error(e.message)
    }
}
