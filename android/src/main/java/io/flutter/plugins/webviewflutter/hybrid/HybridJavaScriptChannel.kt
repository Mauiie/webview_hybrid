// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package io.flutter.plugins.webviewflutter.hybrid

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import io.flutter.plugin.common.MethodChannel
import java.util.*

/**
 * Added as a JavaScript interface to the WebView for any JavaScript channel that the Dart code sets
 * up.
 *
 *
 * Exposes a single method named `postMessage` to JavaScript, which sends a message over a method
 * channel to the Dart code.
 */
internal class HybridJavaScriptChannel
/**
 * @param methodChannel the Flutter WebView method channel to which JS messages are sent
 * @param javaScriptChannelName the name of the JavaScript channel, this is sent over the method
 * channel with each message to let the Dart code know which JavaScript channel the message
 * was sent through
 */(
        private val methodChannel: MethodChannel, private val javaScriptChannelName: String, private val platformThreadHandler: Handler) {

    // Suppressing unused warning as this is invoked from JavaScript.
    @JavascriptInterface
    fun postMessage(message: String) {
        val postMessageRunnable = Runnable {
            val arguments = HashMap<String, String>()
            arguments["channel"] = javaScriptChannelName
            arguments["message"] = message
            methodChannel.invokeMethod("javascriptChannelMessage", arguments)
        }
        if (platformThreadHandler.looper == Looper.myLooper()) {
            postMessageRunnable.run()
        } else {
            platformThreadHandler.post(postMessageRunnable)
        }
    }

}