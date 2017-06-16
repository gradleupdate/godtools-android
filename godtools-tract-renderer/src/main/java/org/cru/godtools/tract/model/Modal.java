package org.cru.godtools.tract.model;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.ccci.gto.android.common.util.XmlPullParserUtils;
import org.cru.godtools.base.model.Event;
import org.cru.godtools.tract.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.cru.godtools.tract.Constants.XMLNS_TRACT;

public final class Modal extends Base implements Parent, StylesParent {
    static final String XML_MODAL = "modal";
    private static final String XML_TITLE = "title";
    private static final String XML_LISTENERS = "listeners";
    private static final String XML_DISMISS_LISTENERS = "dismiss-listeners";

    private final int mPosition;

    @NonNull
    private Set<Event.Id> mListeners = ImmutableSet.of();
    @NonNull
    private Set<Event.Id> mDismissListeners = ImmutableSet.of();

    @Nullable
    Text mTitle;

    @NonNull
    private List<Content> mContent = ImmutableList.of();

    private Modal(@NonNull final Base parent, final int position) {
        super(parent);
        mPosition = position;
    }

    @NonNull
    public String getId() {
        return getPage().getId() + "-" + mPosition;
    }

    @NonNull
    public Set<Event.Id> getListeners() {
        return mListeners;
    }

    @NonNull
    public Set<Event.Id> getDismissListeners() {
        return mDismissListeners;
    }

    @NonNull
    @Override
    public List<Content> getContent() {
        return mContent;
    }

    @Override
    public int getTextColor() {
        return Color.WHITE;
    }

    @Override
    public int getPrimaryColor() {
        return Color.TRANSPARENT;
    }

    @Override
    public int getPrimaryTextColor() {
        return Color.WHITE;
    }

    @Override
    public int getButtonColor() {
        return Color.WHITE;
    }

    @NonNull
    static Modal fromXml(@NonNull final Base parent, @NonNull final XmlPullParser parser, final int position)
            throws IOException, XmlPullParserException {
        return new Modal(parent, position).parse(parser);
    }

    @NonNull
    private Modal parse(@NonNull final XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, XMLNS_TRACT, XML_MODAL);

        mListeners = parseEvents(parser, XML_LISTENERS);
        mDismissListeners = parseEvents(parser, XML_DISMISS_LISTENERS);

        // process any child elements
        final ImmutableList.Builder<Content> contentList = ImmutableList.builder();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            // process recognized nodes
            switch (parser.getNamespace()) {
                case XMLNS_TRACT:
                    switch (parser.getName()) {
                        case XML_TITLE:
                            mTitle = Text.fromNestedXml(this, parser, XMLNS_TRACT, XML_TITLE);
                            continue;
                    }
                    break;
            }

            // try parsing this child element as a content node
            final Content content = Content.fromXml(this, parser);
            if (content != null) {
                contentList.add(content);
                continue;
            }

            // skip unrecognized nodes
            XmlPullParserUtils.skipTag(parser);
        }
        mContent = contentList.build();

        return this;
    }

    @NonNull
    public static ModalViewHolder getViewHolder(@NonNull final View root) {
        final Object holder = root.getTag(R.id.view_holder);
        if (holder instanceof ModalViewHolder) {
            return (ModalViewHolder) holder;
        } else {
            return new ModalViewHolder(root);
        }
    }

    public static class ModalViewHolder extends ParentViewHolder<Modal> {
        ModalViewHolder(@NonNull final View root) {
            super(Modal.class, root, null);
        }

        @Override
        void onBind() {
            super.onBind();
        }
    }
}
