import 'dart:collection';

import 'package:flutter/material.dart';

const String _title = "title";
const String _subtitle = "subtitle";
const String _datetime = "datetime";
const String _contents = "contents";
const String _barCode = "barCode";
const String _qrCode = "qrCode";
const String _prompt = "prompt";

mixin BaseModel {
  Map toMap();
}

class PrintModel implements BaseModel {
  final String title;
  final String subtitle;
  final String datetime;
  final List<Map<String, String>> contents;
  final String barCode;
  final String qrCode;
  final String prompt;

  PrintModel(
      {@required this.title,
      @required this.subtitle,
      @required this.datetime,
      @required this.contents,
      this.barCode,
      this.qrCode,
      this.prompt});

  @override
  Map toMap() {
    return {
      _title: title,
      _subtitle: subtitle,
      _datetime: datetime,
      _barCode: barCode,
      _contents: contents,
      _qrCode: qrCode,
      _prompt: prompt
    };
  }
}

class TextModel implements BaseModel {
  /// 内容
  final String content;

  /// 字体宽 1-10
  final int fontSizeHeight;

  /// 字体高 1-10
  final int fontSizeWidth;

  /// 水平位置 /dot
  /// 接受的值： 0 至 32000
  /// 默认值： 0
  final int horizontal;

  /// 垂直位置 /dot
  /// 接受的值： 0 至 32000
  /// 默认值： 0
  final int vertical;

  TextModel(this.content,
      {this.fontSizeHeight,
      this.fontSizeWidth,
      this.horizontal,
      this.vertical});

  @override
  Map toMap() {
    return {
      "content": content,
      "fontSizeHeight": fontSizeHeight,
      "fontSizeWidth": fontSizeWidth,
      "horizontal": horizontal,
      "vertical": vertical,
    };
  }
}

enum Correct { H, Q, M, L }

class QrCodeModel implements BaseModel {
  /// 水平位置 /dot
  final int horizontal;

  /// 垂直位置 /dot
  final int vertical;

  /// 方向
  /// 接受的值：
  /// 0 = 正常
  /// 1 = 顺时针旋转 90 度
  /// 2 = 反转 180 度
  /// 3 = 逆时针旋转 270 度，逆时针读取
  /// 初始值：0
  final int direction;

  /// 大小 1-10
  final int size;

  /// 纠错级别 'H' 'Q' 'M' 'L'
  final Correct correct;

  /// 二维码内容
  final String content;

  QrCodeModel(this.content,
      {this.horizontal,
      this.vertical,
      this.direction,
      this.size,
      this.correct});

  @override
  Map toMap() {
    return {
      "content": content,
      "horizontal": horizontal,
      "vertical": vertical,
      "direction": direction,
      "size": size,
      "correct": correct,
    };
  }
}

class BarCodeModel implements BaseModel {
  /// 模块宽度 /dot
  /// 接受的值：1 至 10
  /// 开机时的初始值：2
  final int narrowBarWidth;

  /// 宽条与窄条的宽度比
  /// 接受的值：2.0 至 3.0，增量为 0.1
  /// 此参数对固定比率的条码没有影响。
  /// 默认值：3.0
  final double wideToNarrowRatio;

  /// 条码高度 /dot
  /// 开机时的初始值： 10
  final int barHeight;

  /// 字段原点位置为 X 轴方向 计算距离 /dot
  final int horizontal;

  /// vertical - Y 轴方向从左上角向下 计算距离 /dot
  final int vertical;

  /// 条码高度/dot
  final int height;

  /// 注释高/dot 0-10
  final int heightHumanRead;

  /// 注释宽/dot 0-10
  final int widthHumanRead;

  /// true 显示注释
  /// heightHumanRead > 0 && widthHumanRead > 0 注释居左，宽高生效
  /// heightHumanRead = 0 && widthHumanRead = 0 注释居中，宽高不生效
  final bool flagHumanRead;

  /// false 注释在条码下方 true 注释在条码上方
  final bool posHumanRead;

  /// 条码码内容
  final String content;

  BarCodeModel(
    this.content, {
    this.narrowBarWidth = 2,
    this.wideToNarrowRatio = 3.0,
    this.barHeight,
    this.horizontal,
    this.vertical,
    this.height,
    this.heightHumanRead,
    this.widthHumanRead,
    this.flagHumanRead,
    this.posHumanRead,
  });

  @override
  Map toMap() {
    return {
      "content": content,
      "narrowBarWidth": narrowBarWidth,
      "wideToNarrowRatio": wideToNarrowRatio,
      "barHeight": barHeight,
      "horizontal": horizontal,
      "vertical": vertical,
      "height": height,
      "heightHumanRead": heightHumanRead,
      "widthHumanRead": widthHumanRead,
      "flagHumanRead": flagHumanRead,
      "posHumanRead": posHumanRead,
    };
  }
}
