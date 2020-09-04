import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:webview_flutter/platform_interface.dart';
import 'package:webview_flutter/src/webview_method_channel.dart';

class AndroidWebViewHybrid extends WebViewPlatform {
  // This is used in the platform side to register the view.
  final String viewType = 'hybrid-webView';

  // Pass parameters to the platform side.
  final Map<String, dynamic> creationParams = <String, dynamic>{};
  MethodChannelWebViewPlatform _methodChannelWebViewPlatform;

  @override
  Widget build({
    BuildContext context,
    CreationParams creationParams,
    @required WebViewPlatformCallbacksHandler webViewPlatformCallbacksHandler,
    WebViewPlatformCreatedCallback onWebViewPlatformCreated,
    Set<Factory<OneSequenceGestureRecognizer>> gestureRecognizers,
  }) {
    return PlatformViewLink(
      viewType: viewType,
      surfaceFactory: (BuildContext context, PlatformViewController controller) {
        return AndroidViewSurface(
          controller: controller,
          gestureRecognizers: const <Factory<OneSequenceGestureRecognizer>>{},
          hitTestBehavior: PlatformViewHitTestBehavior.opaque,
        );
      },
      onCreatePlatformView: (PlatformViewCreationParams params) {
        return PlatformViewsService.initSurfaceAndroidView(
          id: params.id,
          viewType: viewType,
          layoutDirection: TextDirection.ltr,
          creationParams: MethodChannelWebViewPlatform.creationParamsToMap(creationParams),
          creationParamsCodec: StandardMessageCodec(),
        )
          ..addOnPlatformViewCreatedListener((id) {
            params.onPlatformViewCreated(id);
            _methodChannelWebViewPlatform = MethodChannelWebViewPlatform(id, webViewPlatformCallbacksHandler);
            if (onWebViewPlatformCreated == null) {
              return;
            }
            onWebViewPlatformCreated(_methodChannelWebViewPlatform);
          })
          ..create();
      },
    );
  }

  @override
  Future<bool> clearCookies() => MethodChannelWebViewPlatform.clearCookies();
}
