package org.cru.godtools.tract.model;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.view.View;

import org.ccci.gto.android.common.picasso.view.SimplePicassoImageView;
import org.ccci.gto.android.common.util.XmlPullParserUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.cru.godtools.tract.Constants.XMLNS_MANIFEST;
import static org.cru.godtools.tract.Constants.XMLNS_TRACT;
import static org.cru.godtools.tract.model.CallToAction.XML_CALL_TO_ACTION;
import static org.cru.godtools.tract.model.Card.XML_CARD;
import static org.cru.godtools.tract.model.Header.XML_HEADER;
import static org.cru.godtools.tract.model.Hero.XML_HERO;
import static org.cru.godtools.tract.model.Utils.parseColor;

public final class Page extends Base {
    static final String XML_PAGE = "page";
    private static final String XML_MANIFEST_SRC = "src";
    private static final String XML_CARDS = "cards";

    @ColorInt
    private static final int DEFAULT_BACKGROUND_COLOR = Color.TRANSPARENT;
    private static final Align DEFAULT_BACKGROUND_IMAGE_ALIGN = Align.CENTER;

    private final int mPosition;

    @Nullable
    private String mLocalFileName;
    private boolean mPageXmlParsed = false;

    @Nullable
    @ColorInt
    private Integer mPrimaryColor = null;
    @Nullable
    @ColorInt
    private Integer mPrimaryTextColor = null;
    @Nullable
    @ColorInt
    private Integer mTextColor = null;
    @ColorInt
    private int mBackgroundColor = DEFAULT_BACKGROUND_COLOR;
    @Nullable
    private String mBackgroundImage;
    @NonNull
    private Align mBackgroundImageAlign = DEFAULT_BACKGROUND_IMAGE_ALIGN;

    @Nullable
    private Header mHeader;
    @Nullable
    private Hero mHero;
    private final List<Card> mCards = new ArrayList<>();
    @NonNull
    private CallToAction mCallToAction;

    private Page(@NonNull final Manifest manifest, final int position) {
        super(manifest);
        mPosition = position;
        mCallToAction = new CallToAction(this);
    }

    @NonNull
    @Override
    protected Page getPage() {
        return this;
    }

    public int getPosition() {
        return mPosition;
    }

    boolean isLastPage() {
        return mPosition == getManifest().getPages().size() - 1;
    }

    @Nullable
    public String getLocalFileName() {
        return mLocalFileName;
    }

    @ColorInt
    int getPrimaryColor() {
        return mPrimaryColor != null ? mPrimaryColor : getManifest().getPrimaryColor();
    }

    @ColorInt
    static int getPrimaryColor(@Nullable final Page page) {
        return page != null ? page.getPrimaryColor() : Manifest.getPrimaryColor(null);
    }

    @ColorInt
    int getPrimaryTextColor() {
        return mPrimaryTextColor != null ? mPrimaryTextColor : getManifest().getPrimaryTextColor();
    }

    @ColorInt
    private int getTextColor() {
        return mTextColor != null ? mTextColor : Manifest.getTextColor(getManifest());
    }

    @ColorInt
    static int getTextColor(@Nullable final Page page) {
        return page != null ? page.getTextColor() : Manifest.getTextColor(null);
    }

    @ColorInt
    public static int getBackgroundColor(@Nullable final Page page) {
        return page != null ? page.mBackgroundColor : DEFAULT_BACKGROUND_COLOR;
    }

    @Nullable
    public Header getHeader() {
        return mHeader;
    }

    @Nullable
    public Hero getHero() {
        return mHero;
    }

    @NonNull
    public List<Card> getCards() {
        return Collections.unmodifiableList(mCards);
    }

    @NonNull
    public CallToAction getCallToAction() {
        return mCallToAction;
    }

    @NonNull
    @WorkerThread
    static Page fromManifestXml(@NonNull final Manifest manifest, final int position,
                                @NonNull final XmlPullParser parser)
            throws XmlPullParserException, IOException {
        return new Page(manifest, position).parseManifestXml(parser);
    }

    @WorkerThread
    private Page parseManifestXml(@NonNull final XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, XMLNS_MANIFEST, XML_PAGE);

        mLocalFileName = parser.getAttributeValue(null, XML_MANIFEST_SRC);

        // discard any nested nodes
        XmlPullParserUtils.skipTag(parser);

        return this;
    }

    @WorkerThread
    public void parsePageXml(@NonNull final XmlPullParser parser) throws IOException, XmlPullParserException {
        // make sure we haven't parsed this page XML already
        if (mPageXmlParsed) {
            throw new IllegalStateException("Page XML already parsed");
        }
        parser.require(XmlPullParser.START_TAG, XMLNS_TRACT, XML_PAGE);

        mPrimaryColor = parseColor(parser, XML_PRIMARY_COLOR, mPrimaryColor);
        mPrimaryTextColor = parseColor(parser, XML_PRIMARY_TEXT_COLOR, mPrimaryTextColor);
        mTextColor = parseColor(parser, XML_TEXT_COLOR, mTextColor);
        mBackgroundColor = parseColor(parser, XML_BACKGROUND_COLOR, mBackgroundColor);
        mBackgroundImage = parser.getAttributeValue(null, XML_BACKGROUND_IMAGE);
        mBackgroundImageAlign = Align.parseAlign(parser, XML_BACKGROUND_IMAGE_ALIGN, mBackgroundImageAlign);

        // process any child elements
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            // process recognized nodes
            switch (parser.getNamespace()) {
                case XMLNS_TRACT:
                    switch (parser.getName()) {
                        case XML_HEADER:
                            mHeader = Header.fromXml(this, parser);
                            continue;
                        case XML_HERO:
                            mHero = Hero.fromXml(this, parser);
                            continue;
                        case XML_CARDS:
                            parseCardsXml(parser);
                            continue;
                        case XML_CALL_TO_ACTION:
                            mCallToAction = CallToAction.fromXml(this, parser);
                            continue;
                    }
                    break;
            }

            // skip unrecognized nodes
            XmlPullParserUtils.skipTag(parser);
        }

        // mark page XML as parsed
        mPageXmlParsed = true;
    }

    private void parseCardsXml(@NonNull final XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, XMLNS_TRACT, XML_CARDS);

        // process any child elements
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            // process recognized nodes
            switch (parser.getNamespace()) {
                case XMLNS_TRACT:
                    switch (parser.getName()) {
                        case XML_CARD:
                            mCards.add(Card.fromXml(this, parser));
                            continue;
                    }
                    break;
            }

            // skip unrecognized nodes
            XmlPullParserUtils.skipTag(parser);
        }
    }

    public static void bindBackgroundImage(@Nullable final Page page, @NonNull final SimplePicassoImageView view) {
        final Resource resource = page != null ? page.getResource(page.mBackgroundImage) : null;
        Resource.bind(resource, view);
        view.setVisibility(resource != null ? View.VISIBLE : View.GONE);
    }
}