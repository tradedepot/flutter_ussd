import 'dart:async';

import 'package:flutter/services.dart';

class FlutterUssd {
  static const MethodChannel _channel = const MethodChannel('flutter_ussd');

  ///
  ///Dials the {ussd} code. Works only on android 8.0 and above.
  ///This methods requires the permission android.permission.CALL_PHONE to be
  ///grandted
  ///
  static Future<String> dial(String ussd) async {
    final String response = await _channel.invokeMethod('dial', {'ussd': ussd});
    return response;
  }

  ///
  /// Checks if this method is supported on this device
  ///
  static Future<bool> isSupported() async {
    final bool response = await _channel.invokeMethod('isSupported');
    return response;
  }
}
