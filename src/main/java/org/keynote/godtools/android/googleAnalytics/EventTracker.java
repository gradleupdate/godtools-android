package org.keynote.godtools.android.googleAnalytics;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.keynote.godtools.android.snuffy.SnuffyApplication;
import org.keynote.godtools.android.utils.GoogleAnalytics;

/**
 * This class is used to track events for Google Analytics.
 */
public class EventTracker {
    public static final String SCREEN_SETTINGS = "Settings";

    private static final int DIMENSION_SCREEN_NAME = 1;
    private static final int DIMENSION_LANGUAGE = 2;

    @NonNull
    private final Tracker mTracker;

    private static EventTracker instance;

    private EventTracker(@NonNull final Context context) {
        mTracker = GoogleAnalytics.getTracker(context);
    }

    @NonNull
    public static EventTracker getInstance(@NonNull final Context context) {
        synchronized (EventTracker.class) {
            if (instance == null) {
                instance = new EventTracker(context.getApplicationContext());
            }

            return instance;
        }
    }

    public void activeScreen(@NonNull final String screen) {
        mTracker.setScreenName(screen);
    }

    public void screenView(@NonNull final String name, @NonNull final String language) {
        mTracker.setScreenName(name);
        mTracker.send(new HitBuilders.ScreenViewBuilder()
                              .setCustomDimension(DIMENSION_SCREEN_NAME, name)
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

    /**
     * Track an event
     */
    public static void track(SnuffyApplication app, String screenName, String category, String event)
    {
        Tracker tracker = app.getTracker();
        tracker.setScreenName(screenName);
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(event)
                .setLabel(event)
                .build());
    }
}
