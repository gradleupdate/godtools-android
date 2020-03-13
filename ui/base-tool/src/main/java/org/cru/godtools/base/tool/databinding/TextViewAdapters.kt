package org.cru.godtools.base.tool.databinding

import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.databinding.BindingAdapter
import org.cru.godtools.base.tool.model.view.bindTo
import org.cru.godtools.xml.model.Text

@BindingAdapter("android:text", "android:textSize", "defaultTextColor", requireAll = false)
fun TextView.bindTextNode(text: Text?, @DimenRes textSize: Int?, @ColorInt defaultTextColor: Int?) =
    text.bindTo(this, textSize, defaultTextColor)
