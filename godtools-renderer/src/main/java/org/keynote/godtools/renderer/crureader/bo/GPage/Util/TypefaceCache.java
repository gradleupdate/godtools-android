package org.keynote.godtools.renderer.crureader.bo.GPage.Util;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;

import org.keynote.godtools.renderer.crureader.bo.GPage.RenderHelpers.RenderSingleton;

/**
 * Created by ryancarlson on 4/17/14.
 * <p/>
 * Source: https://code.google.com/p/android/issues/detail?id=9904#c7
 * (Format altered for better style.)
 */
class TypefaceCache {
    private static final LruCache<String, Typeface> TYPEFACE_CACHE = new LruCache<>(5);

    @Nullable
    public static Typeface get(@NonNull final Context c, @NonNull final String path) {
        synchronized (TYPEFACE_CACHE) {
            Typeface tf = TYPEFACE_CACHE.get(path);
            if (tf == null) {
                try {
                    tf = Typeface.createFromAsset(c.getAssets(), path);
                    TYPEFACE_CACHE.put(path, tf);
                } catch (@NonNull final Exception e) {
                    RenderSingleton.getInstance().getAppConfig().overLog("Unable to load custom typeface: " + path);
                    RenderSingleton.getInstance().getAppConfig().logException(e);
                    return null;
                }
            }

            return tf;
        }
    }
}
