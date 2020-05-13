package org.cru.godtools.util

import android.app.Activity
import org.cru.godtools.article.ui.categories.startCategoriesActivity
import org.cru.godtools.model.Tool.Type
import org.cru.godtools.tract.activity.TractActivity
import java.util.Locale

fun Activity.openToolActivity(code: String, type: Type, vararg languages: Locale) = when (type) {
    Type.TRACT -> TractActivity.start(this, code, *languages)
    Type.ARTICLE -> startCategoriesActivity(code, languages[0])
    Type.UNKNOWN -> Unit
}
