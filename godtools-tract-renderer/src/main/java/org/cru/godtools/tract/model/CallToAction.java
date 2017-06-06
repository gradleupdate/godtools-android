package org.cru.godtools.tract.model;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.ccci.gto.android.common.util.XmlPullParserUtils;
import org.cru.godtools.tract.R;
import org.cru.godtools.tract.util.DrawableUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

import static org.cru.godtools.tract.Constants.XMLNS_CONTENT;
import static org.cru.godtools.tract.Constants.XMLNS_TRACT;
import static org.cru.godtools.tract.model.Text.XML_TEXT;

public final class CallToAction extends Base {
    static final String XML_CALL_TO_ACTION = "call-to-action";
    private static final String XML_EVENT = "event";

    public interface Callbacks {
        void goToNextPage();
    }

    @Nullable
    private Text mLabel;

    private final List<Object> mEvents = new ArrayList<>();

    CallToAction(@NonNull final Base parent) {
        super(parent);
    }

    @ColorInt
    private static int getArrowColor(@Nullable final CallToAction callToAction) {
        return Page.getPrimaryColor(callToAction != null ? callToAction.getPage() : null);
    }

    @NonNull
    static CallToAction fromXml(@NonNull final Base parent, @NonNull final XmlPullParser parser)
            throws IOException, XmlPullParserException {
        return new CallToAction(parent).parse(parser);
    }

    @NonNull
    private CallToAction parse(@NonNull final XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, XMLNS_TRACT, XML_CALL_TO_ACTION);

        // process any child elements
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            // process recognized nodes
            switch (parser.getNamespace()) {
                case XMLNS_CONTENT:
                    switch (parser.getName()) {
                        case XML_TEXT:
                            mLabel = Text.fromXml(this, parser);
                            continue;
                    }
                    break;
            }

            // skip unrecognized nodes
            XmlPullParserUtils.skipTag(parser);
        }

        return this;
    }

    public static void bind(@Nullable final CallToAction callToAction, @Nullable final View view,
                            @Nullable final Callbacks callbacks) {
        if (view != null) {
            bindLabel(callToAction, ButterKnife.findById(view, R.id.call_to_action_label));
            bindArrow(callToAction, ButterKnife.findById(view, R.id.call_to_action_arrow), callbacks);
        }
    }

    private static void bindLabel(@Nullable final CallToAction callToAction, @Nullable final TextView mLabel) {
        if (mLabel != null) {
            Text.bind(callToAction != null ? callToAction.mLabel : null, mLabel);
        }
    }

    private static void bindArrow(@Nullable final CallToAction callToAction, @Nullable final ImageView arrow,
                                  @Nullable final Callbacks callbacks) {
        if (arrow != null) {
            final boolean visible =
                    callToAction == null || !callToAction.getPage().isLastPage() || !callToAction.mEvents.isEmpty();
            arrow.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            arrow.setImageDrawable(DrawableUtils.tint(arrow.getDrawable(), CallToAction.getArrowColor(callToAction)));
            arrow.setOnClickListener((v) -> CallToAction.trigger(callToAction, callbacks));
        }
    }

    static void trigger(@Nullable final CallToAction callToAction, @Nullable final Callbacks callbacks) {
        if (callbacks != null && (callToAction == null || callToAction.mEvents.isEmpty())) {
            callbacks.goToNextPage();
        } else if (callToAction != null) {
            //TODO: trigger events
        }
    }
}