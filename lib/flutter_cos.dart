
import 'dart:async';

import 'package:flutter/services.dart';

class FlutterCos {
  static const MethodChannel _channel = MethodChannel('flutter_cos');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<String> get getNative async {
    return await _channel.invokeMethod('getNative');
  }

  static Future<dynamic> uploadByFile(
      String region,
      String appid,
      String bucket,
      String secretId,
      String secretKey,
      String sessionToken,
      int expiredTime,
      String cosPath,
      String localPath) {
    return _channel.invokeMethod<dynamic>('uploadFile', {
      'region': region,
      'appid': appid,
      'bucket': bucket,
      'secretId': secretId,
      'secretKey': secretKey,
      'expiredTime': expiredTime,
      'sessionToken': sessionToken,
      'cosPath': cosPath,
      'localPath': localPath,
    });
  }

  static void setMethodCallHandler(Future<dynamic> Function(MethodCall call) handler) {
    _channel.setMethodCallHandler(handler);
  }
}
