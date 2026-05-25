package idv.wennyli.bloodpressurelog.data.repository

import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.data.model.DataState
import kotlinx.coroutines.flow.Flow

interface BloodPressureRepository {
    fun observeRecords(startMs: Long? = null, endMs: Long? = null): Flow<DataState<List<BloodPressureRecord>>>
    suspend fun getRecord(id: String): BloodPressureRecord?
    suspend fun addRecord(record: BloodPressureRecord)
    suspend fun updateRecord(record: BloodPressureRecord)
    suspend fun deleteRecord(id: String)
}
