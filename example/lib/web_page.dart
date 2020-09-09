import 'dart:io';

import 'package:android_intent/android_intent.dart';
import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

class WebPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    var url = 'https://github.com/flutter/flutter/issues/63897';


    return Scaffold(
      appBar: AppBar(
        title: Text("WebPage"),
        leading: GestureDetector(
          child: Icon(Icons.arrow_back),
          onTap: () {
            Navigator.pop(context);
          },
        ),
      ),
      body: Builder(
        builder: (BuildContext context) {
          return WebView(
              initialUrl: 'https://github.com/flutter/flutter/issues/63897',
              useHybridWebView: true,
              javascriptMode: JavascriptMode.unrestricted,
              gestureNavigationEnabled: true,
              onWebViewCreated: (WebViewController webViewController) {
//                _controller.complete(webViewController);
              },
              navigationDelegate: (NavigationRequest request) {
                if (request.url.startsWith('https://www.youtube.com/')) {
                  print('blocking navigation to $request}');
                  return NavigationDecision.prevent;
                }
                print('allowing navigation to $request');
                return NavigationDecision.navigate;
              });
        },
      ),
      floatingActionButton: FloatingActionButton(
        child: Text("open other activity"),
        onPressed: () async {
          if (Platform.isAndroid) {
            AndroidIntent intent = AndroidIntent(
              action: 'android.settings.SETTINGS',
            );
            await intent.launch();
          }
        },
      ),
    );
  }
}
