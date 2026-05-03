package idv.wennyli.bloodpressurelog.data.repository

import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.DataState
import kotlinx.coroutines.flow.Flow

interface BloodPressureRepository {
    fun observeRecords(): Flow<DataState<List<BloodPressureRecord>>>
    suspend fun getRecord(id: String): DataState<BloodPressureRecord?>
    suspend fun addRecord(record: BloodPressureRecord): DataState<Unit>
    suspend fun updateRecord(record: BloodPressureRecord): DataState<Unit>
    suspend fun deleteRecord(id: String): DataState<Unit>
}
