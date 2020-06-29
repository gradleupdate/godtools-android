package org.cru.godtools.xml.model

import android.net.Uri
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import org.ccci.gto.android.common.util.xmlpull.skipTag
import org.cru.godtools.base.model.Event
import org.cru.godtools.xml.XMLNS_ANALYTICS
import org.cru.godtools.xml.XMLNS_CONTENT
import org.xmlpull.v1.XmlPullParser

private const val XML_COLOR = "color"
private const val XML_TYPE = "type"
private const val XML_TYPE_EVENT = "event"
private const val XML_TYPE_URL = "url"
private const val XML_URL = "url"

class Button : Content, Styles {
    companion object {
        internal const val XML_BUTTON = "button"
    }

    enum class Type {
        EVENT, URL, UNKNOWN;

        companion object {
            internal val DEFAULT = UNKNOWN

            internal fun parseOrNull(value: String?) = when (value) {
                XML_TYPE_EVENT -> EVENT
                XML_TYPE_URL -> URL
                else -> null
            }
        }
    }

    internal constructor(parent: BaseModel, parser: XmlPullParser) : super(parent, parser) {
        parser.require(XmlPullParser.START_TAG, XMLNS_CONTENT, XML_BUTTON)

        type = Type.parseOrNull(parser.getAttributeValue(null, XML_TYPE)) ?: Type.DEFAULT
        events = parseEvents(parser, XML_EVENTS)
        url = parser.getAttributeValueAsUriOrNull(XML_URL)
        _buttonColor = parser.getAttributeValueAsColorOrNull(XML_COLOR)

        // process any child elements
        var analyticsEvents: Collection<AnalyticsEvent> = emptySet()
        var text: Text? = null
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            val ns = parser.namespace
            val name = parser.name
            when {
                ns == XMLNS_ANALYTICS && name == AnalyticsEvent.XML_EVENTS ->
                    analyticsEvents = AnalyticsEvent.fromEventsXml(parser)
                ns == XMLNS_CONTENT && name == Text.XML_TEXT -> text = Text(this, parser)
                else -> parser.skipTag()
            }
        }
        this.analyticsEvents = analyticsEvents
        this.text = text
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    internal constructor(parent: BaseModel, text: ((Button) -> Text?)? = null) : super(parent) {
        type = Type.DEFAULT
        events = emptySet()
        url = null
        _buttonColor = null

        analyticsEvents = emptySet()
        this.text = text?.invoke(this)
    }

    val type: Type
    val events: Set<Event.Id>
    val url: Uri?

    @ColorInt
    private val _buttonColor: Int?
    @get:ColorInt
    override val buttonColor: Int get() = _buttonColor ?: stylesParent.buttonColor

    val text: Text?
    override val textAlign get() = Text.Align.CENTER
    override val textColor get() = stylesParent.primaryTextColor

    val analyticsEvents: Collection<AnalyticsEvent>
}

val Button?.buttonColor get() = this?.buttonColor ?: stylesParent.buttonColor
val Button?.textColor get() = this?.textColor ?: stylesParent.primaryTextColor
