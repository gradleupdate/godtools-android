package org.cru.godtools.analytics;

import android.content.Context;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.cru.godtools.base.model.Event;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class AnalyticsService {
    /* Screen event names */
    public static final String SCREEN_HOME = "Home";
    public static final String SCREEN_ADD_TOOLS = "Add Tools";
    public static final String SCREEN_TOOL_DETAILS = "Tool Info";
    public static final String SCREEN_LANGUAGE_SETTINGS = "Language Settings";
    public static final String SCREEN_LANGUAGE_SELECTION = "Select Language";
    public static final String SCREEN_MENU = "Menu";
    public static final String SCREEN_ABOUT = "About";
    public static final String SCREEN_HELP = "Help";
    public static final String SCREEN_CONTACT_US = "Contact Us";
    public static final String SCREEN_SHARE_GODTOOLS = "Share App";
    public static final String SCREEN_SHARE_STORY = "Share Story";
    public static final String SCREEN_TERMS_OF_USE = "Terms of Use";
    public static final String SCREEN_PRIVACY_POLICY = "Privacy Policy";
    public static final String SCREEN_COPYRIGHT = "Copyright Info";

    /* Custom dimensions */
    private static final int DIMENSION_TOOL = 1;
    private static final int DIMENSION_LANGUAGE = 2;

    /* Legacy constants */
    public static final String SCREEN_EVERYSTUDENT = "EveryStudent";
    public static final String CATEGORY_MENU = "Menu Event";
    public static final String CATEGORY_CONTENT_EVENT = "Content Event";
    private Tracker mTracker = null;

    private AnalyticsService(@NonNull final Context context) {
        mTracker = GoogleAnalytics.getInstance(context).newTracker(BuildConfig.GOOGLE_ANALYTICS_CLIENT_ID);
        EventBus.getDefault().register(this);
    }

    @Nullable
    private static AnalyticsService sInstance;
    @NonNull
    public static synchronized AnalyticsService getInstance(@NonNull final Context context) {
        if (sInstance == null) {
            sInstance = new AnalyticsService(context.getApplicationContext());
        }

        return sInstance;
    }

    public void trackScreen(@NonNull final String screen) {
        mTracker.setScreenName(screen);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void screenView(@NonNull final String name, @NonNull final String language) {
        mTracker.setScreenName(name);
        mTracker.send(new HitBuilders.ScreenViewBuilder()
                .setCustomDimension(DIMENSION_LANGUAGE, language)
                .build());

    }

    public void settingChanged(@NonNull final String category, @NonNull final String event) {

        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(event)
                .setLabel(event)
                .build());

    }

    public void trackEveryStudentSearch(@NonNull final String query) {
        mTracker.setScreenName("everystudent-search");
        mTracker.send(new HitBuilders.EventBuilder()
                              .setCategory("searchbar")
                              .setAction("tap")
                              .setLabel(query)
                              .build());
    }

    public void menuEvent(@NonNull final String item) {

        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(CATEGORY_MENU)
                .setAction(item)
                .setLabel(item)
                .build());

    }

    @MainThread
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void trackContentEvent(@NonNull final Event event) {
        mTracker.send(new HitBuilders.EventBuilder()
                              .setCategory(CATEGORY_CONTENT_EVENT)
                              .setAction(event.id.namespace + ":" + event.id.name)
                              .build());
    }
}