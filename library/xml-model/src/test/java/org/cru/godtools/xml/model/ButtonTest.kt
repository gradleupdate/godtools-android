package org.cru.godtools.xml.model

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.cru.godtools.base.model.Event
import org.cru.godtools.xml.util.getXmlParserForResource
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ButtonTest {
    private lateinit var manifest: Manifest

    @Before
    fun setup() {
        manifest = Manifest(TOOL_CODE)
    }

    @Test
    fun testParseButtonEvent() {
        val events = Event.Id.parse(TOOL_CODE, "ns:event1 event2")
        val button = Button(manifest, getXmlParserForResource("button_event.xml"))
        assertFalse(button.isIgnored)
        assertThat(button.events, containsInAnyOrder(*events.toTypedArray()))
        assertEquals("event button", button.text!!.text)
        assertEquals(Color.RED, button.buttonColor)
    }

    @Test
    fun testParseButtonUrl() {
        val button = Button(manifest, getXmlParserForResource("button_url.xml"))
        assertFalse(button.isIgnored)
        assertEquals(Button.Type.URL, button.type)
        assertEquals("https://www.google.com/", button.url!!.toString())
        assertEquals("url button", button.text!!.text)
        assertThat(button.analyticsEvents, contains(instanceOf(AnalyticsEvent::class.java)))
    }

    @Test
    fun testParseButtonRestrictTo() {
        val button = Button(manifest, getXmlParserForResource("button_restrictTo.xml"))
        assertTrue(button.isIgnored)
    }

    @Test
    fun testButtonTextColorFallbackBehavior() {
        val parent: Styles = mock {
            whenever(it.primaryColor) doReturn Color.RED
            whenever(it.primaryTextColor) doReturn Color.GREEN
        }

        assertEquals(Color.BLUE, Button(parent, text = { Text(it, textColor = Color.BLUE) }).text!!.textColor)
        assertEquals(Color.GREEN, Button(parent, text = { Text(it, textColor = null) }).text!!.textColor)
    }
}