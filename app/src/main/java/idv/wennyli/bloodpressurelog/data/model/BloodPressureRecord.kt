package idv.wennyli.bloodpressurelog.data.model

import java.time.Instant
import java.time.ZoneId

data class BloodPressureRecord(
    /** Firestore 文件 ID，新增時由 Firestore 自動產生，預設為空字串。 */
    val id: String = "",
    /** 收縮壓（mmHg），即心臟收縮時的血壓值。 */
    val systolic: Int,
    /** 舒張壓（mmHg），即心臟舒張時的血壓值。 */
    val diastolic: Int,
    /** 脈搏（次/分鐘）。 */
    val pulse: Int,
    /** 備註，供使用者記錄量測當下的狀況（如運動後、起床時等）。 */
    val note: String = "",
    /** 量測時間（Unix timestamp，毫秒），由使用者選擇，預設為建立當下時間。 */
    val recordedAt: Long = System.currentTimeMillis(),
    /** 紀錄建立時間（Unix timestamp，毫秒），寫入後不應變更。 */
    val createdAt: Long = System.currentTimeMillis(),
    /** 紀錄最後更新時間（Unix timestamp，毫秒）。 */
    val updatedAt: Long = System.currentTimeMillis(),
)

val BloodPressureRecord.timeSlot: TimeSlot
    get() {
        val hour = Instant.ofEpochMilli(recordedAt)
            .atZone(ZoneId.systemDefault())
            .hour
        return when (hour) {
            in 6..11 -> TimeSlot.MORNING
            in 12..17 -> TimeSlot.AFTERNOON
            in 18..23 -> TimeSlot.EVENING
            else -> TimeSlot.NIGHT
        }
    }
