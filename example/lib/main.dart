// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// ignore_for_file: public_member_api_docs

import 'dart:async';
import 'dart:io';

import 'package:android_intent/android_intent.dart';
import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:webview_flutter_example/web_page.dart';

void main() => runApp(MaterialApp(home: WebViewExample()));

class WebViewExample extends StatefulWidget {
  @override
  _WebViewExampleState createState() => _WebViewExampleState();
}

class _WebViewExampleState extends State<WebViewExample> {
  final Completer<WebViewController> _controller = Completer<WebViewController>();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Flutter WebView example')),
      // We're using a Builder here so we have a context that is below the Scaffold
      // to allow calling Scaffold.of(context) so we can show a snackbar.
      body: Builder(builder: (BuildContext context) {
        return Column(
          children: [
            RaisedButton(
              child: Text("First web page"),
              onPressed: () {
                Navigator.push(context, MaterialPageRoute(builder: (context) {
                  return WebPage();
                }));
              },
            ),
            RaisedButton(
              child: Text("Second web page"),
              onPressed: () {
                Navigator.push(context, MaterialPageRoute(builder: (context) {
                  return WebPage();
                }));
              },
            ),
            RaisedButton(
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
          ],
        );
      }),
    );
  }
}
