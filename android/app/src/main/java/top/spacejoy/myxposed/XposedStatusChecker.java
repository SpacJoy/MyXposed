package top.spacejoy.myxposed;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Xposed模块激活状态检测器
 */
public class XposedStatusChecker {
    private static final String TAG = "XposedStatusChecker";
    
    // 可能的 Xposed 配置目录
    private static final String[] XPOSED_DIRS = {
        "/data/adb/lspd",                           // LSPosed
        "/data/user_de/0/org.lsposed.manager",      // LSPosed (旧版)
        "/data/data/org.meowcat.edxposed.manager",  // EdXposed
        "/data/user_de/0/org.meowcat.edxposed.manager",
        "/data/data/de.robv.android.xposed.installer", // 原版 Xposed
        "/data/user_de/0/de.robv.android.xposed.installer"
    };

    /**
     * 检查模块是否被激活
     */
    public static boolean isModuleActivated(Context context) {
        // 首先尝试通过 XposedBridge BASE_DIR
        String xposedBaseDir = getXposedBaseDir();
        if (xposedBaseDir != null) {
            File statusFile = new File(xposedBaseDir, "myxposed/activated");
            if (statusFile.exists()) {
                Log.d(TAG, "找到激活状态文件: " + statusFile.getAbsolutePath());
                return true;
            }
        }
        
        // 尝试已知的 Xposed 配置目录
        for (String dir : XPOSED_DIRS) {
            File statusFile = new File(dir, "myxposed/activated");
            if (statusFile.exists()) {
                Log.d(TAG, "找到激活状态文件: " + statusFile.getAbsolutePath());
                return true;
            }
        }
        
        // 尝试备用位置
        File backupFile = new File("/data/local/tmp/.myxposed_activated");
        if (backupFile.exists()) {
            Log.d(TAG, "找到备用激活状态文件: " + backupFile.getAbsolutePath());
            return true;
        }
        
        Log.d(TAG, "未找到激活状态文件");
        return false;
    }

    /**
     * 获取 Xposed 基础目录
     */
    private static String getXposedBaseDir() {
        try {
            Class<?> xposedBridge = Class.forName("de.robv.android.xposed.XposedBridge");
            java.lang.reflect.Field field = xposedBridge.getField("BASE_DIR");
            return (String) field.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取激活状态描述
     */
    public static String getActivationStatus(Context context) {
        boolean activated = isModuleActivated(context);
        return activated ? "已激活" : "未激活";
    }
}
