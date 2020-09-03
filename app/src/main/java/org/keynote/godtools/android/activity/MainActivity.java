package org.keynote.godtools.android.activity;

import android.os.Bundle;

import com.annimon.stream.Stream;
import com.google.android.material.tabs.TabLayout;

import org.cru.godtools.BuildConfig;
import org.cru.godtools.base.Settings;
import org.cru.godtools.base.tool.service.ManifestManager;
import org.cru.godtools.base.util.LocaleUtils;
import org.cru.godtools.model.Tool;
import org.cru.godtools.tutorial.PageSet;
import org.cru.godtools.tutorial.activity.TutorialActivityKt;
import org.cru.godtools.ui.tooldetails.ToolDetailsActivityKt;
import org.cru.godtools.ui.tools.ToolsFragment;
import org.cru.godtools.util.ActivityUtilsKt;

import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dagger.Lazy;
import dagger.hilt.android.AndroidEntryPoint;

import static org.cru.godtools.base.Settings.FEATURE_TUTORIAL_ONBOARDING;
import static org.keynote.godtools.android.activity.KotlinMainActivityKt.TAB_ALL_TOOLS;
import static org.keynote.godtools.android.activity.KotlinMainActivityKt.TAB_FAVORITE_TOOLS;

@AndroidEntryPoint
public class MainActivity extends KotlinMainActivity implements ToolsFragment.Callbacks {
    @Inject
    Lazy<ManifestManager> mManifestManager;

    // region Lifecycle
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        triggerOnboardingIfNecessary();
    }

    @Override
    public void onTabSelected(final TabLayout.Tab tab) {
        if (tab.getPosition() == TAB_FAVORITE_TOOLS) {
            showFavoriteTools();
        } else if (tab.getPosition() == TAB_ALL_TOOLS) {
            showAllTools();
        } else if (BuildConfig.DEBUG) {
            // The tab selection logic is brittle, so throw an error in unrecognized scenarios
            throw new IllegalArgumentException("Unrecognized tab!! something changed with the navigation tabs");
        }
    }

    @Override
    public void onToolSelect(@Nullable final String code, @NonNull final Tool.Type type,
                             @Nullable Locale... languages) {
        // short-circuit if we don't have a valid tool code
        if (code == null) {
            return;
        }

        // sanitize the languages list, and short-circuit if we don't have any languages
        if (languages != null) {
            languages = Stream.of(languages).withoutNulls().toArray(Locale[]::new);
        }
        if (languages == null || languages.length == 0) {
            return;
        }

        // start pre-loading the tool in the first language
        mManifestManager.get().preloadLatestPublishedManifest(code, languages[0]);

        ActivityUtilsKt.openToolActivity(this, code, type, languages, false);
    }

    @Override
    public void onToolInfo(@Nullable final String code) {
        if (code != null) {
            ToolDetailsActivityKt.startToolDetailsActivity(this, code);
        }
    }

    @Override
    public void onNoToolsAvailableAction() {
        showAllTools();
    }
    // endregion Lifecycle

    // region Onboarding
    private void triggerOnboardingIfNecessary() {
        // TODO: remove this once we support onboarding in all languages
        // mark OnBoarding as discovered if this isn't a supported language
        final Settings settings = getSettings();
        if (!PageSet.ONBOARDING.supportsLocale(LocaleUtils.getDeviceLocale(this))) {
            settings.setFeatureDiscovered(FEATURE_TUTORIAL_ONBOARDING);
        }

        if (!settings.isFeatureDiscovered(FEATURE_TUTORIAL_ONBOARDING)) {
            TutorialActivityKt.startTutorialActivity(this, PageSet.ONBOARDING);
        }
    }
    // endregion Onboarding
}
