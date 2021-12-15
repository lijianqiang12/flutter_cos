package com.shuge888.flutter_cos

import android.os.Build
import com.tencent.cos.xml.CosXmlService

/** FlutterCosPlugin  */
class FlutterCosPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private var channel: MethodChannel? = null
  private var mContext: Context? = null
  private var mActivity: Activity? = null

  // Registrar registrar;
  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  //  public static void registerWith(Registrar registrar) {
  //    final MethodChannel channel = new MethodChannel(registrar.messenger(), "fw_cos");
  //    channel.setMethodCallHandler(new FlutterCosPlugin(registrar, channel));
  //  }
  // flutter 1.12之前的注册方法，没测试，估计用不了
  //  public FlutterCosPlugin(PluginRegistry.Registrar registrar, MethodChannel channel) {
  //    this.registrar = registrar;
  //    this.channel = channel;
  //  }
  fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "fw_cos")
    mContext = flutterPluginBinding.getApplicationContext()
    channel.setMethodCallHandler(this)
  }

  fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + Build.VERSION.RELEASE)
    } else if (call.method.equals("getNative")) {
      result.success("getNative")
    } else if (call.method.equals("uploadFile")) {
      Log.d("onMethodCall", "uploadFile")
      val secretId: String = call.argument("secretId") //secretId
      val secretKey: String = call.argument("secretKey") //secretKey

      // 此秘钥计算方法与项目中用到的不符合，所以不使用该方法生成秘钥
      // QCloudCredentialProvider myCredentialProvider =
      //        new ShortTimeCredentialProvider(secretId, secretKey, 300);
      val myCredentialProvider = LocalSessionCredentialProvider(
        call.< String > argument < String ? > "secretId",
        call.< String > argument < String ? > "secretKey",
        call.< String > argument < String ? > "sessionToken",
        call.argument("expiredTime").toString().toLong()
      )
      val region: String = call.argument("region") // region
      val bucket: String = call.argument("bucket") // bucket
      val localPath: String = call.argument("localPath") // localPath
      val cosPath: String = call.argument("cosPath") // cosPath

      /// 初始化 COS Service
      // 创建 CosXmlServiceConfig 对象，根据需要修改默认的配置参数
      val serviceConfig: CosXmlServiceConfig = Builder()
        .setRegion(region)
        .isHttps(true) // 使用 HTTPS 请求, 默认为 HTTP 请求
        .builder()
      val cosXmlService = CosXmlService(mContext, serviceConfig, myCredentialProvider)

      // 初始化 TransferConfig，这里使用默认配置，如果需要定制，请参考 SDK 接口文档
      val transferConfig: TransferConfig = Builder().build()
      //初始化 TransferManager
      val transferManager = TransferManager(cosXmlService, transferConfig)
      //上传文件
      val cosxmlUploadTask: COSXMLUploadTask = transferManager.upload(bucket, cosPath, localPath, null)
      val data = HashMap<String, Any>()
      data["localPath"] = localPath
      data["cosPath"] = cosPath
      Log.d("onMethodCall", "startUpload")
      cosxmlUploadTask.setCosXmlProgressListener(object : CosXmlProgressListener() {
        fun onProgress(complete: Long, target: Long) {
          Log.d("onProgress", "$complete : $target")
          mActivity.runOnUiThread(Runnable {
            val progress = HashMap<String, Any>()
            progress["cosPath"] = cosPath
            progress["localPath"] = localPath
            progress["progress"] = complete * 100.0 / target
            channel.invokeMethod("onProgress", progress)
          })
        }
      })

      //设置返回结果回调
      cosxmlUploadTask.setCosXmlResultListener(object : CosXmlResultListener() {
        fun onSuccess(request: CosXmlRequest?, httpResult: CosXmlResult) {
          Log.d("onSuccess", httpResult.printResult())
          mActivity.runOnUiThread(Runnable {
            result.success(data)
            channel.invokeMethod("onSuccess", cosPath)
          })
        }

        fun onFail(request: CosXmlRequest?, exception: CosXmlClientException?, serviceException: CosXmlServiceException) {
          Log.d("onFail", exception.toString() + serviceException.toString())
          data["message"] = exception.toString() + serviceException.toString()
          mActivity.runOnUiThread(Runnable {
            result.error("400", "error", exception.toString())
            channel.invokeMethod("onFailed", data)
          })
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

  fun onDetachedFromEngine(@NonNull binding: FlutterPluginBinding?) {
    channel.setMethodCallHandler(null)
    channel = null
  }

  ///activity 生命周期
  fun onAttachedToActivity(@NonNull binding: ActivityPluginBinding) {
    mActivity = binding.getActivity()
  }

  fun onDetachedFromActivity() {
    mActivity = null
  }

  fun onDetachedFromActivityForConfigChanges() {}
  fun onReattachedToActivityForConfigChanges(@NonNull binding: ActivityPluginBinding?) {}
}