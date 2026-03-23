package top.spacejoy.myxposed;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Xposed模块激活状态检测器
 * 检测模块是否被Xposed框架启用并加载
 */
public class XposedStatusChecker {
    private static final String TAG = "XposedStatusChecker";
    private static final String STATUS_FILE = "/data/local/tmp/myxposed_status.txt";

    /**
     * 检查模块是否被激活（被Xposed框架加载）
     * @param context 应用上下文（未使用，保留接口兼容性）
     * @return true 如果模块已被激活
     */
    public static boolean isModuleActivated(Context context) {
        try {
            File statusFile = new File(STATUS_FILE);
            if (!statusFile.exists()) {
                Log.d(TAG, "状态文件不存在: " + STATUS_FILE);
                return false;
            }

            Map<String, String> status = readStatusFile();
            boolean activated = "true".equals(status.get("activated"));
            Log.d(TAG, "模块激活状态: " + activated);
            return activated;
        } catch (Exception e) {
            Log.e(TAG, "检测模块激活状态失败", e);
            return false;
        }
    }

    /**
     * 获取模块激活时间
     * @param context 应用上下文
     * @return 激活时间戳，未激活返回0
     */
    public static long getActivationTime(Context context) {
        try {
            Map<String, String> status = readStatusFile();
            String timestamp = status.get("timestamp");
            if (timestamp != null && !timestamp.isEmpty()) {
                return Long.parseLong(timestamp);
            }
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "获取激活时间失败", e);
            return 0;
        }
    }

    /**
     * 获取最后Hook的包名
     * @param context 应用上下文
     * @return 最后Hook的包名，未激活返回null
     */
    public static String getLastHookedPackage(Context context) {
        try {
            Map<String, String> status = readStatusFile();
            return status.get("package");
        } catch (Exception e) {
            Log.e(TAG, "获取最后Hook包名失败", e);
            return null;
        }
    }

    /**
     * 获取详细的激活状态信息
     * @param context 应用上下文
     * @return 状态描述字符串
     */
    public static String getActivationStatus(Context context) {
        boolean activated = isModuleActivated(context);
        if (activated) {
            String lastPackage = getLastHookedPackage(context);
            if (lastPackage != null && !lastPackage.isEmpty()) {
                return "已激活 - 最后Hook: " + lastPackage;
            }
            return "已激活";
        } else {
            return "未激活";
        }
    }

    /**
     * 读取状态文件
     * @return 状态键值对
     */
    private static Map<String, String> readStatusFile() {
        Map<String, String> status = new HashMap<>();
        try {
            File statusFile = new File(STATUS_FILE);
            if (!statusFile.exists()) {
                return status;
            }

            BufferedReader reader = new BufferedReader(new FileReader(statusFile));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    status.put(parts[0].trim(), parts[1].trim());
                }
            }
            reader.close();
        } catch (Exception e) {
            Log.e(TAG, "读取状态文件失败", e);
        }
        return status;
    }

    /**
     * 重置激活状态（用于测试）
     * @param context 应用上下文
     */
    public static void resetActivationStatus(Context context) {
        try {
            File statusFile = new File(STATUS_FILE);
            if (statusFile.exists()) {
                statusFile.delete();
            }
            Log.d(TAG, "激活状态已重置");
        } catch (Exception e) {
            Log.e(TAG, "重置激活状态失败", e);
        }
    }
}
