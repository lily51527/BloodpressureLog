package idv.wennyli.bloodpressurelog.utils

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class SnackbarControllerImplTest {

    private lateinit var controller: SnackbarControllerImpl

    @BeforeTest
    fun setUp() {
        controller = SnackbarControllerImpl()
    }

    /** sendMessage 送出訊息後，正在監聽的 collector 應收到相同內容。 */
    @Test
    fun `sendMessage emits message to messages flow`() = runTest {
        controller.messages.test {
            controller.sendMessage("儲存完成")

            assertThat(awaitItem()).isEqualTo("儲存完成")
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** 連續送出多則訊息時，collector 應依序收到每一則。 */
    @Test
    fun `sendMessage emits multiple messages in order`() = runTest {
        controller.messages.test {
            controller.sendMessage("first")
            controller.sendMessage("second")

            assertThat(awaitItem()).isEqualTo("first")
            assertThat(awaitItem()).isEqualTo("second")
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** messages 沒有 replay，訊息送出後才開始監聽的 collector 不會收到先前的訊息。 */
    @Test
    fun `messages does not replay events to late collectors`() = runTest {
        controller.sendMessage("missed")

        controller.messages.test {
            controller.sendMessage("received")

            assertThat(awaitItem()).isEqualTo("received")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
