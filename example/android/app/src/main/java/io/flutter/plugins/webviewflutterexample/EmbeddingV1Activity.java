// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.webviewflutterexample;

import android.content.Intent;

import androidx.annotation.NonNull;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.platform.PlatformViewRegistry;
import io.flutter.plugins.webviewflutter.hybrid.HybridWebViewFactory;


public class EmbeddingV1Activity extends FlutterActivity {
    HybridWebViewFactory factory;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        factory = new HybridWebViewFactory(flutterEngine.getDartExecutor().getBinaryMessenger(), this);

        PlatformViewRegistry registry = flutterEngine.getPlatformViewsController()
                .getRegistry();
        registry.registerViewFactory("hybrid-webView", factory);
        super.configureFlutterEngine(flutterEngine);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 180) {
            factory.onActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
