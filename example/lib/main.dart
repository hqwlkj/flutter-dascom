import 'dart:collection';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_dascom/flutter_dascom.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  String _checkStateMsg = '--';
  String _connectDeviceMsg = '--';
  String _printMsg = '--';

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await FlutterDascom.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: [
              Text('Running on: $_platformVersion\n'),
              FlatButton(
                  color: Theme.of(context).primaryColor,
                  onPressed: () async {
                    String msg = await FlutterDascom.checkScanDevice;
                    if (!mounted) return;
                    setState(() {
                      _checkStateMsg = msg;
                    });
                  },
                  child: Text('检查是否有可用设备')),
              Text('check state msg: $_checkStateMsg\n'),
              FlatButton(
                  color: Theme.of(context).primaryColor,
                  onPressed: () async {
                    String msg = await FlutterDascom.connectDevice;
                    if (!mounted) return;
                    setState(() {
                      _connectDeviceMsg = msg;
                    });
                  },
                  child: Text('连接可用设备')),
              Text('connect device msg: $_connectDeviceMsg\n'),
              FlatButton(
                  color: Theme.of(context).primaryColor,
                  onPressed: () async {
                    String msg = await FlutterDascom.testPrint;
                    if (!mounted) return;
                    setState(() {
                      _printMsg = msg;
                    });
                  },
                  child: Text('测试打印文本信息')),
              Text('print msg: $_printMsg\n'),
              FlatButton(
                  color: Theme.of(context).primaryColor,
                  onPressed: () async {
                    String msg = await FlutterDascom.testPrintBarCode;
                    if (!mounted) return;
                    setState(() {
                      _printMsg = msg;
                    });
                  },
                  child: Text('测试打印条码信息')),
              FlatButton(
                  color: Theme.of(context).primaryColor,
                  onPressed: () async {
                    String msg = await FlutterDascom.testPrintCodeQR;
                    if (!mounted) return;
                    setState(() {
                      _printMsg = msg;
                    });
                  },
                  child: Text('测试打印二维码信息')),
              FlatButton(
                  color: Theme.of(context).primaryColor,
                  onPressed: () async {
                    Map<String, dynamic> msg = await FlutterDascom.initPrintDevice;
                    if (!mounted) return;
                    setState(() {
                      _checkStateMsg = '${msg['result_code']} - ${msg['code']} - ${msg['message']}';
                    });
                  },
                  child: Text('一键初始化')),
              FlatButton(
                  color: Theme.of(context).primaryColor,
                  onPressed: () async {
                    PrintModel _model = new PrintModel(
                        title: '虚拟医院',
                        subtitle: '门诊缴费凭证',
                        datetime:
                            '${DateTime.now().year}-${DateTime.now().month}-${DateTime.now().day} 15:00-15:30',
                        contents: [ // 这里需要注意打印的顺序
                          {"label": "支付流水号", "value": "42000043423156642"},
                          {"label": "支付金额", "value": "￥100.00"},
                          {"label": "支付方式", "value": "银联支付"},
                          {"label": "位    置", "value": "门诊1号楼4层"},
                          {"label": "执行科室", "value": "CT室"},
                          {"label": "项目名称", "value": "CT直接增强"},
                          {"label": "开单科室", "value": "内分泌科"},
                          {"label": "就诊卡号", "value": "0007730"},
                          {"label": "就 诊 人", "value": "张三"}
                        ],
                        barCode: '6907992100272',
                        qrCode: '6907992100272',
                        //     barCode: 'BarCodeModel('6907992100272').toMap()',
                        //     qrCode: QrCodeModel('6907992100272').toMap(),
                        prompt: '（请妥善保管或撕毁处理）');
                    bool msg =
                        await FlutterDascom.printSmallTicketTemplate(_model);
                    if (!mounted) return;
                    setState(() {
                      _printMsg = msg ? '打印成功' : '打印失败';
                    });
                  },
                  child: Text('测试打印挂号信息')),
            ],
          ),
        ),
      ),
    );
  }
}
