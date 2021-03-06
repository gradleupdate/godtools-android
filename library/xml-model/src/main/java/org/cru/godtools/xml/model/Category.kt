package org.cru.godtools.xml.model

import androidx.annotation.WorkerThread
import org.ccci.gto.android.common.util.XmlPullParserUtils
import org.cru.godtools.xml.XMLNS_ARTICLE
import org.cru.godtools.xml.XMLNS_MANIFEST
import org.xmlpull.v1.XmlPullParser

private const val XML_ID = "id"
private const val XML_LABEL = "label"
private const val XML_BANNER = "banner"
private const val XML_AEM_TAG = "aem-tag"

@OptIn(ExperimentalStdlibApi::class)
class Category internal constructor(manifest: Manifest, parser: XmlPullParser) : Base(manifest) {
    val id: String?
    val label: Text?
    val aemTags: Set<String>
    private val _banner: String?
    val banner: Resource? get() = getResource(_banner)

    init {
        parser.require(XmlPullParser.START_TAG, XMLNS_MANIFEST, XML_CATEGORY)

        id = parser.getAttributeValue(null, XML_ID)
        _banner = parser.getAttributeValue(null, XML_BANNER)

        var label: Text? = null
        aemTags = buildSet<String> {
            parsingChildren@ while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) continue

                when (parser.namespace) {
                    XMLNS_MANIFEST -> when (parser.name) {
                        XML_LABEL -> {
                            label = Text.fromNestedXml(this@Category, parser, XMLNS_MANIFEST, XML_LABEL)
                            continue@parsingChildren
                        }
                    }
                    XMLNS_ARTICLE -> when (parser.name) {
                        XML_AEM_TAG -> add(parser.getAttributeValue(null, XML_ID))
                    }
                }

                // skip unrecognized nodes
                XmlPullParserUtils.skipTag(parser)
            }
        }
        this.label = label
    }

    companion object {
        // TODO: make this internal
        const val XML_CATEGORY = "category"

        @JvmStatic
        @WorkerThread
        @Deprecated(
            "Use constructor instead",
            ReplaceWith("Category(manifest, parser)", "org.cru.godtools.xml.model.Category")
        )
        fun fromXml(manifest: Manifest, parser: XmlPullParser) = Category(manifest, parser)
    }
}
