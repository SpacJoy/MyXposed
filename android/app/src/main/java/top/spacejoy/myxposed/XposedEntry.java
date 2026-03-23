package top.spacejoy.myxposed;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedEntry implements IXposedHookLoadPackage {
    private static final String TAG = "XposedEntry";
    private static final String STATUS_FILE = "/data/local/tmp/myxposed_status.txt";

    // 加载Rust编译的so库
    static {
        System.loadLibrary("rust_core");
    }

    // 声明Rust实现的Native方法，用于初始化Hook逻辑
    private native void initXposed(ClassLoader classLoader, XC_LoadPackage.LoadPackageParam lpparam);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 标记模块已激活
        String packageName = lpparam.appInfo != null ? lpparam.appInfo.packageName : "unknown";
        saveActivationStatus(packageName);
        
        // 调用Rust的Hook初始化逻辑
        initXposed(lpparam.classLoader, lpparam);
    }

    /**
     * 保存模块激活状态到文件
     */
    private void saveActivationStatus(String packageName) {
        try {
            File statusFile = new File(STATUS_FILE);
            File parentDir = statusFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            FileWriter writer = new FileWriter(statusFile);
            writer.write("activated=true\n");
            writer.write("timestamp=" + System.currentTimeMillis() + "\n");
            writer.write("package=" + packageName + "\n");
            writer.close();
            
            Log.d(TAG, "模块激活状态已保存到: " + STATUS_FILE);
        } catch (Exception e) {
            Log.e(TAG, "保存激活状态失败: " + e.getMessage());
        }
    }
}
