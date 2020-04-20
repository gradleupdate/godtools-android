package org.cru.godtools.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.databinding.ViewDataBinding
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import butterknife.BindView
import org.ccci.gto.android.common.sync.event.SyncFinishedEvent
import org.ccci.gto.android.common.sync.swiperefreshlayout.widget.SwipeRefreshSyncHelper
import org.cru.godtools.R
import org.cru.godtools.activity.BasePlatformActivity
import org.cru.godtools.base.Settings
import org.cru.godtools.base.Settings.Companion.PREF_FEATURE_DISCOVERED
import org.cru.godtools.base.Settings.Companion.PREF_PARALLEL_LANGUAGE
import org.cru.godtools.base.Settings.Companion.PREF_PRIMARY_LANGUAGE
import org.cru.godtools.base.ui.fragment.BaseFragment
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

private const val EXTRA_SYNC_HELPER = "org.cru.godtools.fragment.BasePlatformFragment.SYNC_HELPER"

abstract class BasePlatformFragment<B : ViewDataBinding>(@LayoutRes layoutId: Int? = null) : BaseFragment<B>(layoutId) {
    @Inject
    protected lateinit var eventBus: EventBus
    @Inject
    protected lateinit var settings: Settings
    private val settingsChangeListener = ChangeListener()

    protected var primaryLanguage = Settings.defaultLanguage

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // restore any saved state
        savedInstanceState?.let {
            syncHelper.onRestoreInstanceState(it.getBundle(EXTRA_SYNC_HELPER))
        }

        loadLanguages(true)
        triggerInitialSync()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRefreshView()
    }

    override fun onStart() {
        super.onStart()
        eventBus.register(this)
        startSettingsChangeListener()
        loadLanguages(false)
        syncHelper.updateState()
    }

    @CallSuper
    protected open fun onSyncData(helper: SwipeRefreshSyncHelper, force: Boolean) = Unit

    protected open fun onUpdatePrimaryLanguage() = Unit
    protected open fun onUpdateFeatureDiscovery(feature: String) = Unit

    @MainThread
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSyncCompleted(event: SyncFinishedEvent) = syncHelper.updateState()

    override fun onStop() {
        super.onStop()
        stopSettingsChangeListener()
        eventBus.unregister(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(EXTRA_SYNC_HELPER, syncHelper.onSaveInstanceState())
    }

    override fun onDestroyView() {
        cleanupRefreshView()
        super.onDestroyView()
    }
    // endregion Lifecycle

    // region Sync Logic
    @JvmField
    @BindView(R.id.refresh)
    internal var refreshLayout: SwipeRefreshLayout? = null

    private val syncHelper = SwipeRefreshSyncHelper()

    private fun triggerInitialSync() {
        if ((activity as? BasePlatformActivity)?.handleChildrenSyncs != true) syncHelper.triggerSync()
    }

    internal fun SwipeRefreshSyncHelper.triggerSync(force: Boolean = false) {
        onSyncData(this, force)
        updateState()
    }

    private fun setupRefreshView() {
        syncHelper.refreshLayout = refreshLayout
        refreshLayout?.setOnRefreshListener { syncHelper.triggerSync(true) }
    }

    private fun cleanupRefreshView() {
        syncHelper.refreshLayout = null
        refreshLayout?.setOnRefreshListener(null)
    }
    // endregion Sync Logic

    internal fun loadLanguages(initial: Boolean) {
        val oldPrimary = primaryLanguage
        primaryLanguage = settings.primaryLanguage

        // trigger lifecycle events
        if (!initial) {
            if (oldPrimary != primaryLanguage) {
                onUpdatePrimaryLanguage()
            }
        }
    }

    private fun startSettingsChangeListener() {
        settings.registerOnSharedPreferenceChangeListener(settingsChangeListener)
    }

    private fun stopSettingsChangeListener() {
        settings.unregisterOnSharedPreferenceChangeListener(settingsChangeListener)
    }

    internal inner class ChangeListener : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
            when (key) {
                PREF_PRIMARY_LANGUAGE, PREF_PARALLEL_LANGUAGE -> loadLanguages(false)
            }
            if (key?.startsWith(PREF_FEATURE_DISCOVERED) == true) {
                onUpdateFeatureDiscovery(key.removePrefix(PREF_FEATURE_DISCOVERED))
            }
        }
    }
}
