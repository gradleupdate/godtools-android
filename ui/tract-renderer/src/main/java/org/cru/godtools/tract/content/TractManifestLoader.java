package org.cru.godtools.tract.content;

import android.content.Context;
import android.support.annotation.NonNull;

import org.ccci.gto.android.common.eventbus.content.CachingAsyncTaskEventBusLoader;
import org.cru.godtools.model.loader.TranslationEventBusSubscriber;
import org.cru.godtools.xml.model.Manifest;
import org.cru.godtools.xml.service.ManifestManager;

import java.util.Locale;

public class TractManifestLoader extends CachingAsyncTaskEventBusLoader<Manifest> {
    private final ManifestManager mManifestManager;

    @NonNull
    private final String mTool;
    @NonNull
    private final Locale mLocale;

    public TractManifestLoader(@NonNull final Context context, @NonNull final String toolCode,
                               @NonNull final Locale locale) {
        super(context);
        mManifestManager = ManifestManager.getInstance(context);
        mTool = toolCode;
        mLocale = locale;

        addEventBusSubscriber(new TranslationEventBusSubscriber(this));
    }

    @NonNull
    public Locale getLocale() {
        return mLocale;
    }

    @Override
    public Manifest loadInBackground() {
        try {
            return mManifestManager.getLatestPublishedManifest(mTool, mLocale).get();
        } catch (final Exception ignored) {
            return null;
        }
    }
}
