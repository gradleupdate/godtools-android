package org.keynote.godtools.android.sync;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v4.util.SimpleArrayMap;

import org.greenrobot.eventbus.EventBus;
import org.keynote.godtools.android.api.GodToolsApi;
import org.keynote.godtools.android.db.GodToolsDao;

import static android.content.ContentResolver.SYNC_EXTRAS_MANUAL;

@WorkerThread
abstract class BaseSyncTasks {
    private final Context mContext;
    final GodToolsApi mApi;
    final GodToolsDao mDao;
    final EventBus mEventBus;

    BaseSyncTasks(@NonNull final Context context) {
        mContext = context;
        mApi = GodToolsApi.getInstance(mContext);
        mDao = GodToolsDao.getInstance(mContext);
        mEventBus = EventBus.getDefault();
    }

    static boolean isForced(@NonNull final Bundle extras) {
        return extras.getBoolean(SYNC_EXTRAS_MANUAL, false);
    }

    void coalesceEvent(@NonNull final SimpleArrayMap<Class<?>, Object> events, @NonNull final Object event) {
        final Class<?> type = event.getClass();
        Object currEvent = events.get(type);
        if (currEvent != null) {
            // coalesce any events that need to be coalesced
        } else {
            currEvent = event;
        }
        events.put(type, currEvent);
    }

    void sendEvents(@NonNull final SimpleArrayMap<Class<?>, Object> events) {
        for (int i = 0; i < events.size(); i++) {
            mEventBus.post(events.valueAt(i));
        }
        events.clear();
    }
}
