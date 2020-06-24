package org.cru.godtools.xml.model.tips

import org.ccci.gto.android.common.util.xmlpull.skipTag
import org.cru.godtools.xml.XMLNS_TRAINING
import org.cru.godtools.xml.model.Base
import org.cru.godtools.xml.model.BaseModel
import org.xmlpull.v1.XmlPullParser

private const val XML_TIP = "tip"
private const val XML_PAGES = "pages"

@OptIn(ExperimentalStdlibApi::class)
class Tip : BaseModel {
    val pages: List<TipPage>

    constructor(base: Base, parser: XmlPullParser) : super(base) {
        parser.require(XmlPullParser.START_TAG, XMLNS_TRAINING, XML_TIP)

        pages = buildList {
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) continue

                when (parser.namespace) {
                    XMLNS_TRAINING -> when (parser.name) {
                        XML_PAGES -> addAll(parser.parsePages())
                        else -> parser.skipTag()
                    }
                    else -> parser.skipTag()
                }
            }
        }
    }

    private fun XmlPullParser.parsePages() = buildList {
        require(XmlPullParser.START_TAG, XMLNS_TRAINING, XML_PAGES)

        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) continue

            when (namespace) {
                XMLNS_TRAINING -> when (name) {
                    TipPage.XML_PAGE -> add(TipPage(this@Tip, this@parsePages))
                    else -> skipTag()
                }
                else -> skipTag()
            }
        }
    }
}
