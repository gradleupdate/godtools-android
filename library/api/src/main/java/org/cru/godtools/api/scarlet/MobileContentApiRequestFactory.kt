package org.cru.godtools.api.scarlet

import com.tinder.scarlet.websocket.okhttp.request.RequestFactory
import okhttp3.Request

class MobileContentApiRequestFactory(private val url: String) : RequestFactory {
    override fun createRequest(): Request = Request.Builder()
        // Sec-WebSocket-Key is automatically added to the request by OkHttp3
        // .header("Sec-WebSocket-Key", "nawjkerjkhasdf")
        .header("Sec-WebSocket-Protocol", "actioncable-v1-json, actioncable-unsupported")
        .header("Sec-WebSocket-Version", "13")
        .header("Origin", "https://mobile-content-api-stage.cru.org")
        .url(url)
        .build()
}
