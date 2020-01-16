package org.cru.godtools.tutorial.analytics.model

import org.cru.godtools.analytics.model.AnalyticsActionEvent
import org.cru.godtools.analytics.model.AnalyticsSystem

private const val TUTORIAL_SITE_SECTION = "tutorial"

class TutorialAnalyticsActionEvent(action: String) : AnalyticsActionEvent(null, action) {
    override fun isForSystem(system: AnalyticsSystem): Boolean {
        return system == AnalyticsSystem.ADOBE || system == AnalyticsSystem.FACEBOOK
    }

    override val adobeSiteSection get() = TUTORIAL_SITE_SECTION

    override fun getAttributes() = mapOf(action to 1)

    companion object {
        const val TUTORIAL_MORE_ACTION = "onboarding_more"
        const val TUTORIAL_START_ACTION = "onboarding_start"
    }
}
