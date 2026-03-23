package top.spacejoy.myxposed;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedEntry implements IXposedHookLoadPackage {
    private static final String TAG = "MyXposed";
    
    // 可能的 Xposed 配置目录
    private static final String[] XPOSED_DIRS = {
        "/data/adb/lspd",                           // LSPosed
        "/data/user_de/0/org.lsposed.manager",      // LSPosed (旧版)
        "/data/data/org.meowcat.edxposed.manager",  // EdXposed
        "/data/user_de/0/org.meowcat.edxposed.manager",
        "/data/data/de.robv.android.xposed.installer", // 原版 Xposed
        "/data/user_de/0/de.robv.android.xposed.installer",
        "/data/local/tmp"                           // 备用位置
    };

    // 加载Rust编译的so库
    static {
        System.loadLibrary("rust_core");
    }

    // 声明Rust实现的Native方法
    private native void initXposed(ClassLoader classLoader, XC_LoadPackage.LoadPackageParam lpparam);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Log.i(TAG, "==============================");
        Log.i(TAG, "模块被加载!");
        Log.i(TAG, "目标包名: " + lpparam.packageName);
        Log.i(TAG, "==============================");
        
        // 保存激活状态
        saveActivationStatus();
        
        // 调用 Rust 的 Hook 初始化逻辑
        initXposed(lpparam.classLoader, lpparam);
    }

    /**
     * 保存激活状态到多个位置
     */
    private void saveActivationStatus() {
        for (String basePath : XPOSED_DIRS) {
            try {
                File baseDir = new File(basePath);
                if (!baseDir.exists() || !baseDir.canWrite()) {
                    continue;
                }
                
                File statusDir = new File(baseDir, "myxposed");
                if (!statusDir.exists()) {
                    if (!statusDir.mkdirs()) {
                        continue;
                    }
                }
                
                File statusFile = new File(statusDir, "activated");
                FileWriter writer = new FileWriter(statusFile);
                writer.write("activated=true\n");
                writer.write("time=" + System.currentTimeMillis() + "\n");
                writer.close();
                
                statusFile.setReadable(true, false);
                statusDir.setReadable(true, false);
                
                Log.i(TAG, "激活状态已保存到: " + statusFile.getAbsolutePath());
                return; // 成功写入一个位置就返回
            } catch (Exception e) {
                Log.w(TAG, "写入 " + basePath + " 失败: " + e.getMessage());
            }
        }
        
        // 所有位置都失败，尝试直接写入备用文件
        try {
            File backupFile = new File("/data/local/tmp/.myxposed_activated");
            FileWriter writer = new FileWriter(backupFile);
            writer.write("1");
            writer.close();
            backupFile.setReadable(true, false);
            Log.i(TAG, "激活状态已保存到备用文件: " + backupFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "所有写入尝试都失败", e);
        }
    }
}
