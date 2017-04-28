package org.keynote.godtools.android.content;

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v4.content.Loader;

import org.ccci.gto.android.common.eventbus.content.EventBusSubscriber;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.keynote.godtools.android.event.LanguageUpdateEvent;

public final class LanguageEventBusSubscriber extends EventBusSubscriber {
    LanguageEventBusSubscriber(@NonNull final Loader loader) {
        super(loader);
    }

    @MainThread
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLanguageUpdateEvent(@NonNull final LanguageUpdateEvent event) {
        triggerLoad();
    }
}