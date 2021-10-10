import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:proxity_flutter/proxity_flutter.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    final ok = await ProxityFlutter.initialize(
      apiKey: "<YOUR-API-KEY-HERE>",
      deviceId: "",
    );
    if (ok == null || !ok) {
      print('Failed to initialize Proxity client');

    }

    ProxityFlutter.start();
    print(await ProxityFlutter.location);

    final messsages = const EventChannel("eu.proxity.messages")
      .receiveBroadcastStream();
    messsages.listen((ids) => print(ids));
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: const Center(
          child: Text('abahaba'),
        ),
      ),
    );
  }
}
