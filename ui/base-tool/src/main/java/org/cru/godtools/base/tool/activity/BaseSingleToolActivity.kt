package org.cru.godtools.base.tool.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.observe
import org.ccci.gto.android.common.util.os.getLocale
import org.ccci.gto.android.common.util.os.putLocale
import org.cru.godtools.base.Constants
import org.cru.godtools.base.tool.viewmodel.LatestPublishedManifestDataModel
import org.cru.godtools.model.Language
import org.cru.godtools.model.Translation
import org.cru.godtools.xml.model.Manifest
import java.util.Locale

abstract class BaseSingleToolActivity @JvmOverloads constructor(
    immersive: Boolean,
    private val requireTool: Boolean = true
) : BaseToolActivity(immersive) {
    override var activeManifest: Manifest? = null
    private var translationLoaded = false
    private var translation: Translation? = null

    private val dataModel: BaseSingleToolActivityDataModel by viewModels()
    protected val manifestDataModel: LatestPublishedManifestDataModel get() = dataModel

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.extras?.let { extras ->
            dataModel.toolCode.value = extras.getString(Constants.EXTRA_TOOL, dataModel.toolCode.value)
            dataModel.locale.value = extras.getLocale(Constants.EXTRA_LANGUAGE, dataModel.locale.value)
        }

        // finish now if this activity is in an invalid state
        if (!validStartState()) {
            finish()
            return
        }

        startLoaders()
    }

    override fun onStart() {
        super.onStart()
        startDownloadProgressListener(dataModel.toolCode.value, dataModel.locale.value)
    }

    private fun onUpdateTranslation() {
        updateVisibilityState()
    }

    override fun onStop() {
        stopDownloadProgressListener()
        super.onStop()
    }
    // endregion Lifecycle

    private fun hasTool() = dataModel.toolCode.value != null && dataModel.locale.value != null

    protected val tool: String
        get() = when {
            !requireTool -> throw UnsupportedOperationException(
                "You cannot get the tool code on a fragment that doesn't require a tool"
            )
            else -> checkNotNull(dataModel.toolCode.value) { "requireTool is true, but a tool wasn't specified" }
        }

    protected val locale: Locale
        get() = when {
            !requireTool -> throw UnsupportedOperationException(
                "You cannot get the locale on a fragment that doesn't require a tool"
            )
            else -> checkNotNull(dataModel.locale.value?.takeUnless { it == Language.INVALID_CODE }) {
                "requireTool is true, but a valid locale wasn't specified"
            }
        }

    override fun cacheTools() {
        val toolCode = dataModel.toolCode.value ?: return
        val locale = dataModel.locale.value ?: return
        downloadManager.cacheTranslation(toolCode, locale)
    }

    override fun determineActiveToolState() = when {
        !hasTool() -> STATE_LOADED
        activeManifest?.type?.let { isSupportedType(it) } == false -> STATE_INVALID_TYPE
        activeManifest != null -> STATE_LOADED
        translationLoaded && translation == null -> STATE_NOT_FOUND
        else -> STATE_LOADING
    }

    protected abstract fun isSupportedType(type: Manifest.Type): Boolean

    private fun validStartState() = !requireTool || hasTool()

    private fun startLoaders() {
        dataModel.manifest.observe(this) { setManifest(it) }
        dataModel.translation.observe(this) { setTranslation(it) }
    }

    private fun setManifest(manifest: Manifest?) {
        activeManifest = manifest
        onUpdateActiveManifest()
    }

    private fun setTranslation(translation: Translation?) {
        translationLoaded = true
        this.translation = translation
        onUpdateTranslation()
    }

    // region Up Navigation
    override fun buildParentIntentExtras(): Bundle {
        val extras = super.buildParentIntentExtras()
        extras.putString(Constants.EXTRA_TOOL, dataModel.toolCode.value)
        extras.putLocale(Constants.EXTRA_LANGUAGE, dataModel.locale.value)
        return extras
    }
    // endregion Up Navigation

    companion object {
        fun buildExtras(context: Context, toolCode: String?, language: Locale?) = buildExtras(context).apply {
            putString(Constants.EXTRA_TOOL, toolCode)
            putLocale(Constants.EXTRA_LANGUAGE, language)
        }
    }
}
