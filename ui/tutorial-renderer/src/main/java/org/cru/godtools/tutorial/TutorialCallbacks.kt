package org.cru.godtools.tutorial

import android.view.View

interface TutorialCallbacks {
    fun nextPage()
    fun onTutorialAction(view: View)
}
