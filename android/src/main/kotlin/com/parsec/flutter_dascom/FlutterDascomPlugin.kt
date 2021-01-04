package com.parsec.flutter_dascom

import android.hardware.usb.UsbDevice
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import androidx.annotation.NonNull
import com.dascom.print.PrintCommands.ZPL
import com.dascom.print.Transmission.Pipe
import com.dascom.print.Transmission.UsbPipe
import com.dascom.print.Utils.Unit.DPI_203
import com.dascom.print.Utils.UsbUtils
import com.parsec.flutter_dascom.handlers.FlutterDascomHandler
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.util.*
import java.util.logging.Logger
import kotlin.collections.HashMap

/** FlutterDascomPlugin */
public class FlutterDascomPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    private val uiThreadHandler = Handler(Looper.getMainLooper())

    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var usbUtils: UsbUtils
    private lateinit var usbDevice: UsbDevice
    private var smartPrint: ZPL? = null
    private var pipe: Pipe? = null
    /// 自定义参数（后期扩展使用）

    private lateinit var labelWidth: Number

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "flutter_dascom")
        channel.setMethodCallHandler(this);
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    companion object {
        const val tag = "FlutterDascomPlugin"

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            FlutterDascomHandler.setContext(registrar.activity())
            val channel = MethodChannel(registrar.messenger(), "flutter_dascom")
            channel.setMethodCallHandler(FlutterDascomPlugin())
        }
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == "getPlatformVersion") {
            result.success("Android ${android.os.Build.VERSION.RELEASE}")
        } else if (call.method == "initPrintDevice") { // 初始化
//            labelWidth = call.argument<Int>("labelWidth")!!
            initPrintDevice(result)
        } else if (call.method == "checkScanDevice") {
            checkScanDevice(result)
        } else if (call.method == "connectDevice") {
            connectDevice(result)
        } else if (call.method == "testPrintText") {
            text(result)
        } else if (call.method == "testPrintBarCode") {
            code128("parsec-code", result)
        } else if (call.method == "testPrintCodeQR") {
            codeQR("parsec_qr_code", result)
        } else if (call.method == "printSmallTicketTemplate") {
            printSmallTicketTemplate(call, result)
        } else {
            result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }


    private fun checkScanDevice(@NonNull result: Result) {
        usbUtils = UsbUtils.getInstance(FlutterDascomHandler.getContext())
        usbUtils.getConnectUsbDevice(fun(device, grant) {
            if (grant) {
                usbDevice = device;
                result.success("扫描到可用 USB 设备")
            } else {
                if (device != null) {
                    result.error("没有USB授权", "没有USB授权", null)
                } else {
                    result.error("没有扫描到USB设备", "没有扫描到USB设备", null)
                }
            }
        })
    }

    private fun Pipe(pipe: Pipe?) {
        smartPrint = ZPL(pipe)
    }

    private fun connectDevice(@NonNull result: Result) {
        pipe?.close()
        if (usbDevice != null) {
            try {
                pipe = UsbPipe(FlutterDascomHandler.getContext(), usbDevice)
                Pipe(pipe)
                result.success("usb设备连接成功")
            } catch (e: Exception) {
                e.printStackTrace()
                result.success("usb设备连接失败")
            }
        } else {
            result.success("请先扫描usb设备并授予权限")
        }
    }

    /// 后期使用
    private fun initPrintDevice(@NonNull result: Result) {
        val params: HashMap<String, String> = HashMap()
        params["result_code"] = "ERROR"
        params["code"] = "500"
        usbUtils = UsbUtils.getInstance(FlutterDascomHandler.getContext())
        usbUtils.getConnectUsbDevice(fun(device, grant) {
            if (grant) {
                usbDevice = device;
                pipe?.close()
                if (usbDevice != null) {
                    try {
                        pipe = UsbPipe(FlutterDascomHandler.getContext(), usbDevice)
                        Pipe(pipe)
                        params["result_code"] = "SUCCESS"
                        params["code"] = "200"
                        params["message"] = "打印机初始化连接成功"
                        result.success(params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        params["message"] = "打印机初始化连接失败"
                        result.success(params)
                    }
                } else {
                    params["message"] = "请先扫描usb设备并授予权限"
                    result.success(params)
                }
            } else {
                if (device != null) {
                    params["message"] = "没有USB授权"
                    result.success(params)
                } else {
                    params["message"] = "没有扫描到USB设备"
                    result.success(params)
                }
            }
        })
    }

    private fun text(@NonNull result: Result) {
        if (pipe == null || !pipe!!.isConnected) {
            result.error("请先连接打印机", "请先连接打印机", null)
            return
        }
        Thread {
            smartPrint!!.setLabelStart()
            smartPrint!!.setLabelWidth(labelWidth.toInt() * DPI_203.MM)
            smartPrint!!.setLabelLength(DPI_203.CM)
            smartPrint!!.printText(0, 0, 2, 2, "打印字体：")
            smartPrint!!.setLabelEnd()
            smartPrint!!.setLabelStart()
            smartPrint!!.setLabelWidth(75 * DPI_203.MM)
            smartPrint!!.setLabelLength(DPI_203.CM)
            smartPrint!!.printText(0, 0, 1, 1, "FNT字体：")
            smartPrint!!.setLabelEnd()
            smartPrint!!.setLabelStart() //标签开始
            smartPrint!!.setLabelWidth(75 * DPI_203.MM) //标签宽度
            smartPrint!!.setLabelLength((6.5 * DPI_203.CM).toInt()) //标签长度
            //打印文本
            smartPrint!!.printText(5 * DPI_203.MM, 0, 1, 1, "得实集团1988年成立于香港，是一家以香港为总部的")
            //文本高宽放大两倍
            smartPrint!!.printText(0, 4 * DPI_203.MM, 2, 2, "高科技企业集团。经过三十")
            //文本高宽放大三倍
            smartPrint!!.printText(0, 11 * DPI_203.MM, 3, 3, "年的努力，得实集")
            //文本高宽放大四倍
            smartPrint!!.printText(0, 21 * DPI_203.MM, 4, 4, "团已发展成为")
            smartPrint!!.printText(0, 34 * DPI_203.MM, 3, 3, "涵盖计算机硬件、")
            smartPrint!!.printText(0, 44 * DPI_203.MM, 2, 2, "个人健康服务、LED照明等")
            smartPrint!!.printText(0, 51 * DPI_203.MM, 1, 1, "业务领域的全球性公司，倾力打造“百年老店”是得实")
            smartPrint!!.printText(0, 55 * DPI_203.MM, 1, 1, "集团一贯秉持的目标。")
            smartPrint!!.setLabelEnd() //标签结束，然后开始打印
            smartPrint!!.setLabelStart()
            smartPrint!!.setLabelWidth(75 * DPI_203.MM)
            smartPrint!!.setLabelLength(5 * DPI_203.MM)
            smartPrint!!.printTextTTF(0, 0, 36, 36, "TTF字体：")
            smartPrint!!.setLabelEnd()
            smartPrint!!.setLabelStart() //标签开始
            smartPrint!!.setLabelWidth(75 * DPI_203.MM) //标签宽度
            smartPrint!!.setLabelLength((6.5 * DPI_203.CM).toInt()) //标签长度
            smartPrint!!.printTextTTF(0, 0, 24, 24, "得实集团1988年成立于香港，是一家以香港为总部的")
            smartPrint!!.printTextTTF(0, 4 * DPI_203.MM, 48, 48, "高科技企业集团。经过三十")
            smartPrint!!.printTextTTF(0, 11 * DPI_203.MM, 72, 72, "年的努力，得实集")
            smartPrint!!.printTextTTF(0, 21 * DPI_203.MM, 96, 96, "团已发展成为")
            smartPrint!!.printTextTTF(0, 34 * DPI_203.MM, 72, 72, "涵盖计算机硬件、")
            smartPrint!!.printTextTTF(0, 44 * DPI_203.MM, 48, 48, "个人健康服务、LED照明等")
            smartPrint!!.printTextTTF(0, 51 * DPI_203.MM, 24, 24, "业务领域的全球性公司，倾力打造“百年老店”是得实")
            smartPrint!!.printTextTTF(0, 55 * DPI_203.MM, 10, 10, "集团一贯秉持的目标。")
            val b = smartPrint!!.setLabelEnd() //标签结束，然后开始打印
            uiThreadHandler.post(Runnable {
                if (b) {
                    result.success("发送成功")
                } else {
                    result.error("发送失败", "发送失败", null)
                }
            })
        }.start()
    }

    private fun code128(@NonNull code: String, @NonNull result: Result) {
        if (pipe == null || !pipe!!.isConnected) {
            result.success("请先连接打印机")
            return
        }
        Thread {
            smartPrint!!.setLabelStart()
            smartPrint!!.setLabelWidth(37 * DPI_203.MM)
            smartPrint!!.setLabelLength(DPI_203.CM)
            smartPrint!!.printText(0, 0, 2, 2, "打印一维码：")
            smartPrint!!.setLabelEnd()

            smartPrint!!.setLabelStart()
            smartPrint!!.setLabelWidth(37 * DPI_203.MM)
            smartPrint!!.setLabelLength((2.5 * DPI_203.CM).toInt())
            smartPrint!!.printText(0, 0, 1, 1, "高度10毫米:")
            smartPrint!!.printCode128(0, 5 * DPI_203.MM, 10 * DPI_203.MM, 1, 1, false, false, code)
            smartPrint!!.setLabelEnd()

            smartPrint!!.setLabelStart()
            smartPrint!!.setLabelWidth(37 * DPI_203.MM)
            smartPrint!!.setLabelLength((2.5 * DPI_203.CM).toInt())
            smartPrint!!.printText(0, 0, 1, 1, "显示下注释:")
            smartPrint!!.printCode128(0, 5 * DPI_203.MM, 10 * DPI_203.MM, 1, 1, true, false, code)
            smartPrint!!.setLabelEnd()

            smartPrint!!.setLabelStart()
            smartPrint!!.setLabelWidth(37 * DPI_203.MM)
            smartPrint!!.setLabelLength((2.5 * DPI_203.CM).toInt())
            smartPrint!!.printText(0, 0, 1, 1, "显示上注释:")
            smartPrint!!.printCode128(0, 10 * DPI_203.MM, 10 * DPI_203.MM, 2, 2, true, true, code)
            smartPrint!!.setLabelEnd()

            smartPrint!!.setLabelStart()
            smartPrint!!.setLabelWidth(37 * DPI_203.MM)
            smartPrint!!.setLabelLength((2.5 * DPI_203.CM).toInt())
            smartPrint!!.printText(0, 0, 1, 1, "居中显示上注释:")
            smartPrint!!.printCode128(0, 8 * DPI_203.MM, 10 * DPI_203.MM, 0, 0, true, true, code)
            smartPrint!!.setLabelEnd()

            smartPrint!!.setLabelStart()
            smartPrint!!.setLabelWidth(37 * DPI_203.MM)
            smartPrint!!.setLabelLength((2.5 * DPI_203.CM).toInt())
            smartPrint!!.printText(0, 0, 1, 1, "居中显示下注释:")
            smartPrint!!.printCode128(0, 5 * DPI_203.MM, 10 * DPI_203.MM, 0, 0, true, false, code)
            val b = smartPrint!!.setLabelEnd()
            uiThreadHandler.post(Runnable {
                if (b) {
                    result.success("发送成功")
                } else {
                    result.error("发送失败", "发送失败", null)
                }
            })
        }.start()
    }

    private fun codeQR(@NonNull text: String, @NonNull result: Result) {
        if (pipe == null || !pipe!!.isConnected) {
            result.success("请先连接打印机")
            return
        }
        Thread {
            smartPrint!!.setLabelStart()
            smartPrint!!.setLabelWidth(37 * DPI_203.MM)
            smartPrint!!.setLabelLength(DPI_203.CM)
            smartPrint!!.printText(0, 0, 2, 2, "打印二维码：")
            smartPrint!!.setLabelEnd()
            smartPrint!!.setLabelStart()
            smartPrint!!.setLabelWidth(37 * DPI_203.MM)
            smartPrint!!.setLabelLength((2 * DPI_203.CM))
            //打印二维码,大小3，纠错级别 L
            smartPrint!!.printText(0, 0, 1, 1, "大小：3，纠错级别:L")
            smartPrint!!.printQRCode(0, 5 * DPI_203.MM, 3, 'L', text)
            smartPrint!!.setLabelEnd()
            smartPrint!!.setLabelStart()
            smartPrint!!.setLabelWidth(37 * DPI_203.MM)
            smartPrint!!.setLabelLength((3 * DPI_203.CM))
            //打印二维码,大小3，纠错级别 L
            smartPrint!!.printText(0, 0, 1, 1, "大小：5，纠错级别:Q")
            smartPrint!!.printQRCode(0, 5 * DPI_203.MM, 5, 'Q', text)
            smartPrint!!.setLabelEnd()
            smartPrint!!.setLabelStart()
            smartPrint!!.setLabelWidth(37 * DPI_203.MM)
            smartPrint!!.setLabelLength((4 * DPI_203.CM))
            //打印二维码,大小3，纠错级别 L
            smartPrint!!.printText(0, 0, 1, 1, "大小：7，纠错级别:M")
            smartPrint!!.printQRCode(0, 5 * DPI_203.MM, 7, 'M', text)
            smartPrint!!.setLabelEnd()
            smartPrint!!.setLabelStart()
            smartPrint!!.setLabelWidth(37 * DPI_203.MM)
            smartPrint!!.setLabelLength((5.5 * DPI_203.CM).toInt())
            //打印二维码,大小3，纠错级别 L
            smartPrint!!.printText(0, 0, 1, 1, "大小：10，纠错级别:H")
            smartPrint!!.printQRCode(0, 5 * DPI_203.MM, 10, 'H', text)
            val b = smartPrint!!.setLabelEnd()
            uiThreadHandler.post(Runnable {
                if (b) {
                    result.success("发送成功")
                } else {
                    result.error("发送失败", "发送失败", null)
                }
            })
        }.start()
    }


    /// 凯桥信息 互连网医院就诊小票
    /// 1dot=0.125mm
    private fun printSmallTicketTemplate(@NonNull call: MethodCall, @NonNull result: Result) {
        if (pipe == null || !pipe!!.isConnected) {
            result.success(false)
            return
        }
        val labelWidth: Number = 58 * DPI_203.MM // 纸张宽度  单位 毫米
        val title: Map<String, Any> = call.argument<Map<String, Any>>("title")!!
        val subtitle: String = call.argument<String>("subtitle")!!
        val datetime: String = call.argument<String>("datetime")!!
        val prompt: List<String> = call.argument<List<String>>("prompt")!!
        val barCode: String = call.argument<String>("barCode")!!
        val qrCode: String = call.argument<String>("qrCode")!!
        val contents: List<Map<String, String>> = call.argument("contents")!!
//        val barCode: Map<String, String> = call.argument("barCode")!!
//        val qrCode: Map<String, String> = call.argument("qrCode")!!
        Thread { // 注意打印顺序
            // 打印空行 便于切纸机切纸
            smartPrint!!.setLabelStart()
            smartPrint!!.setFieldBlock(0, 1, 0, 1) // 打印内容自定义 打印内容居中
            smartPrint!!.setLabelWidth(labelWidth.toInt())
            smartPrint!!.setLabelLength(2 * DPI_203.CM) //
            smartPrint!!.printText(0, 0, 2, 2, "      ")
            smartPrint!!.setLabelEnd()
            if (prompt.isNotEmpty()) {
                prompt.reversed().forEach{item ->
                    smartPrint!!.setLabelStart()
                    smartPrint!!.setLabelWidth(labelWidth.toInt())
                    smartPrint!!.setLabelLength(50)
                    smartPrint!!.setFieldBlock(0, 1, 1, 1) // 打印内容自定义 打印内容居中
                    smartPrint!!.printText(0, 0, 1, 1, item)
                    smartPrint!!.setLabelEnd()}
            }

            if(!TextUtils.isEmpty(barCode)){
                smartPrint!!.setLabelStart()
                smartPrint!!.setFieldBlock(0, 1, 1, 1) // 打印内容自定义 打印内容居中
                smartPrint!!.setLabelWidth(labelWidth.toInt())
                smartPrint!!.setLabelLength((2.5 * DPI_203.CM).toInt())
                smartPrint!!.printCode128(3 * DPI_203.MM, 5 * DPI_203.MM, 10 * DPI_203.MM, 0, 0, true, false, barCode)
                smartPrint!!.setLabelEnd()
            }

            if(!TextUtils.isEmpty(qrCode)){
                smartPrint!!.setLabelStart()
                smartPrint!!.setFieldBlock(0, 1, 1, 1) // 打印内容自定义 打印内容居中
                smartPrint!!.setLabelWidth(labelWidth.toInt())
                smartPrint!!.setLabelLength(5 * DPI_203.CM) // 当前内容所占用的行高
                //打印二维码,大小7，纠错级别 H
                smartPrint!!.printQRCode(6 * DPI_203.MM, 5 * DPI_203.MM, 8, 'H', qrCode) //  二维码
                smartPrint!!.setLabelEnd()
            }

            contents.reversed().forEach { map ->
                smartPrint!!.setLabelStart()
                smartPrint!!.setLabelWidth(labelWidth.toInt())
                smartPrint!!.setLabelLength(5 * DPI_203.MM)
                smartPrint!!.printText(5 * DPI_203.MM, 0, 1, 1, "${map["label"]}：${map["value"]}")
                smartPrint!!.setLabelEnd()
            }

            if (datetime != "") {
                smartPrint!!.setLabelStart()
                smartPrint!!.setLabelWidth(labelWidth.toInt())
                smartPrint!!.setLabelLength(DPI_203.CM)
                smartPrint!!.setFieldBlock(0, 1, 0, 1) // 打印内容自定义 打印内容居中
                smartPrint!!.printText(0, 0, 1, 1, datetime)
                smartPrint!!.setLabelEnd()
            }
            if (subtitle != "") {
                smartPrint!!.setLabelStart()
                smartPrint!!.setFieldBlock(0, 1, 1, 1) // 打印内容自定义 打印内容居中
                smartPrint!!.setLabelWidth(labelWidth.toInt())
                smartPrint!!.setLabelLength(40)
                smartPrint!!.printText(0, 0, 1, 1, subtitle)
                smartPrint!!.setLabelEnd()
            }
            val text: String? = title["text"] as String?
            val line: Int = title["line"] as Int
            val textPair: Int = title["textPair"] as Int
            val fontSize: Int = title["fontSize"] as Int
            smartPrint!!.setLabelStart()
            smartPrint!!.setLabelWidth(labelWidth.toInt())
            smartPrint!!.setFieldBlock(0, line, textPair, 1) // 打印内容自定义 打印内容居中
            smartPrint!!.setLabelLength(line * 60)
            smartPrint!!.printText(0, 0, fontSize, fontSize, text)
            smartPrint!!.setLabelEnd()

            // 打印空行 便于切纸机切纸
            smartPrint!!.setLabelStart()
            smartPrint!!.setFieldBlock(0, 1, 0, 1) // 打印内容自定义 打印内容居中
            smartPrint!!.setLabelWidth(labelWidth.toInt())
            smartPrint!!.setLabelLength(2 * DPI_203.CM) //
            smartPrint!!.printText(0, 0, 2, 2, "      ")
            val b = smartPrint!!.setLabelEnd()
            uiThreadHandler.post(Runnable {
                result.success(b)
            })
        }.start()
    }

    override fun onDetachedFromActivity() {
        TODO("Not yet implemented")
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        TODO("Not yet implemented")
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        FlutterDascomHandler.setContext(binding.activity)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        TODO("Not yet implemented")
    }
}
