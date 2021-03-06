package org.cru.godtools.sync.task

import android.database.sqlite.SQLiteDatabase
import androidx.collection.SimpleArrayMap
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.same
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.ccci.gto.android.common.db.Expression
import org.ccci.gto.android.common.db.Query
import org.cru.godtools.model.Language
import org.cru.godtools.model.event.LanguageUpdateEvent
import org.greenrobot.eventbus.EventBus
import org.junit.Before
import org.junit.Test
import org.keynote.godtools.android.db.Contract.LanguageTable
import org.keynote.godtools.android.db.GodToolsDao
import java.util.Locale

class BaseDataSyncTasksTest {
    private lateinit var dao: GodToolsDao
    private lateinit var eventBus: EventBus
    private lateinit var events: SimpleArrayMap<Class<*>, Any>

    private lateinit var tasks: BaseDataSyncTasks

    @Before
    fun setup() {
        dao = mock()
        eventBus = mock()
        events = mock()
        tasks = object : BaseDataSyncTasks(dao, eventBus) {}
    }

    @Test
    fun verifyStoreLanguage() {
        // setup test
        val language = Language().apply {
            setId(1L)
            code = Locale("lt")
        }

        // run test
        tasks.storeLanguage(events, language)
        verify(dao).refresh(same(language))
        verify(dao, never()).update(any<Any>(), anyOrNull<Expression>(), any<String>())
        verify(dao).updateOrInsert(same(language), eq(SQLiteDatabase.CONFLICT_REPLACE), any())
        verify(events).put(LanguageUpdateEvent.javaClass, LanguageUpdateEvent)
    }

    @Test
    fun verifyStoreLanguageChangingCode() {
        // setup test
        val language = Language().apply {
            setId(1L)
            code = Locale("lt")
            isAdded = false
        }
        val originalLanguage = Language().apply {
            setId(1L)
            code = Locale.forLanguageTag("lt-LT")
            isAdded = true
        }
        val pk = Expression.raw("test", "")
        whenever(dao.get(any<Query<Language>>())).thenReturn(listOf(originalLanguage))
        whenever(dao.getPrimaryKeyWhere(eq(originalLanguage))).thenReturn(pk)

        // run test
        tasks.storeLanguage(events, language)
        verify(dao).refresh(same(language))
        verify(dao).update(same(language), same(pk), eq(LanguageTable.COLUMN_CODE))
        verify(dao).updateOrInsert(same(language), eq(SQLiteDatabase.CONFLICT_REPLACE), any())
        verify(events, times(2)).put(LanguageUpdateEvent.javaClass, LanguageUpdateEvent)
    }
}
