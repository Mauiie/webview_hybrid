package io.flutter.plugins.webviewflutter.hybrid

import android.R
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebStorage
import android.webkit.*
import io.flutter.Log
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView


/**
 * Create by TaiJL
 * on 2020/8/24
 * desc:
 */
class HybridWebView(private val id: Int, private val params: Map<String, Any>, private val mActivity: Activity, private val messenger: BinaryMessenger) : PlatformView, MethodChannel.MethodCallHandler {

    private val webView = WebView(mActivity)
    private lateinit var methodChannel: MethodChannel
    private lateinit var flutterWebViewClient: HybridWebViewClient
    private val webChromeClient: HybridChromeClient = HybridChromeClient()
    private var isFullScreen = false
    var customViewHideTime: Long = 0
    private var uploadFile: ValueCallback<Uri>? = null
    private var uploadFiles: ValueCallback<Array<Uri>>? = null
    private lateinit var platformThreadHandler: Handler

    init {
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        methodChannel = MethodChannel(messenger, "plugins.flutter.io/webview_$id")
        methodChannel.setMethodCallHandler(this)
        var injectTitleFontFamily = false
        var injectFontFamily = false
        if (null != params["settings"] && params["settings"] is Map<*, *>) {
            if ((params["settings"] as Map<*, *>).containsKey("injectTitleFontFamily")) injectTitleFontFamily = (params["settings"] as Map<*, *>)["injectTitleFontFamily"] as Boolean
            if ((params["settings"] as Map<*, *>).containsKey("injectFontFamily")) injectFontFamily = (params["settings"] as Map<*, *>)["injectFontFamily"] as Boolean
        }
        flutterWebViewClient = HybridWebViewClient(methodChannel, injectTitleFontFamily, injectFontFamily)
        applySettings(params["settings"] as Map<String, Any>)
        platformThreadHandler = Handler(mActivity.mainLooper)
        customSetting()
        if (params.containsKey("initialUrl")) {
            val url = params["initialUrl"] as String?
            webView.loadUrl(url)
        }
    }

