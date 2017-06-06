package org.cru.godtools.tract.model;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ccci.gto.android.common.util.NumberUtils;
import org.ccci.gto.android.common.util.XmlPullParserUtils;
import org.cru.godtools.tract.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import butterknife.ButterKnife;

import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static org.cru.godtools.tract.Constants.XMLNS_CONTENT;
import static org.cru.godtools.tract.model.Utils.parseColor;

public final class Text extends Content {
    static final String XML_TEXT = "text";
    private static final String XML_TEXT_SCALE = "text-scale";

    private static final double DEFAULT_TEXT_SCALE = 1.0;

    @ColorInt
    @Nullable
    private Integer mTextColor = null;
    @Nullable
    private Double mTextScale = null;

    @Nullable
    private String mText;

    static Text fromXml(@NonNull final Base parent, @NonNull final XmlPullParser parser)
            throws IOException, XmlPullParserException {
        return new Text(parent).parse(parser);
    }

    @Nullable
    static Text fromNestedXml(@NonNull final Base parent, @NonNull final XmlPullParser parser,
                              @Nullable final String parentNamespace, @NonNull final String parentName)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, parentNamespace, parentName);

        // process any child elements
        Text text = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            // process recognized nodes
            switch (parser.getNamespace()) {
                case XMLNS_CONTENT:
                    switch (parser.getName()) {
                        case XML_TEXT:
                            text = fromXml(parent, parser);
                            continue;
                    }
                    break;
            }

            // skip unrecognized nodes
            XmlPullParserUtils.skipTag(parser);
        }

        return text;
    }

    private Text(@NonNull final Base parent) {
        super(parent);
    }

    @ColorInt
    public int getTextColor(@ColorInt final int defColor) {
        return mTextColor != null ? mTextColor : defColor;
    }

    private double getTextScale(final double defScale) {
        return mTextScale != null ? mTextScale : defScale;
    }

    @Nullable
    public String getText() {
        return mText;
    }

    @WorkerThread
    private Text parse(@NonNull final XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, XMLNS_CONTENT, XML_TEXT);

        mTextColor = parseColor(parser, XML_TEXT_COLOR, mTextColor);
        mTextScale = NumberUtils.toDouble(parser.getAttributeValue(null, XML_TEXT_SCALE), mTextScale);

        mText = XmlPullParserUtils.safeNextText(parser);
        return this;
    }

    public static void bind(@Nullable final Text text, @Nullable final TextView view) {
        bind(text, view, Page.getTextColor(text != null ? text.getPage() : null));
    }

    public static void bind(@Nullable final Text text, @Nullable final TextView view,
                            @ColorInt final int defaultTextColor) {
        if (view != null) {
            final float textSize = view.getContext().getResources().getDimension(R.dimen.text_size_base);
            bind(text, view, defaultTextColor, textSize, DEFAULT_TEXT_SCALE);
        }
    }

    public static void bind(@Nullable final Text text, @Nullable final TextView view,
                            @ColorInt final int defaultTextColor, final float textSize) {
        bind(text, view, defaultTextColor, textSize, DEFAULT_TEXT_SCALE);
    }

    public static void bind(@Nullable final Text text, @Nullable final TextView view,
                            @ColorInt final int defaultTextColor, final float textSize, final double defaultTextScale) {
        if (view != null) {
            if (text != null) {
                view.setText(text.mText);
                view.setTextSize(COMPLEX_UNIT_PX, (float) (textSize * text.getTextScale(defaultTextScale)));
                view.setTextColor(text.getTextColor(defaultTextColor));
            } else {
                view.setText(null);
                view.setTextSize(COMPLEX_UNIT_PX, (float) (textSize * defaultTextScale));
                view.setTextColor(defaultTextColor);
            }
        }
    }

    @NonNull
    @Override
    public View render(@NonNull final LinearLayout parent) {
        final View view =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.tract_content_text, parent, false);

        final TextView content = ButterKnife.findById(view, R.id.content);
        if (content != null) {
            bind(this, content);
        }

        return view;
    }
}