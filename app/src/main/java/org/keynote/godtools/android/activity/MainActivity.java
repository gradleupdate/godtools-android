package org.keynote.godtools.android.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import com.annimon.stream.Stream;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;

import org.cru.godtools.BuildConfig;
import org.cru.godtools.R;
import org.cru.godtools.activity.BasePlatformActivity;
import org.cru.godtools.activity.ToolDetailsActivity;
import org.cru.godtools.analytics.model.AnalyticsScreenEvent;
import org.cru.godtools.article.activity.CategoriesActivity;
import org.cru.godtools.fragment.ToolsFragment;
import org.cru.godtools.model.Tool;
import org.cru.godtools.sync.GodToolsSyncService;
import org.cru.godtools.tract.activity.TractActivity;
import org.cru.godtools.xml.service.ManifestManager;

import java.util.Locale;

import me.thekey.android.core.CodeGrantAsyncTask;

import static android.arch.lifecycle.Lifecycle.State.RESUMED;
import static android.arch.lifecycle.Lifecycle.State.STARTED;
import static org.cru.godtools.analytics.model.AnalyticsScreenEvent.SCREEN_FIND_TOOLS;
import static org.cru.godtools.analytics.model.AnalyticsScreenEvent.SCREEN_HOME;
import static org.cru.godtools.base.Settings.FEATURE_LANGUAGE_SETTINGS;
import static org.cru.godtools.model.Tool.CODE_EVERYSTUDENT;

public class MainActivity extends BasePlatformActivity implements ToolsFragment.Callbacks {
    private static final String EXTRA_FEATURE_DISCOVERY = MainActivity.class.getName() + ".FEATURE_DISCOVERY";
    private static final String EXTRA_ACTIVE_STATE = MainActivity.class.getName() + ".ACTIVE_STATE";
    private static final String EXTRA_FEATURE = MainActivity.class.getName() + ".FEATURE";
    private static final String EXTRA_FORCE = MainActivity.class.getName() + ".FORCE";

    private static final String TAG_MAIN_FRAGMENT = "mainFragment";

    private static final int TASK_FEATURE_DISCOVERY = 1;

    private static final int STATE_MY_TOOLS = 0;
    private static final int STATE_FIND_TOOLS = 1;

    @NonNull
    /*final*/ Handler mTaskHandler;

    @Nullable
    private TabLayout.Tab mMyToolsTab;
    @Nullable
    private TabLayout.Tab mFindToolsTab;
    @Nullable
    TapTargetView mFeatureDiscovery;

    private int mActiveState = STATE_MY_TOOLS;
    @Nullable
    String mFeatureDiscoveryActive;

    // region Lifecycle Events

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTaskHandler = new Handler(this::onHandleMessage);
        setContentView(R.layout.activity_dashboard);

        processIntent(getIntent());

        if (savedInstanceState != null) {
            mActiveState = savedInstanceState.getInt(EXTRA_ACTIVE_STATE, mActiveState);
            mFeatureDiscoveryActive = savedInstanceState.getString(EXTRA_FEATURE_DISCOVERY, mFeatureDiscoveryActive);
        }

