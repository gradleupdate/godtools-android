package org.keynote.godtools.android.db

import android.content.ContentValues
import android.database.Cursor
import org.ccci.gto.android.common.db.AbstractMapper
import org.ccci.gto.android.common.util.database.getString
import org.cru.godtools.model.LocalFile
import org.keynote.godtools.android.db.Contract.LocalFileTable

internal object LocalFileMapper : AbstractMapper<LocalFile>() {
    override fun mapField(values: ContentValues, field: String, file: LocalFile) {
        when (field) {
            LocalFileTable.COLUMN_NAME -> values.put(field, file.fileName)
            else -> super.mapField(values, field, file)
        }
    }

    override fun newObject(c: Cursor) = LocalFile()
    override fun toObject(c: Cursor) = super.toObject(c).apply {
        fileName = c.getString(LocalFileTable.COLUMN_NAME, LocalFile.INVALID_FILE_NAME)
    }
}
