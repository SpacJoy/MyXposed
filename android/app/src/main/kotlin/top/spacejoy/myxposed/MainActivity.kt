package top.spacejoy.myxposed

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.myxposed/status"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "isModuleActivated" -> {
                    val activated = XposedStatusChecker.isModuleActivated(applicationContext)
                    result.success(activated)
                }
                "getActivationStatus" -> {
                    val status = XposedStatusChecker.getActivationStatus(applicationContext)
                    result.success(status)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }
}