        // sync any pending updates
        syncData();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadInitialFragmentIfNeeded();
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        trackInAnalytics();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (mFeatureDiscoveryActive != null) {
            showFeatureDiscovery(mFeatureDiscoveryActive, true);
        } else {
            showNextFeatureDiscovery();
        }
    }

    boolean onHandleMessage(@NonNull final Message message) {
        switch (message.what) {
            case TASK_FEATURE_DISCOVERY:
                showFeatureDiscovery(message);
                return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                showAddTools();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(final TabLayout.Tab tab) {
        if (tab == mMyToolsTab) {
            showMyTools();
        } else if (tab == mFindToolsTab) {
            showAddTools();
        } else if (BuildConfig.DEBUG) {
            // The tab selection logic is brittle, so throw an error in unrecognized scenarios
            throw new IllegalArgumentException("Unrecognized tab!! something changed with the navigation tabs");
        }
    }

    @Override
    public void onToolSelect(@Nullable final String code, @NonNull final Tool.Type type, Locale... languages) {
        if (mActiveState == STATE_FIND_TOOLS) {
            ToolDetailsActivity.start(this, code);
            return;
        }

        if (code != null) {
            switch (type) {
                case TRACT:
                    if (languages != null) {
                        languages = Stream.of(languages).withoutNulls().toArray(Locale[]::new);
                        if (languages.length > 0) {
                            // start preloading the tract in the first language
                            ManifestManager.getInstance(this).getLatestPublishedManifest(code, languages[0]);

                            TractActivity.start(this, code, languages);
                        }
                    }
                    break;
                case ARTICLE:
                    // hardcode everystudent content for now
                    if (CODE_EVERYSTUDENT.equals(code)) {
//                        EveryStudent.start(this);
                        CategoriesActivity.start(this, code, languages[0]);
                    }
            }
        }
    }

    @Override
    public void onToolInfo(@Nullable final String code) {
        ToolDetailsActivity.start(this, code);
    }

    @Override
    public void onNoToolsAvailableAction() {
        showAddTools();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_ACTIVE_STATE, mActiveState);
        outState.putString(EXTRA_FEATURE_DISCOVERY, mFeatureDiscoveryActive);
    }

    @Override
    protected void onDestroy() {
        mTaskHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // endregion Lifecycle Events

    private void processIntent(@Nullable final Intent intent) {
        final String action = intent != null ? intent.getAction() : null;
        final Uri data = intent != null ? intent.getData() : null;
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            if (getString(R.string.account_deeplink_host).equalsIgnoreCase(data.getHost())) {
                if (getString(R.string.account_deeplink_path).equalsIgnoreCase(data.getPath())) {
                    new CodeGrantAsyncTask(mTheKey, data).execute();
                    intent.setData(null);
                }
            }
        }
    }

    @Override
    protected void setupNavigationTabs() {
        super.setupNavigationTabs();
        if (mNavigationTabs != null) {
            // This logic is brittle, so throw an error on debug builds if something changes.
            if (BuildConfig.DEBUG && mNavigationTabs.getTabCount() != 2) {
                throw new IllegalStateException("The navigation tabs changed!!! Logic needs to be updated!!!");
            }

            mMyToolsTab = mNavigationTabs.getTabAt(0);
            mFindToolsTab = mNavigationTabs.getTabAt(1);
        }
    }

    private void trackInAnalytics() {
        // only track analytics if this activity has been started
        if (getLifecycle().getCurrentState().isAtLeast(STARTED)) {
            switch (mActiveState) {
                case STATE_FIND_TOOLS:
                    mEventBus.post(new AnalyticsScreenEvent(SCREEN_FIND_TOOLS));
                    break;
                case STATE_MY_TOOLS:
                default:
                    mEventBus.post(new AnalyticsScreenEvent(SCREEN_HOME));
            }
        }
    }

    private void showAddTools() {
        // update the displayed fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame, ToolsFragment.newInstance(ToolsFragment.MODE_AVAILABLE), TAG_MAIN_FRAGMENT)
                .commit();

        selectNavigationTabIfNecessary(mFindToolsTab);
        mActiveState = STATE_FIND_TOOLS;
        trackInAnalytics();
    }

    private void showMyTools() {
        // update the displayed fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame, ToolsFragment.newInstance(ToolsFragment.MODE_ADDED), TAG_MAIN_FRAGMENT)
                .commit();

        selectNavigationTabIfNecessary(mMyToolsTab);
        mActiveState = STATE_MY_TOOLS;
        trackInAnalytics();
    }

    private void syncData() {
        GodToolsSyncService.syncFollowups(this).sync();
        GodToolsSyncService.syncToolShares(this).sync();
    }

    @Override
    protected boolean showNavigationDrawerIndicator() {
        return true;
    }

    @MainThread
    private void loadInitialFragmentIfNeeded() {
        final FragmentManager fm = getSupportFragmentManager();

        // short-circuit if there is a currently attached fragment
        Fragment fragment = fm.findFragmentByTag(TAG_MAIN_FRAGMENT);
        if (fragment != null) {
            return;
        }

        // default to My Tools
        showMyTools();
    }

    void showNextFeatureDiscovery() {
        if (!prefs().isFeatureDiscovered(FEATURE_LANGUAGE_SETTINGS) &&
                canShowFeatureDiscovery(FEATURE_LANGUAGE_SETTINGS)) {
            dispatchDelayedFeatureDiscovery(FEATURE_LANGUAGE_SETTINGS, false, 15000);
        }
    }

    /**
     * Returns if the activity is in a state that it can actually show the specified feature discovery.
     */
    private boolean canShowFeatureDiscovery(@NonNull final String feature) {
        switch (feature) {
            case FEATURE_LANGUAGE_SETTINGS:
                return mToolbar != null && (mDrawerLayout == null || !mDrawerLayout.isDrawerOpen(Gravity.START));
        }

        // assume we can show it if we don't have any specific rules about it
        return true;
    }

    private void showFeatureDiscovery(final Message message) {
        final Bundle data = message.getData();
        final String feature = data.getString(EXTRA_FEATURE);
        if (feature != null) {
            showFeatureDiscovery(feature, data.getBoolean(EXTRA_FORCE, false));
        }
    }

    private void showFeatureDiscovery(@NonNull final String feature, final boolean force) {
        // short-circuit if this activity is not started
        if (!getLifecycle().getCurrentState().isAtLeast(RESUMED)) {
            return;
        }

        // short-circuit if feature discovery is already visible
        if (mFeatureDiscovery != null) {
            return;
        }

        // short-circuit if this feature was discovered and we aren't forcing it
        if (prefs().isFeatureDiscovered(feature) && !force) {
            return;
        }

        // short-circuit if we can't show this feature discovery right now,
        // and try to show the next feature discovery that can be shown.
        if (!canShowFeatureDiscovery(feature)) {
            showNextFeatureDiscovery();
            return;
        }

        // dispatch specific feature discovery
        switch (feature) {
            case FEATURE_LANGUAGE_SETTINGS:
                assert mToolbar != null : "canShowFeatureDiscovery() verifies mToolbar is not null";
                if (mToolbar.findViewById(R.id.action_switch_language) != null) {
                    // purge any pending feature discovery triggers since we are showing feature discovery now
                    purgeQueuedFeatureDiscovery(FEATURE_LANGUAGE_SETTINGS);

                    // show language settings feature discovery
                    final TapTarget target = TapTarget.forToolbarMenuItem(
                            mToolbar, R.id.action_switch_language,
                            getString(R.string.feature_discovery_title_language_settings),
                            getString(R.string.feature_discovery_desc_language_settings));
                    mFeatureDiscovery =
                            TapTargetView.showFor(this, target, new LanguageSettingsFeatureDiscoveryListener());
                    mFeatureDiscoveryActive = feature;
                } else {
                    // TODO: we currently don't (can't?) distinguish between when the menu item doesn't exist and when
                    // TODO: the menu item just hasn't been drawn yet.

                    // the toolbar action isn't available yet.
                    // re-attempt this feature discovery on the next frame iteration.
                    dispatchDelayedFeatureDiscovery(feature, force, 17);
                }
                break;
        }
    }

    private void dispatchDelayedFeatureDiscovery(@NonNull final String feature, final boolean force, final long delay) {
        final Message msg = mTaskHandler.obtainMessage(TASK_FEATURE_DISCOVERY, feature);
        final Bundle data = new Bundle();
        data.putString(EXTRA_FEATURE, feature);
        data.putBoolean(EXTRA_FORCE, force);
        msg.setData(data);
        mTaskHandler.sendMessageDelayed(msg, delay);
    }

    private void purgeQueuedFeatureDiscovery(@NonNull final String feature) {
        mTaskHandler.removeMessages(TASK_FEATURE_DISCOVERY, feature);
    }

    private class LanguageSettingsFeatureDiscoveryListener extends TapTargetView.Listener {
        @Override
        public void onTargetClick(final TapTargetView view) {
            super.onTargetClick(view);
            showLanguageSettings();
        }

        @Override
        public void onOuterCircleClick(final TapTargetView view) {
            onTargetCancel(view);
        }

        @Override
        public void onTargetDismissed(final TapTargetView view, final boolean userInitiated) {
            super.onTargetDismissed(view, userInitiated);
            if (userInitiated) {
                prefs().setFeatureDiscovered(FEATURE_LANGUAGE_SETTINGS);
                mFeatureDiscoveryActive = null;
                showNextFeatureDiscovery();
            }

            if (view == mFeatureDiscovery) {
                mFeatureDiscovery = null;
            }
        }
    }
}
