package org.cru.godtools.xml.model

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.RestrictTo
import org.ccci.gto.android.common.util.XmlPullParserUtils
import org.cru.godtools.base.model.Event
import org.cru.godtools.xml.R
import org.cru.godtools.xml.XMLNS_TRACT
import org.cru.godtools.xml.model.Text.Companion.fromNestedXml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

private const val XML_TITLE = "title"

class Modal : Base, Parent, Styles {
    val id get() = "${page.id}-$position"
    private val position: Int

    val title: Text?
    override val content: List<Content>

    val listeners: Set<Event.Id>
    val dismissListeners: Set<Event.Id>

    @get:ColorInt
    override val primaryColor get() = Color.TRANSPARENT
    @get:ColorInt
    override val primaryTextColor get() = Color.WHITE
    @get:ColorInt
    override val textColor get() = Color.WHITE
    @get:ColorInt
    override val buttonColor get() = Color.WHITE

    @get:DimenRes
    override val textSize get() = R.dimen.text_size_modal
    override val textAlign get() = Text.Align.CENTER

    @RestrictTo(RestrictTo.Scope.TESTS)
    internal constructor(parent: Base, position: Int) : super(parent) {
        this.position = position
        title = null
        content = emptyList()
        listeners = emptySet()
        dismissListeners = emptySet()
    }

    @OptIn(ExperimentalStdlibApi::class)
    internal constructor(parent: Base, position: Int, parser: XmlPullParser) : super(parent) {
        this.position = position

        parser.require(XmlPullParser.START_TAG, XMLNS_TRACT, XML_MODAL)

        listeners = parseEvents(parser, XML_LISTENERS)
        dismissListeners = parseEvents(parser, XML_DISMISS_LISTENERS)

        // process any child elements
        var title: Text? = null
        content = buildList<Content> {
            parsingChildren@ while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) continue

                when (parser.namespace) {
                    XMLNS_TRACT -> when (parser.name) {
                        XML_TITLE -> {
                            title = fromNestedXml(this@Modal, parser, XMLNS_TRACT, XML_TITLE)
                            continue@parsingChildren
                        }
                    }
                }

                // try parsing this child element as a content node
                val content = Content.fromXml(this@Modal, parser)
                if (content != null) {
                    if (!content.isIgnored) add(content)
                    continue
                }

                // skip unrecognized nodes
                XmlPullParserUtils.skipTag(parser)
            }
        }
        this.title = title
    }

    companion object {
        const val XML_MODAL = "modal"

        @JvmStatic
        @Deprecated(
            "Use constructor directly",
            ReplaceWith("Modal(parent, position, parser)", "org.cru.godtools.xml.model.Modal")
        )
        @Throws(IOException::class, XmlPullParserException::class)
        fun fromXml(parent: Base, parser: XmlPullParser, position: Int) = Modal(parent, position, parser)
    }
}
