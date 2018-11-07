package org.cru.godtools.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.fragment.app.transaction
import org.cru.godtools.R
import org.cru.godtools.analytics.model.AnalyticsScreenEvent
import org.cru.godtools.analytics.model.AnalyticsScreenEvent.SCREEN_TOOL_DETAILS
import org.cru.godtools.base.Constants.EXTRA_TOOL
import org.cru.godtools.base.ui.activity.BaseActivity
import org.cru.godtools.fragment.ToolDetailsFragment

private const val TAG_MAIN_FRAGMENT = "mainFragment"

fun Activity.startToolDetailsActivity(toolCode: String) {
    Intent(this, ToolDetailsActivity::class.java)
        .putExtras(BaseActivity.buildExtras(this))
        .putExtra(EXTRA_TOOL, toolCode)
        .also { startActivity(it) }
}

class ToolDetailsActivity : BasePlatformActivity(), ToolDetailsFragment.Callbacks {
    // these properties should be treated as final and only set/modified in onCreate()
    private lateinit var tool: String

    // region Lifecycle Events

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // finish now if we couldn't process the intent
        if (!processIntent()) {
            finish()
            return
        }

        setContentView(R.layout.activity_generic_fragment_with_nav_drawer)
    }

    override fun onSetupActionBar() {
        super.onSetupActionBar()
        title = ""
    }

    override fun onStart() {
        super.onStart()
        loadInitialFragmentIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        mEventBus.post(AnalyticsScreenEvent(SCREEN_TOOL_DETAILS))
    }

    override fun onToolAdded() {
        finish()
    }

    override fun onToolRemoved() {
        finish()
    }

    // endregion Lifecycle Events

    /**
     * @return true if the intent was successfully processed, otherwise return false
     */
    private fun processIntent(): Boolean {
        tool =  intent?.extras?.getString(EXTRA_TOOL) ?: return false
        return true
    }

    @MainThread
    private fun loadInitialFragmentIfNeeded() {
        supportFragmentManager?.apply {
            if (findFragmentByTag(TAG_MAIN_FRAGMENT) == null) {
                transaction {
                    replace(R.id.frame, ToolDetailsFragment.newInstance(tool), TAG_MAIN_FRAGMENT)
                }
            }
        }
    }
}