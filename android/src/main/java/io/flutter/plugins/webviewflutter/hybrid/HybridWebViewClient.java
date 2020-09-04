// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.webviewflutter.hybrid;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;



import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodChannel;

// We need to use WebViewClientCompat to get
// shouldOverrideUrlLoading(WebView view, WebResourceRequest request)
// invoked by the webview on older Android devices, without it pages that use iframes will
// be broken when a navigationDelegate is set on Android version earlier than N.
class HybridWebViewClient {
    private static final String TAG = "FlutterWebViewClient";
    private final MethodChannel methodChannel;
    private boolean hasNavigationDelegate;
    boolean injectTitleFontFamily = false;
    boolean injectFontFamily = false;

    HybridWebViewClient(MethodChannel methodChannel, boolean injectTitleFontFamily, boolean injectFontFamily) {
        this.methodChannel = methodChannel;
        this.injectFontFamily = injectFontFamily;
        this.injectTitleFontFamily = injectTitleFontFamily;
    }

    private static String errorCodeToString(int errorCode) {
        switch (errorCode) {
            case WebViewClient.ERROR_AUTHENTICATION:
                return "authentication";
            case WebViewClient.ERROR_BAD_URL:
                return "badUrl";
            case WebViewClient.ERROR_CONNECT:
                return "connect";
            case WebViewClient.ERROR_FAILED_SSL_HANDSHAKE:
                return "failedSslHandshake";
            case WebViewClient.ERROR_FILE:
                return "file";
            case WebViewClient.ERROR_FILE_NOT_FOUND:
                return "fileNotFound";
            case WebViewClient.ERROR_HOST_LOOKUP:
                return "hostLookup";
            case WebViewClient.ERROR_IO:
                return "io";
            case WebViewClient.ERROR_PROXY_AUTHENTICATION:
                return "proxyAuthentication";
            case WebViewClient.ERROR_REDIRECT_LOOP:
                return "redirectLoop";
            case WebViewClient.ERROR_TIMEOUT:
                return "timeout";
            case WebViewClient.ERROR_TOO_MANY_REQUESTS:
                return "tooManyRequests";
            case WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME:
                return "unsupportedAuthScheme";
            case WebViewClient.ERROR_UNSUPPORTED_SCHEME:
                return "unsupportedScheme";
            default:
                return "unknown";
        }
    }

    String[] attachedFileSuffix = {".pdf", ".doc", ".docx", ".zip", ".rar", ".xls", ".xlsx", ".ppt", ".pptx",
            ".PDF", ".DOC", ".DOCX", ".ZIP", ".RAR", ".XLS", ".XLSX", ".PPT", ".PPTX",};


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        if (url.contains(".")) {
            String suffix = url.substring(url.lastIndexOf("."));
            if (Arrays.asList(attachedFileSuffix).contains(suffix)) {
                try {
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    view.getContext().startActivity(intent);
                    return true;
                } catch (Exception e) {
                }
            }
        }
        if (!hasNavigationDelegate) {
            return false;
        }

