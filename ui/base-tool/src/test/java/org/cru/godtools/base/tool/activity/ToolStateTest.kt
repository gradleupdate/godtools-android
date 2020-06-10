package org.cru.godtools.base.tool.activity

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cru.godtools.base.tool.activity.BaseToolActivity.ToolState
import org.cru.godtools.model.Translation
import org.cru.godtools.xml.model.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.EnumSet

@RunWith(AndroidJUnit4::class)
class ToolStateTest {
    @Test
    fun verifyDetermineToolState() {
        val types = EnumSet.allOf(Manifest.Type::class.java) - Manifest.Type.UNKNOWN
        types.forEach { validType ->
            val manifest = Manifest().apply { mType = validType }
            assertEquals(ToolState.LOADED, ToolState.determineToolState(manifest, null))
            assertEquals(ToolState.LOADED, ToolState.determineToolState(manifest, null, manifestType = validType))
            EnumSet.complementOf(EnumSet.of(validType)).forEach { invalidType ->
                assertEquals(
                    ToolState.INVALID_TYPE, ToolState.determineToolState(manifest, null, manifestType = invalidType)
                )
            }
        }

        assertEquals(ToolState.LOADING, ToolState.determineToolState(null, null, isSyncFinished = false))
        assertEquals(ToolState.LOADING, ToolState.determineToolState(null, Translation(), isSyncFinished = true))
        assertEquals(ToolState.NOT_FOUND, ToolState.determineToolState(null, null, isSyncFinished = true))
    }
}
