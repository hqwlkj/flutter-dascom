
import 'dart:async';
import 'package:flutter_dascom/model/print_model.dart';
import 'package:flutter/services.dart';

export 'model/print_model.dart';
class FlutterDascom {

  static const MethodChannel _channel = const MethodChannel('flutter_dascom');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<String> get checkScanDevice async {
    final String message = await _channel.invokeMethod('checkScanDevice');
    return message;
  }

  static Future<String> get connectDevice async {
    final String message = await _channel.invokeMethod('connectDevice');
    return message;
  }

  /// 初始化 检查和链接打印设备
  static Future<Map<String, dynamic>> get initPrintDevice async {
    final Map<String, dynamic> message = await _channel.invokeMapMethod('initPrintDevice');
    return message;
  }
  static Future<String> get testPrint async {
    final String message = await _channel.invokeMethod('testPrintText');
    return message;
  }

  static Future<String> get testPrintBarCode async {
    final String message = await _channel.invokeMethod('testPrintBarCode');
    return message;
  }

  static Future<String> get testPrintCodeQR async {
    final String message = await _channel.invokeMethod('testPrintCodeQR');
    return message;
  }

  static Future<bool> printSmallTicketTemplate(PrintModel model) async {
    final bool message = await _channel.invokeMethod('printSmallTicketTemplate', model.toMap());
    return message;
  }
}
