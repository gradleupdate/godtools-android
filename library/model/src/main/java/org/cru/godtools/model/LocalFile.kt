package org.cru.godtools.model

import android.content.Context
import org.cru.godtools.base.util.getGodToolsFile

class LocalFile(val fileName: String?) {
    fun getFile(context: Context) = context.getGodToolsFile(fileName)
}
