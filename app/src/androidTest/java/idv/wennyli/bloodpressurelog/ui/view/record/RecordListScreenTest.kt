package idv.wennyli.bloodpressurelog.ui.view.record

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import idv.wennyli.bloodpressurelog.data.model.BloodPressureRecord
import idv.wennyli.bloodpressurelog.utils.DateUtils
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun record(
        id: String = "1",
        systolic: Int = 120,
        diastolic: Int = 80,
        pulse: Int = 70,
        note: String = "",
        recordedAt: Long = 1_700_000_000_000L,
    ) = BloodPressureRecord(
        id = id,
        systolic = systolic,
        diastolic = diastolic,
        pulse = pulse,
        note = note,
        recordedAt = recordedAt,
    )

    @Test
    fun loadingState_hidesEmptyAndErrorText() {
        composeTestRule.setContent {
            RecordListContent(
                uiState = RecordListUiState(isLoading = true),
                onSignOut = {},
                onEditRecord = {},
                onDeleteRecord = {},
            )
        }

        composeTestRule.onNodeWithText("尚無紀錄").assertDoesNotExist()
    }

    @Test
    fun errorState_showsErrorMessage() {
        composeTestRule.setContent {
            RecordListContent(
                uiState = RecordListUiState(isLoading = false, errorMessage = "載入失敗"),
                onSignOut = {},
                onEditRecord = {},
                onDeleteRecord = {},
            )
        }

        composeTestRule.onNodeWithText("載入失敗").assertIsDisplayed()
    }

    @Test
    fun emptyState_showsEmptyMessage() {
        composeTestRule.setContent {
            RecordListContent(
                uiState = RecordListUiState(isLoading = false, records = emptyList()),
                onSignOut = {},
                onEditRecord = {},
                onDeleteRecord = {},
            )
        }

        composeTestRule.onNodeWithText("尚無紀錄").assertIsDisplayed()
    }

    @Test
    fun successState_showsRecordCardContent() {
        val fixedRecord = record(
            systolic = 132,
            diastolic = 84,
            pulse = 72,
            note = "運動後量測",
        )

        composeTestRule.setContent {
            RecordListContent(
                uiState = RecordListUiState(isLoading = false, records = listOf(fixedRecord)),
                onSignOut = {},
                onEditRecord = {},
                onDeleteRecord = {},
            )
        }

        composeTestRule.onNodeWithText("132/84 mmHg  ·  72 bpm").assertIsDisplayed()
        composeTestRule.onNodeWithText(DateUtils.formatDateTime(fixedRecord.recordedAt)).assertIsDisplayed()
        composeTestRule.onNodeWithText("運動後量測").assertIsDisplayed()
    }

    @Test
    fun successState_withMultipleRecords_showsAllOfThem() {
        val records = listOf(
            record(id = "1", systolic = 115, diastolic = 75, pulse = 68),
            record(id = "2", systolic = 132, diastolic = 84, pulse = 72),
            record(id = "3", systolic = 145, diastolic = 92, pulse = 80),
        )

        composeTestRule.setContent {
            RecordListContent(
                uiState = RecordListUiState(isLoading = false, records = records),
                onSignOut = {},
                onEditRecord = {},
                onDeleteRecord = {},
            )
        }

        composeTestRule.onNodeWithText("115/75 mmHg  ·  68 bpm").assertIsDisplayed()
        composeTestRule.onNodeWithText("132/84 mmHg  ·  72 bpm").assertIsDisplayed()
        composeTestRule.onNodeWithText("145/92 mmHg  ·  80 bpm").assertIsDisplayed()
    }

    @Test
    fun recordWithoutNote_doesNotShowNoteText() {
        val recordWithoutNote = record(note = "")

        composeTestRule.setContent {
            RecordListContent(
                uiState = RecordListUiState(isLoading = false, records = listOf(recordWithoutNote)),
                onSignOut = {},
                onEditRecord = {},
                onDeleteRecord = {},
            )
        }

        composeTestRule.onNodeWithText("運動後量測").assertDoesNotExist()
    }

    @Test
    fun signOutIconClick_invokesOnSignOut() {
        var signOutCalled = false

        composeTestRule.setContent {
            RecordListContent(
                uiState = RecordListUiState(isLoading = false, records = emptyList()),
                onSignOut = { signOutCalled = true },
                onEditRecord = {},
                onDeleteRecord = {},
            )
        }

        composeTestRule.onNodeWithContentDescription("登出").performClick()

        assert(signOutCalled)
    }

    @Test
    fun editIconClick_invokesOnEditRecordWithCorrectId() {
        var editedId: String? = null
        val fixedRecord = record(id = "rec-42")

        composeTestRule.setContent {
            RecordListContent(
                uiState = RecordListUiState(isLoading = false, records = listOf(fixedRecord)),
                onSignOut = {},
                onEditRecord = { editedId = it },
                onDeleteRecord = {},
            )
        }

        composeTestRule.onNodeWithContentDescription("編輯").performClick()

        assert(editedId == "rec-42")
    }

    @Test
    fun deleteIconClick_invokesOnDeleteRecordWithCorrectId() {
        // 這裡只驗證 RecordCard 直接呼叫的「請求刪除」callback，
        // 實際的刪除確認對話框（DeleteConfirmationDialog）另外測試。
        var deleteRequestedId: String? = null
        val fixedRecord = record(id = "rec-42")

        composeTestRule.setContent {
            RecordListContent(
                uiState = RecordListUiState(isLoading = false, records = listOf(fixedRecord)),
                onSignOut = {},
                onEditRecord = {},
                onDeleteRecord = { deleteRequestedId = it },
            )
        }

        composeTestRule.onNodeWithContentDescription("刪除").performClick()

        assert(deleteRequestedId == "rec-42")
    }
}