        if (url.contains("fapp/buzz/vioqrymgr/queryWap.do")) {
            return false;
        }
        notifyOnNavigationRequest(url, request.getRequestHeaders(), view, request.isForMainFrame());
        // We must make a synchronous decision here whether to allow the navigation or not,
        // if the Dart code has set a navigation delegate we want that delegate to decide whether
        // to navigate or not, and as we cannot get a response from the Dart delegate synchronously we
        // return true here to block the navigation, if the Dart delegate decides to allow the
        // navigation the plugin will later make an addition loadUrl call for this url.
        //
        // Since we cannot call loadUrl for a subframe, we currently only allow the delegate to stop
        // navigations that target the main frame, if the request is not for the main frame
        // we just return false to allow the navigation.
        //
        // For more details see: https://github.com/flutter/flutter/issues/25329#issuecomment-464863209
        if (request.getUrl().toString().startsWith("tel:")) {
            return true;
        }
        return request.isForMainFrame();
    }

    private boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (!hasNavigationDelegate) {
            return false;
        }
        if (url.contains("fapp/buzz/vioqrymgr/queryWap.do")) {
            return false;
        }
        // This version of shouldOverrideUrlLoading is only invoked by the webview on devices with
        // webview versions  earlier than 67(it is also invoked when hasNavigationDelegate is false).
        // On these devices we cannot tell whether the navigation is targeted to the main frame or not.
        // We proceed assuming that the navigation is targeted to the main frame. If the page had any
        // frames they will be loaded in the main frame instead.
        Log.w(
                TAG,
                "Using a navigationDelegate with an old webview implementation, pages with frames or iframes will not work");
        notifyOnNavigationRequest(url, null, view, true);
        return true;
    }

    private void onPageStarted(WebView view, String url) {
        Map<String, Object> args = new HashMap<>();
        args.put("url", url);
        methodChannel.invokeMethod("onPageStarted", args);
    }

    private void onPageFinished(WebView view, String url) {
        Map<String, Object> args = new HashMap<>();
        args.put("url", url);
        methodChannel.invokeMethod("onPageFinished", args);
    }

    private void onWebResourceError(final int errorCode, final String description) {
        final Map<String, Object> args = new HashMap<>();
        args.put("errorCode", errorCode);
        args.put("description", description);
        args.put("errorType", HybridWebViewClient.errorCodeToString(errorCode));
        methodChannel.invokeMethod("onWebResourceError", args);
    }

    private void notifyOnNavigationRequest(
            String url, Map<String, String> headers, WebView webview, boolean isMainFrame) {
        HashMap<String, Object> args = new HashMap<>();
        args.put("url", url);
        args.put("isForMainFrame", isMainFrame);
        if (isMainFrame) {
            methodChannel.invokeMethod(
                    "navigationRequest", args, new OnNavigationRequestResult(url, headers, webview));
        } else {
            methodChannel.invokeMethod("navigationRequest", args);
        }
    }

    // This method attempts to avoid using WebViewClientCompat due to bug
    // https://bugs.chromium.org/p/chromium/issues/detail?id=925887. Also, see
    // https://github.com/flutter/flutter/issues/29446.
    WebViewClient createWebViewClient(boolean hasNavigationDelegate) {
        this.hasNavigationDelegate = hasNavigationDelegate;
        return internalCreateWebViewClient();
    }

    private WebViewClient internalCreateWebViewClient() {
        return new WebViewClient() {
            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request.getUrl().toString().contains("fapp/buzz/vioqrymgr/queryWap.do")) {
                    return false;
                }
                return HybridWebViewClient.this.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                hideErrorTips(view);
                HybridWebViewClient.this.onPageStarted(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                hideErrorPage(view);
                //设置模版字体
                if (injectFontFamily)
                    view.loadUrl("javascript:!function(){" +
                            "s=document.createElement('style');s.innerHTML=" +
                            "\"@font-face{font-family:myhyqh;src:url('****/fonts/FZBiaoYS_GBK_YS.ttf')format('truetype');}*{font-family:myhyqh;}\";" +
                            "document.getElementsByTagName('head')[0].appendChild(s);" +
                            "document.getElementsByTagName('body')[0].style.fontFamily = \"myhyqh\";}()");
                if (injectTitleFontFamily)
                    view.loadUrl("javascript:!function(){" +
                            "var n=document.createElement('style');n.innerHTML=" +
                            "\"@font-face{font-family:otherFont;src:url('****/fonts/FZZHUNYSK.ttf')format('truetype');}.NewsDetail #title{font-family:otherFont;!important}.NewsDetail.xyAccountName{font-family:otherFont;!important}\";" +
                            "document.getElementsByTagName('head')[0].appendChild(n);}()");
                HybridWebViewClient.this.onPageFinished(view, url);
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(
                    WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    showErrorPage(view);
                }
                if (request.getUrl().toString().equals(view.getUrl())) {
                    HybridWebViewClient.this.onWebResourceError(
                            error.getErrorCode(), error.getDescription().toString());
                }
            }

            @Override
            public void onReceivedError(
                    WebView view, int errorCode, String description, String failingUrl) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    showErrorPage(view);
                }
                if (failingUrl.equals(view.getUrl())) {
                    HybridWebViewClient.this.onWebResourceError(errorCode, description);
                }
            }

            @Override
            public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
                // Deliberately empty. Occasionally the webview will mark events as having failed to be
                // handled even though they were handled. We don't want to propagate those as they're not
                // truly lost.
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                WebResourceResponse response = super.shouldInterceptRequest(view, url);
                if (url.contains("FZBiaoYS_GBK_YS.ttf") && injectFontFamily) {
                    String assertPath = "flutter_assets/assets/fonts/FZBiaoYS_GBK_YS.ttf";
                    try {
                        response = new WebResourceResponse("application/x-font-ttf", "UTF8",
                                view.getContext().getAssets().open(assertPath));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (url.contains("FZZHUNYSK.ttf") && injectTitleFontFamily) {
                    String assertPath = "flutter_assets/assets/fonts/FZZHUNYSK.ttf";
                    try {
                        response = new WebResourceResponse("application/x-font-ttf", "UTF8",
                                view.getContext().getAssets().open(assertPath));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return response;
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                WebResourceResponse response = super.shouldInterceptRequest(view, request);
                if (null != request && null != request.getUrl()) {
                    String url = request.getUrl().toString();
                    if (url.contains("FZBiaoYS_GBK_YS.ttf")) {
                        String assertPath = "flutter_assets/assets/fonts/FZBiaoYS_GBK_YS.ttf";
                        try {
                            response = new WebResourceResponse("application/x-font-ttf", "UTF8",
                                    view.getContext().getAssets().open(assertPath));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (url.contains("FZZHUNYSK.ttf")) {
                        String assertPath = "flutter_assets/assets/fonts/FZZHUNYSK.ttf";
                        try {
                            response = new WebResourceResponse("application/x-font-ttf", "UTF8",
                                    view.getContext().getAssets().open(assertPath));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return response;
            }
        };
    }

    boolean pageError = false;
    LinearLayout errorPage;

    private void showErrorPage(final WebView view) {
        errorPage = new LinearLayout(view.getContext());
        errorPage.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        errorPage.setBackgroundColor(Color.parseColor("#d8d8d8"));
        errorPage.setGravity(Gravity.CENTER);
        TextView tips = new TextView(view.getContext());
        tips.setText("页面丢失了，点击重试");
        tips.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.reload();
            }
        });
        errorPage.addView(tips);
        view.addView(errorPage);
        pageError = true;
    }

    private void hideErrorTips(final WebView view) {
        pageError = false;
        if (errorPage != null) {
            errorPage.removeAllViews();
        }
    }

    private void hideErrorPage(final WebView view) {
        if (pageError) {
            return;
        }
        if (errorPage != null) {
            view.removeAllViews();
            errorPage = null;
        }
    }


    private static class OnNavigationRequestResult implements MethodChannel.Result {
        private final String url;
        private final Map<String, String> headers;
        private final WebView webView;

        private OnNavigationRequestResult(String url, Map<String, String> headers, WebView webView) {
            this.url = url;
            this.headers = headers;
            this.webView = webView;
        }

        @Override
        public void success(Object shouldLoad) {
            Boolean typedShouldLoad = (Boolean) shouldLoad;
            if (typedShouldLoad) {
                loadUrl();
            }
        }

        @Override
        public void error(String errorCode, String s1, Object o) {
            throw new IllegalStateException("navigationRequest calls must succeed");
        }

        @Override
        public void notImplemented() {
            throw new IllegalStateException(
                    "navigationRequest must be implemented by the webview method channel");
        }

        private void loadUrl() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                webView.loadUrl(url, headers);
            } else {
                webView.loadUrl(url);
            }
        }
    }
}
