package org.cru.godtools.article.ui.categories

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.fragment.app.commit
import org.cru.godtools.article.R
import org.cru.godtools.article.ui.articles.startArticlesActivity
import org.cru.godtools.base.tool.activity.BaseArticleActivity
import org.cru.godtools.base.tool.activity.BaseSingleToolActivity
import org.cru.godtools.base.tool.analytics.model.SCREEN_CATEGORIES
import org.cru.godtools.base.tool.analytics.model.ToolAnalyticsScreenEvent
import org.cru.godtools.xml.model.Category
import java.util.Locale

fun Context.createCategoriesIntent(toolCode: String, language: Locale): Intent {
    return Intent(this, CategoriesActivity::class.java)
        .putExtras(BaseSingleToolActivity.buildExtras(this, toolCode, language))
}

fun Activity.startCategoriesActivity(toolCode: String, language: Locale) {
    startActivity(createCategoriesIntent(toolCode, language))
}

class CategoriesActivity : BaseArticleActivity(), CategorySelectedListener {
    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        setContentView(R.layout.activity_generic_tool_fragment)
        if (savedInstanceState == null) trackToolOpen(tool)
    }

    override fun onStart() {
        super.onStart()
        loadPrimaryFragmentIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        eventBus.post(ToolAnalyticsScreenEvent(SCREEN_CATEGORIES, tool, locale))
    }

    override fun onCategorySelected(category: Category?) = startArticlesActivity(tool, locale, category?.id)
    // endregion Lifecycle

    @MainThread
    private fun loadPrimaryFragmentIfNeeded() {
        with(supportFragmentManager) {
            if (primaryNavigationFragment != null) return

            commit {
                val fragment = CategoriesFragment(tool, locale)
                replace(R.id.frame, fragment)
                setPrimaryNavigationFragment(fragment)
            }
        }
    }
}
