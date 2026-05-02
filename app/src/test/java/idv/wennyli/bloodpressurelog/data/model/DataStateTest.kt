package idv.wennyli.bloodpressurelog.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DataStateTest {

    @Test
    fun `Loading is singleton`() {
        assertSame(DataState.Loading, DataState.Loading)
    }

    @Test
    fun `Success holds data`() {
        val state = DataState.Success(42)
        assertEquals(42, state.data)
    }

    @Test
    fun `Success with list holds data`() {
        val list = listOf("a", "b")
        val state = DataState.Success(list)
        assertEquals(list, state.data)
    }

    @Test
    fun `Error holds throwable and message`() {
        val throwable = RuntimeException("test error")
        val state = DataState.Error(throwable, "test error")
        assertSame(throwable, state.throwable)
        assertEquals("test error", state.message)
    }

    @Test
    fun `Success with Unit`() {
        val state = DataState.Success(Unit)
        assertEquals(Unit, state.data)
    }
}
