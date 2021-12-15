package com.shuge888.flutter_cos

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import com.tencent.cos.xml.CosXmlService
import com.tencent.cos.xml.transfer.TransferConfig
import androidx.annotation.NonNull
import com.tencent.cos.xml.CosXmlServiceConfig
import com.tencent.cos.xml.exception.CosXmlClientException
import com.tencent.cos.xml.exception.CosXmlServiceException
import com.tencent.cos.xml.listener.CosXmlProgressListener
import com.tencent.cos.xml.listener.CosXmlResultListener
import com.tencent.cos.xml.model.CosXmlRequest
import com.tencent.cos.xml.model.CosXmlResult
import com.tencent.cos.xml.transfer.COSXMLUploadTask
import com.tencent.cos.xml.transfer.TransferManager

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding


/** FlutterCosPlugin  */
class FlutterCosPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private var channel: MethodChannel? = null
  private var mContext: Context? = null
  private var mActivity: Activity? = null

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.getFlutterEngine().dartExecutor, "flutter_cos")
    mContext = flutterPluginBinding.applicationContext
    channel?.setMethodCallHandler(this)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + Build.VERSION.RELEASE)
    } else if (call.method.equals("getNative")) {
      result.success("getNative")
    } else if (call.method.equals("uploadFile")) {
      Log.d("onMethodCall", "uploadFile")
      val secretId: String = call.argument("secretId")!! //secretId
      val secretKey: String = call.argument("secretKey")!! //secretKey
      val sessionToken: String = call.argument("sessionToken")!! //secretKey
      val expiredTime: String = call.argument("expiredTime")!! //secretKey

      // 此秘钥计算方法与项目中用到的不符合，所以不使用该方法生成秘钥
      // QCloudCredentialProvider myCredentialProvider =
      //        new ShortTimeCredentialProvider(secretId, secretKey, 300);
      val myCredentialProvider = LocalSessionCredentialProvider(
        secretId,
        secretKey,
        sessionToken,
        expiredTime.toLong(),
      )
      val region: String = call.argument("region")!! // region
      val bucket: String = call.argument("bucket")!! // bucket
      val localPath: String = call.argument("localPath")!! // localPath
      val cosPath: String= call.argument("cosPath")!! // cosPath

      /// 初始化 COS Service
      // 创建 CosXmlServiceConfig 对象，根据需要修改默认的配置参数
      val serviceConfig: CosXmlServiceConfig = CosXmlServiceConfig.Builder()
        .setRegion(region)
        .isHttps(true) // 使用 HTTPS 请求, 默认为 HTTP 请求
        .builder()
      val cosXmlService = CosXmlService(mContext, serviceConfig, myCredentialProvider)

      // 初始化 TransferConfig，这里使用默认配置，如果需要定制，请参考 SDK 接口文档
      val transferConfig: TransferConfig = TransferConfig.Builder().build()
      //初始化 TransferManager
      val transferManager = TransferManager(cosXmlService, transferConfig)
      //上传文件
      val cosxmlUploadTask: COSXMLUploadTask = transferManager.upload(bucket, cosPath, localPath, null)
      val data = HashMap<String, Any>()
      data["localPath"] = localPath
      data["cosPath"] = cosPath
      Log.d("onMethodCall", "startUpload")
      cosxmlUploadTask.setCosXmlProgressListener(object : CosXmlProgressListener {
        override fun onProgress(complete: Long, target: Long) {
          Log.d("onProgress", "$complete : $target")
          mActivity?.runOnUiThread {
            val progress = HashMap<String, Any>()
            progress["cosPath"] = cosPath
            progress["localPath"] = localPath
            progress["progress"] = complete * 100.0 / target
            channel?.invokeMethod("onProgress", progress)
          }
        }
      })

      //设置返回结果回调
      cosxmlUploadTask.setCosXmlResultListener(object : CosXmlResultListener {
        override fun onSuccess(request: CosXmlRequest?, httpResult: CosXmlResult) {
          Log.d("onSuccess", httpResult.printResult())
          mActivity?.runOnUiThread{
            result.success(data)
            channel?.invokeMethod("onSuccess", cosPath)
          }
        }

        override fun onFail(request: CosXmlRequest?, exception: CosXmlClientException?, serviceException: CosXmlServiceException) {
          Log.d("onFail", exception.toString() + serviceException.toString())
          data["message"] = exception.toString() + serviceException.toString()
          mActivity?.runOnUiThread {
            result.error("400", "error", exception.toString())
            channel?.invokeMethod("onFailed", data)
          }
          if (exception != null) {
            exception.printStackTrace()
          } else {
            serviceException.printStackTrace()
          }
        }
      })
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel?.setMethodCallHandler(null)
    channel = null
  }

  ///activity 生命周期
  override fun onAttachedToActivity(@NonNull binding: ActivityPluginBinding) {
    mActivity = binding.activity
  }

  override fun onDetachedFromActivity() {
    mActivity = null
  }

  override fun onDetachedFromActivityForConfigChanges() {}

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}


}