package org.cru.godtools.tract.model;

import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.text.TextUtilsCompat;
import android.support.v4.view.ViewCompat;

import org.cru.godtools.base.model.Event;
import org.cru.godtools.tract.model.Text.Align;
import org.xmlpull.v1.XmlPullParser;

import java.util.Set;

abstract class Base {
    static final String XML_PRIMARY_COLOR = "primary-color";
    static final String XML_PRIMARY_TEXT_COLOR = "primary-text-color";
    static final String XML_TEXT_COLOR = "text-color";
    static final String XML_BACKGROUND_COLOR = "background-color";
    static final String XML_BACKGROUND_IMAGE = "background-image";
    static final String XML_BACKGROUND_IMAGE_GRAVITY = "background-image-align";
    static final String XML_BACKGROUND_IMAGE_SCALE_TYPE = "background-image-scale-type";

    static final String XML_EVENTS = "events";
    static final String XML_LISTENERS = "listeners";
    static final String XML_DISMISS_LISTENERS = "dismiss-listeners";

    @NonNull
    private final Base mParent;

    Base() {
        mParent = this;
    }

    Base(@NonNull final Base parent) {
        mParent = parent;
    }

    @NonNull
    public Manifest getManifest() {
        if (mParent == this) {
            throw new IllegalStateException();
        } else {
            return mParent.getManifest();
        }
    }

    public int getLayoutDirection() {
        return TextUtilsCompat.getLayoutDirectionFromLocale(getManifest().getLocale());
    }

    public static int getLayoutDirection(@Nullable final Base base) {
        return base != null ? base.getLayoutDirection() : ViewCompat.LAYOUT_DIRECTION_INHERIT;
    }

    private String getDefaultEventNamespace() {
        return getManifest().getCode();
    }

    @Nullable
    Resource getResource(@Nullable final String name) {
        return getManifest().getResource(name);
    }

    @NonNull
    protected Page getPage() {
        if (mParent == this) {
            throw new IllegalStateException();
        } else {
            return mParent.getPage();
        }
    }

    @Nullable
    Styles getStylesParent() {
        if (mParent == this) {
            return null;
        } else if (mParent instanceof Styles) {
            return (Styles) mParent;
        } else {
            return mParent.getStylesParent();
        }
    }

    @Nullable
    static Styles getStylesParent(@Nullable final Base obj) {
        return obj != null ? obj.getStylesParent() : null;
    }

    /* BEGIN: Styles default methods */

    @ColorInt
    public int getPrimaryColor() {
        return Styles.getPrimaryColor(getStylesParent());
    }

    @ColorInt
    public int getPrimaryTextColor() {
        return Styles.getPrimaryTextColor(getStylesParent());
    }

    @ColorInt
    public int getTextColor() {
        return Styles.getTextColor(getStylesParent());
    }

    @DimenRes
    public int getTextSize() {
        return Styles.getTextSize(getStylesParent());
    }

    @NonNull
    public Align getTextAlign() {
        return Styles.getTextAlign(getStylesParent());
    }

    @ColorInt
    public int getButtonColor() {
        return getPrimaryColor();
    }

    /* END: Styles default methods */

    @NonNull
    final Set<Event.Id> parseEvents(@NonNull final XmlPullParser parser, @NonNull final String attribute) {
        final String raw = parser.getAttributeValue(null, attribute);
        return Event.Id.parse(getDefaultEventNamespace(), raw);
    }
}
