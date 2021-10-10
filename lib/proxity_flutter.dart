import 'dart:async';

import 'package:flutter/services.dart';

class ProxityFlutter {
  static const MethodChannel _channel = MethodChannel('eu.proxity');

  static Future<Map?> get location {
    return _channel.invokeMapMethod("location");
  }

  static Future<bool?> initialize({
    required String apiKey,
    String? deviceId,
  }) async {
    return _channel.invokeMethod<bool>("initialize", {
      'apiKey': apiKey,
      'deviceId': deviceId,
    });
  }

  static void start() {
    _channel.invokeMethod("start");
  }

  static void runWebhooks(List<String> ids, String data) {
    _channel.invokeMethod("webhooks", {ids: ids, data: data});
  }
}
