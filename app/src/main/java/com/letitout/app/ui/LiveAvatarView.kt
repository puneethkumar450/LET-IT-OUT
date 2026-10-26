package com.letitout.app.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.webkit.WebViewAssetLoader
import com.letitout.app.R
import org.json.JSONObject

/** The WebView owns the single WebRTC stream. Only a short-lived token crosses into JavaScript. */
internal class LiveAvatarController {
    private var view: WebView? = null
    private var loaded = false
    private var onLoaded: (() -> Unit)? = null

    /** Loads the bundled Simli client once while the welcome screen is idle. No session starts here. */
    fun preload(context: Context) {
        if (view != null) return
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
        view = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                    loader.shouldInterceptRequest(request.url)

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                    request.url.host != "appassets.androidplatform.net"

                override fun onPageFinished(view: WebView, url: String) {
                    if (url == "https://appassets.androidplatform.net/assets/live-avatar/index.html") {
                        loaded = true
                        onLoaded?.invoke()
                    }
                }
            }
            loadUrl("https://appassets.androidplatform.net/assets/live-avatar/index.html")
        }
    }

    fun acquire(context: Context, backgroundColor: Color): WebView {
        preload(context)
        return requireNotNull(view).apply {
            (parent as? ViewGroup)?.removeView(this)
            setBackgroundColor(backgroundColor.toArgb())
        }
    }

    fun whenLoaded(action: () -> Unit) {
        onLoaded = action
        if (loaded) action()
    }

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
        onLoaded = null
        view?.evaluateJavascript("window.avatarBridge?.stop()", null)
        view?.removeJavascriptInterface("AndroidAvatar")
    }

    fun destroy() {
        close()
        view?.let { active ->
            (active.parent as? ViewGroup)?.removeView(active)
            active.destroy()
        }
        view = null
        loaded = false
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun LiveAvatarView(
    sessionToken: String,
    controller: LiveAvatarController,
    backgroundColor: Color,
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
    val frame = remember(sessionToken) { mutableStateOf<ImageBitmap?>(null) }
    val readyReported = remember(sessionToken) { mutableStateOf(false) }
    val main = Handler(Looper.getMainLooper())
    DisposableEffect(controller, sessionToken) { onDispose { controller.close() } }

    Box(modifier.background(backgroundColor)) {
        if (frame.value == null) {
            Image(
                painter = painterResource(R.drawable.ari_onboarding_fullscreen),
                contentDescription = "Ari, your AI companion",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().zIndex(1f)
            )
        }
        AndroidView(
            factory = { context ->
                controller.acquire(context, backgroundColor).apply {
                    addJavascriptInterface(object {
                        @JavascriptInterface fun onReady() { main.post { ready.value() } }
                        @JavascriptInterface fun onFailure() { main.post { failure.value() } }
                        @JavascriptInterface fun onSpeaking() { main.post { speaking.value() } }
                        @JavascriptInterface fun onSilent() { main.post { silent.value() } }
                        @JavascriptInterface fun onFrameDiagnostics(details: String) {
                            if (com.letitout.app.BuildConfig.DEBUG) Log.d("LiveAvatar", details.take(120))
                        }
                        @JavascriptInterface fun onFrame(encoded: String) {
                            if (encoded.length > 150_000) return
                            val bytes = try { Base64.decode(encoded, Base64.NO_WRAP) } catch (_: IllegalArgumentException) { return }
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
                            main.post {
                                frame.value = bitmap.asImageBitmap()
                                if (!readyReported.value) {
                                    readyReported.value = true
                                    ready.value()
                                }
                            }
                        }
                    }, "AndroidAvatar")
                    val active = this
                    controller.whenLoaded {
                        active.evaluateJavascript("window.avatarBridge?.start(${JSONObject.quote(sessionToken)})", null)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        frame.value?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "Live talking avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().zIndex(1f)
            )
        }
    }
}
