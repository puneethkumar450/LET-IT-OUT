package com.letitout.app.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import org.json.JSONObject

/** The WebView owns the single WebRTC stream. Only a short-lived token crosses into JavaScript. */
internal class LiveAvatarController {
    private var view: WebView? = null

    fun attach(webView: WebView) { view = webView }

    fun sendAudio(pcm16: ByteArray) {
        if (pcm16.isEmpty()) return
        val active = view ?: return
        for (offset in pcm16.indices step 8192) {
            val encoded = Base64.encodeToString(pcm16.copyOfRange(offset, minOf(offset + 8192, pcm16.size)), Base64.NO_WRAP)
            active.post {
                if (view === active) active.evaluateJavascript("window.avatarBridge?.sendAudio(${JSONObject.quote(encoded)})", null)
            }
        }
    }

    fun clear() {
        val active = view ?: return
        active.post { if (view === active) active.evaluateJavascript("window.avatarBridge?.clear()", null) }
    }

    fun finishAudio() {
        val active = view ?: return
        active.post { if (view === active) active.evaluateJavascript("window.avatarBridge?.finishAudio()", null) }
    }

    fun close() {
        val old = view ?: return
        view = null
        old.evaluateJavascript("window.avatarBridge?.stop()", null)
        old.postDelayed({ old.destroy() }, 500)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun LiveAvatarView(
    sessionToken: String,
    controller: LiveAvatarController,
    modifier: Modifier = Modifier,
    onReady: () -> Unit,
    onFailure: () -> Unit,
    onSpeaking: () -> Unit,
    onSilent: () -> Unit
) {
    val ready = rememberUpdatedState(onReady)
    val failure = rememberUpdatedState(onFailure)
    val speaking = rememberUpdatedState(onSpeaking)
    val silent = rememberUpdatedState(onSilent)
    val main = Handler(Looper.getMainLooper())
    DisposableEffect(controller, sessionToken) { onDispose { controller.close() } }

    Box(modifier) {
        AndroidView(
            factory = { context ->
                val loader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                    .build()
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    addJavascriptInterface(object {
                        @JavascriptInterface fun onReady() { main.post { ready.value() } }
                        @JavascriptInterface fun onFailure() { main.post { failure.value() } }
                        @JavascriptInterface fun onSpeaking() { main.post { speaking.value() } }
                        @JavascriptInterface fun onSilent() { main.post { silent.value() } }
                    }, "AndroidAvatar")
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                            loader.shouldInterceptRequest(request.url)

                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                            request.url.host != "appassets.androidplatform.net"

                        override fun onPageFinished(view: WebView, url: String) {
                            if (url == "https://appassets.androidplatform.net/assets/live-avatar/index.html") {
                                view.evaluateJavascript("window.avatarBridge?.start(${JSONObject.quote(sessionToken)})", null)
                            }
                        }
                    }
                    controller.attach(this)
                    loadUrl("https://appassets.androidplatform.net/assets/live-avatar/index.html")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
