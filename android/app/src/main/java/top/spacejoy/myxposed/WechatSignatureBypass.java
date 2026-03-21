package top.spacejoy.myxposed;

import android.content.Context;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 微信内部签名校验绕过
 * Hook com.tencent.mm.pluginsdk.model.app.s.a(Context, g, String, boolean) 返回 true
 * 参考: https://github.com/icespite/WXHook
 */
public class WechatSignatureBypass {
    private static final String TAG = "XposedRust";

    /**
     * Hook 微信内部的第三方 APP 签名校验方法
     * @param classLoader 微信的 ClassLoader
     */
    public static void hookAppSignatureCheck(ClassLoader classLoader) {
        try {
            Log.d(TAG, "[+] 开始 Hook 微信内部签名校验");

            // 查找签名验证相关类
            Class<?> clsS = XposedHelpers.findClass(
                "com.tencent.mm.pluginsdk.model.app.s", classLoader);
            Class<?> clsG = XposedHelpers.findClass(
                "com.tencent.mm.pluginsdk.model.app.g", classLoader);

            // Hook 方法 a(Context, g, String, boolean) -> 返回 true
            XposedHelpers.findAndHookMethod(clsS, "a",
                Context.class, clsG, String.class, boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // 获取调用参数用于日志
                        String pkgName = param.args[2] != null ? param.args[2].toString() : "unknown";
                        Log.d(TAG, "[+] 绕过签名校验: " + pkgName);
                        param.setResult(true);
                    }
                });

            Log.d(TAG, "[+] 微信内部签名校验绕过成功");

        } catch (XposedHelpers.ClassNotFoundError e) {
            Log.e(TAG, "[-] 未找到微信签名校验类，版本可能不兼容: " + e.getMessage());
        } catch (NoSuchMethodError e) {
            Log.e(TAG, "[-] 未找到微信签名校验方法，版本可能不兼容: " + e.getMessage());
        } catch (Throwable e) {
            Log.e(TAG, "[-] Hook 微信签名校验失败", e);
        }
    }
}
