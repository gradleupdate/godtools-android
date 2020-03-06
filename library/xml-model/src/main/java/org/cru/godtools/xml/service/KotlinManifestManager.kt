package org.cru.godtools.xml.service

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.ccci.gto.android.common.lifecycle.emptyLiveData
import org.ccci.gto.android.common.lifecycle.observeOnce
import org.cru.godtools.model.Translation
import org.cru.godtools.model.event.TranslationUpdateEvent
import org.cru.godtools.xml.model.Manifest
import org.greenrobot.eventbus.EventBus
import org.keynote.godtools.android.db.Contract.TranslationTable
import org.keynote.godtools.android.db.GodToolsDao
import java.util.Locale

open class KotlinManifestManager(@JvmField protected val context: Context) {
    @JvmField
    protected val dao = GodToolsDao.getInstance(context)

    @JvmField
    protected val manifestParser = ManifestParser.getInstance(context)

    @AnyThread
    fun preloadLatestPublishedManifest(toolCode: String, locale: Locale) {
        GlobalScope.launch(Dispatchers.Default) {
            val t = dao.getLatestTranslation(toolCode, locale, isPublished = true, isDownloaded = true).orElse(null)
            if (t != null) getManifest(t)
        }
    }

    @MainThread
    fun getLatestPublishedManifestLiveData(toolCode: String, locale: Locale) =
        dao.getLatestTranslationLiveData(toolCode, locale, isDownloaded = true)
            .apply {
                observeOnce {
                    if (it != null) {
                        it.updateLastAccessed()
                        dao.update(it, TranslationTable.COLUMN_LAST_ACCESSED)
                    }
                }
            }
            .switchMap {
                when (it) {
                    null -> emptyLiveData()
                    else -> getManifestLiveData(it)
                }
            }

    @WorkerThread
    @Throws(InterruptedException::class)
    fun getManifestBlocking(translation: Translation) = runBlocking { getManifest(translation) }
    fun getManifestLiveData(translation: Translation) = liveData { emit(getManifest(translation)) }

    private suspend fun getManifest(translation: Translation): Manifest? {
        val manifestFileName = translation.manifestFileName ?: return null
        val toolCode = translation.toolCode ?: return null
        return when (val result = manifestParser.parse(manifestFileName, toolCode, translation.languageCode)) {
            is Result.Error.Corrupted, is Result.Error.NotFound -> {
                withContext(Dispatchers.Default) { brokenManifest(manifestFileName) }
                null
            }
            is Result.Data -> result.manifest
            else -> null
        }
    }

    @WorkerThread
    protected open fun brokenManifest(manifestName: String) {
        dao.update(
            Translation().apply { isDownloaded = false },
            TranslationTable.FIELD_MANIFEST.eq(manifestName),
            TranslationTable.COLUMN_DOWNLOADED
        )
        EventBus.getDefault().post(TranslationUpdateEvent)
    }
}
