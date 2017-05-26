package org.cru.godtools.tract.model;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.annimon.stream.Stream;

import org.ccci.gto.android.common.util.XmlPullParserUtils;
import org.cru.godtools.tract.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

import static org.cru.godtools.tract.Constants.XMLNS_TRACT;

public class Paragraph extends Content {
    static final String XML_PARAGRAPH = "paragraph";

    @NonNull
    private final List<Content> mContent = new ArrayList<>();

    private Paragraph(@NonNull final Base parent) {
        super(parent);
    }

    @NonNull
    static Paragraph fromXml(@NonNull final Base parent, @NonNull final XmlPullParser parser)
            throws IOException, XmlPullParserException {
        return new Paragraph(parent).parse(parser);
    }

    @NonNull
    private Paragraph parse(@NonNull final XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, XMLNS_TRACT, XML_PARAGRAPH);

        // process any child elements
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            // try parsing this child element as a content node
            final Content content = Content.fromXml(this, parser);
            if (content != null) {
                mContent.add(content);
                continue;
            }

            // skip unrecognized nodes
            XmlPullParserUtils.skipTag(parser);
        }

        return this;
    }

    @NonNull
    @Override
    public View render(@NonNull final LinearLayout parent) {
        final View view =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.tract_content_paragraph, parent, false);

        // attach all the content to this layout
        final LinearLayout content = ButterKnife.findById(view, R.id.content);
        if (content != null) {
            Stream.of(mContent).map(c -> c.render(content)).forEach(content::addView);
        }

        return view;
    }
}
