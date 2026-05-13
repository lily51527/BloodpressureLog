package idv.wennyli.bloodpressurelog.utils

import android.content.Context

interface ResourceProvider {
    fun getString(stringRes: Int): String
    fun getString(stringRes: Int, vararg formatArgs: Any): String
}

class ResourceProviderImpl(
    private val context: Context,
) : ResourceProvider {

    override fun getString(stringRes: Int): String = context.getString(stringRes)

    override fun getString(stringRes: Int, vararg formatArgs: Any): String =
        context.getString(stringRes, *formatArgs)
}
