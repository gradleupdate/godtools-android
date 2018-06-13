package org.cru.godtools.tract.model;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.text.TextUtilsCompat;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.annimon.stream.Stream;

import org.cru.godtools.base.model.Event;
import org.cru.godtools.tract.R;
import org.cru.godtools.tract.analytics.model.ContentAnalyticsActionEvent;
import org.cru.godtools.tract.model.Text.Align;
import org.greenrobot.eventbus.EventBus;
import org.xmlpull.v1.XmlPullParser;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import butterknife.ButterKnife;

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

    @UiThread
    abstract static class BaseViewHolder<T extends Base> {
        private final Handler mHandler;

        @Nullable
        final BaseViewHolder mParentViewHolder;
        @NonNull
        public final View mRoot;

        @NonNull
        final Class<T> mModelType;
        @Nullable
        T mModel;

        boolean mVisible = false;

        BaseViewHolder(@NonNull final Class<T> modelType, @NonNull final ViewGroup parent, @LayoutRes final int layout,
                       @Nullable final BaseViewHolder parentViewHolder) {
            this(modelType, LayoutInflater.from(parent.getContext()).inflate(layout, parent, false), parentViewHolder);
        }

        BaseViewHolder(@NonNull final Class<T> modelType, @NonNull final View root,
                       @Nullable final BaseViewHolder parentViewHolder) {
            mHandler = new Handler(Looper.getMainLooper());

            mParentViewHolder = parentViewHolder;
            mModelType = modelType;
            mRoot = root;
            ButterKnife.bind(this, mRoot);
            mRoot.setTag(R.id.view_holder, this);
        }

        @Nullable
        public static BaseViewHolder forView(@NonNull final View view) {
            return forView(view, BaseViewHolder.class);
        }

        @Nullable
        public static <T extends BaseViewHolder> T forView(@Nullable final View view, @NonNull final Class<T> clazz) {
            if (view != null) {
                final Object holder = view.getTag(R.id.view_holder);
                if (clazz.isInstance(holder)) {
                    return clazz.cast(holder);
                }
            }

            return null;
        }

        /* BEGIN lifecycle */

        @CallSuper
        void onBind() {
            // update the layout direction for this view
            // HACK: In theory we should be able to set this on the root page only.
            // HACK: But updating the direction doesn't seem to trigger a re-layout of descendant views.
            ViewCompat.setLayoutDirection(mRoot, CallToAction.getLayoutDirection(mModel));
        }

        @CallSuper
        void onVisible() {}

        boolean onValidate() {
            // default to being valid
            return true;
        }

        void onBuildEvent(@NonNull final Event.Builder builder, final boolean recursive) {}

        @CallSuper
        void onHidden() {}

        /* END lifecycle */

        @Nullable
        public final T getModel() {
            return mModel;
        }

        public final void bind(@Nullable final T model) {
            if (model == null) {
                markHidden();
            }
            final T old = mModel;
            mModel = model;
            if (old != mModel) {
                onBind();
            }
        }

        public final void markVisible() {
            if (!mVisible && mModel != null) {
                mVisible = true;
                onVisible();
            }
        }

        public final void markHidden() {
            if (mVisible && mModel != null) {
                mVisible = false;
                onHidden();
            }
        }

        final void sendEvents(@NonNull final Set<Event.Id> ids) {
            // short-circuit if there are no events being triggered
            if (ids.isEmpty()) {
                return;
            }

            // short-circuit if validation fails when it's required
            if (!validate(ids)) {
                return;
            }

            // try letting a parent build the event object
            final Event.Builder builder = Event.builder();
            if (mModel != null) {
                builder.locale(mModel.getManifest().getLocale());
            }
            if (!buildEvent(builder)) {
                // populate the event with our local state since it wasn't populated by a parent
                onBuildEvent(builder, false);
            }

            // trigger an event for every id provided
            Stream.of(ids)
                    .map(builder::id)
                    .map(Event.Builder::build)
                    .forEach(EventBus.getDefault()::post);
        }

        /**
         * Trigger the specified analytics events.
         *
         * @param events All analytics events
         * @param types  The types of analytics events to actually trigger
         * @return Any pending analytics events.
         */
        @NonNull
        final List<Runnable> triggerAnalyticsEvents(final Collection<AnalyticsEvent> events,
                                                    final AnalyticsEvent.Trigger... types) {
            return Stream.of(events)
                    .filter(e -> e.isTriggerType(types))
                    .map(this::sendAnalyticsEvent)
                    .withoutNulls()
                    .toList();
        }

        @Nullable
        private Runnable sendAnalyticsEvent(@NonNull final AnalyticsEvent event) {
            if (event.getDelay() > 0) {
                final Runnable task = () -> EventBus.getDefault().post(new ContentAnalyticsActionEvent(event));
                mHandler.postDelayed(task, event.getDelay() * 1000);
                return task;
            }

            EventBus.getDefault().post(new ContentAnalyticsActionEvent(event));
            return null;
        }

        final void cancelPendingAnalyticsEvents(@NonNull final List<Runnable> pendingTasks) {
            Stream.of(pendingTasks)
                    .forEach(mHandler::removeCallbacks);
        }

        /**
         * @return true if the event has been built by a parent view holder.
         */
        boolean buildEvent(@NonNull final Event.Builder builder) {
            return mParentViewHolder != null && mParentViewHolder.buildEvent(builder);
        }

        boolean validate(@NonNull final Set<Event.Id> ids) {
            // navigate up hierarchy before performing validation
            if (mParentViewHolder != null) {
                return mParentViewHolder.validate(ids);
            }

            // no validation is necessary
            return true;
        }
    }
}