    private fun customSetting() {
        webView.isHorizontalScrollBarEnabled = false
        webView.isVerticalScrollBarEnabled = false
        webView.settings.allowFileAccess = true
        webView.settings.setAppCacheEnabled(true)
        //        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.settings.allowContentAccess = true
        webView.settings.setAllowFileAccessFromFileURLs(true)
        webView.settings.setAllowUniversalAccessFromFileURLs(true)
        webView.settings.setGeolocationEnabled(true)
        webView.settings.textZoom = 100
        webView.settings.databaseEnabled = true
        webView.settings.setSupportZoom(false)
        webView.settings.useWideViewPort = true
        CookieSyncManager.createInstance(webView.context)
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        webView.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.settings.mixedContentMode = 0
        }
        webView.webChromeClient = webChromeClient

    }

    private fun applySettings(settings: Map<String, Any>) {
        for (key in settings.keys) {
            when (key) {
                "jsMode" -> updateJsMode(settings[key] as Int?)
                "hasNavigationDelegate" -> {
                    val hasNavigationDelegate = settings[key] as Boolean
                    val webViewClient: WebViewClient = flutterWebViewClient.createWebViewClient(hasNavigationDelegate)
                    webView.webViewClient = webViewClient
                }
                "debuggingEnabled" -> {
                    val debuggingEnabled = settings[key] as Boolean
                    WebView.setWebContentsDebuggingEnabled(debuggingEnabled)
                }
                "gestureNavigationEnabled", "injectFontFamily", "injectTitleFontFamily", "disableInputProxy" -> {
                }
                "avoidSelectCrash" -> {
                }
                "userAgent" -> updateUserAgent(settings[key] as String?)
                else -> throw IllegalArgumentException("Unknown WebView setting: $key")
            }
        }
    }

    private fun updateJsMode(mode: Int?) {
        when (mode) {
            0 -> webView.settings.javaScriptEnabled = false
            1 -> webView.settings.javaScriptEnabled = true
            else -> throw java.lang.IllegalArgumentException("Trying to set unknown JavaScript mode: $mode")
        }
    }

    private fun updateUserAgent(userAgent: String?) {
        webView.settings.userAgentString = userAgent
    }

    override fun getView(): View {
        return webView
    }


    override fun dispose() {
        webView.destroy()
    }

    override fun onMethodCall(methodCall: MethodCall, result: MethodChannel.Result) {
        when (methodCall.method) {
            "loadUrl" -> loadUrl(methodCall, result)
            "updateSettings" -> updateSettings(methodCall, result)
            "canGoBack" -> canGoBack(result)
            "canGoForward" -> canGoForward(result)
            "goBack" -> goBack(result)
            "goForward" -> goForward(result)
            "reload" -> reload(result)
            "currentUrl" -> currentUrl(result)
            "evaluateJavascript" -> evaluateJavaScript(methodCall, result)
            "addJavascriptChannels" -> addJavaScriptChannels(methodCall, result)
            "removeJavascriptChannels" -> removeJavaScriptChannels(methodCall, result)
            "clearCache" -> clearCache(result)
            "getTitle" -> getTitle(result)
//            todo 暂未用到，注释
//            "scrollTo" -> scrollTo(methodCall, result)
//            "scrollBy" -> scrollBy(methodCall, result)
//            "getScrollX" -> getScrollX(result)
//            "getScrollY" -> getScrollY(result)
//            "exitFullScreen" -> exitFullScreen(result)
            else -> result.notImplemented()
        }
    }

    private fun updateSettings(methodCall: MethodCall, result: MethodChannel.Result) {
        applySettings(methodCall.arguments as Map<String, Any>)
        result.success(null)
    }

    private fun canGoBack(result: MethodChannel.Result) {
        result.success(webView.canGoBack())
    }

    private fun canGoForward(result: MethodChannel.Result) {
        result.success(webView.canGoForward())
    }

    private fun goBack(result: MethodChannel.Result) {
        if (webView.canGoBack()) {
            webView.goBack()
        }
        result.success(null)
    }

    private fun goForward(result: MethodChannel.Result) {
        if (webView.canGoForward()) {
            webView.goForward()
        }
        result.success(null)
    }

    private fun reload(result: MethodChannel.Result) {
        webView.reload()
        result.success(null)
    }

    private fun currentUrl(result: MethodChannel.Result) {
        result.success(webView.url)
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun evaluateJavaScript(methodCall: MethodCall, result: MethodChannel.Result) {
        val jsString = methodCall.arguments as String ?: throw UnsupportedOperationException("JavaScript string cannot be null")
        webView.run {
            evaluateJavascript(jsString) { value -> result.success(value) }
        }
    }

    private fun addJavaScriptChannels(methodCall: MethodCall, result: MethodChannel.Result) {
        val channelNames = methodCall.arguments as List<String>
        registerJavaScriptChannelNames(channelNames)
        result.success(null)
    }

    private fun removeJavaScriptChannels(methodCall: MethodCall, result: MethodChannel.Result) {
        val channelNames = methodCall.arguments as List<String>
        for (channelName in channelNames) {
            webView.removeJavascriptInterface(channelName)
        }
        result.success(null)
    }

    private fun clearCache(result: MethodChannel.Result) {
        webView.clearCache(true)
        WebStorage.getInstance().deleteAllData()
        result.success(null)
    }

    private fun getTitle(result: MethodChannel.Result) {
        result.success(webView.title)
    }

    private fun registerJavaScriptChannelNames(channelNames: List<String>) {
        for (channelName in channelNames) {
            webView.addJavascriptInterface(HybridJavaScriptChannel(methodChannel, channelName, platformThreadHandler), channelName)
        }
    }

    /**
     * 加载地址
     */
    private fun loadUrl(methodCall: MethodCall, result: MethodChannel.Result) {
        val request = methodCall.arguments as Map<String, Any>
        val url = request["url"] as String?
        var headers = request["headers"] as Map<String?, String?>?
        if (headers == null) {
            headers = emptyMap<String?, String>()
        }
        webView.loadUrl(url, headers)
        result.success(null)
    }

    private fun openFileChooseProcess() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        mActivity.startActivityForResult(Intent.createChooser(i, "test"), 180)
    }

    inner class HybridChromeClient : WebChromeClient() {
        private var myVideoView: View? = null
        private var callback: CustomViewCallback? = null
        override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
            callback.invoke(origin, true, false)
            super.onGeolocationPermissionsShowPrompt(origin, callback)
        }

        /**
         * 全屏播放配置
         */
        override fun onShowCustomView(view: View, customViewCallback: CustomViewCallback) {
            webView.visibility = View.GONE
            val rootView: ViewGroup = mActivity.findViewById(R.id.content)
            rootView.addView(view)
            myVideoView = view
            callback = customViewCallback
            isFullScreen = true
        }

        override fun onHideCustomView() {
            customViewHideTime = System.currentTimeMillis()
            if (callback != null) {
                callback!!.onCustomViewHidden()
                callback = null
            }
            if (myVideoView != null) {
                val rootView: ViewGroup = mActivity.findViewById(R.id.content)
                rootView.removeView(myVideoView)
                myVideoView = null
                //                myWebViewParentView.addView(myNormalView);
                webView.visibility = View.VISIBLE
            }
            isFullScreen = false
        }

        // For Android 3.0+
        fun openFileChooser(uploadMsg: ValueCallback<Uri>?, acceptType: String?) {
            Log.i("test", "openFileChooser")
            this@HybridWebView.uploadFile = uploadMsg
            openFileChooseProcess()
        }

        // For Android < 3.0
        fun openFileChooser(uploadMsgs: ValueCallback<Uri>?) {
            Log.i("test", "openFileChooser 2")
            this@HybridWebView.uploadFile = uploadMsgs
            openFileChooseProcess()
        }
        
        // For Android  > 4.1.1
        fun openFileChooser(uploadMsg: ValueCallback<Uri>?, acceptType: String?, capture: String?) {
            Log.i("test", "openFileChooser 3")
            this@HybridWebView.uploadFile = uploadMsg
            openFileChooseProcess()
        }

        // For Android  >= 5.0
        override fun onShowFileChooser(webView: WebView,
                                       filePathCallback: ValueCallback<Array<Uri>>,
                                       fileChooserParams: FileChooserParams): Boolean {
            Log.i("test", "openFileChooser 4:$filePathCallback")
            this@HybridWebView.uploadFiles = filePathCallback
            openFileChooseProcess()
            return true
        }

        override fun getDefaultVideoPoster(): Bitmap {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }


    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                180 -> {
                    if (null != uploadFile) {
                        val result = if (data == null || resultCode != Activity.RESULT_OK) null else data.data
                        uploadFile?.onReceiveValue(result)
                        uploadFile = null
                    }
                    if (null != uploadFiles) {
                        val result = if (data == null || resultCode != Activity.RESULT_OK) null else data.data
                        if (null != result) {
                            uploadFiles?.onReceiveValue(arrayOf<Uri>(result))
                            uploadFiles = null
                        }
                    }
                }
                else -> {
                }
            }
        } else {
            if (null != uploadFile) {
                uploadFile?.onReceiveValue(null)
                uploadFile = null
            }
            if (null != uploadFiles) {
                uploadFiles?.onReceiveValue(null)
                uploadFiles = null
            }
        }
    }
}

