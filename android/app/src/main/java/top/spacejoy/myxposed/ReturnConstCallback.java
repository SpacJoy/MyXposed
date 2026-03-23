package top.spacejoy.myxposed;

import android.util.Log;

/**
 * 返回常量值的回调
 * mode: 0=返回bool, 3=registerThirdApp
 */
public class ReturnConstCallback extends HookCallback {
    private static final String TAG = "XposedRust";
    private final boolean boolValue;
    private final int mode;

    public ReturnConstCallback(boolean boolValue) {
        super();
        this.boolValue = boolValue;
        this.mode = 0;
    }

    public ReturnConstCallback(int mode) {
        super();
        this.boolValue = false;
        this.mode = mode;
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        switch (mode) {
            case 0:
                // 返回常量 bool
                param.setResult(boolValue);
                Log.d(TAG, "[*] 方法被拦截，返回: " + boolValue);
                break;
            case 3:
                // registerThirdApp 拦截
                logRegisterThirdApp(param);
                break;
        }
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        // 不需要处理
    }

    /**
     * 记录 registerThirdApp 的参数
     * 参数: (String appId, String pkgName, long uin, int appId2, int appType, String signature)
     */
    private void logRegisterThirdApp(MethodHookParam param) {
        try {
            Object[] args = param.args;
            if (args != null && args.length >= 2) {
                String appId = args[0] != null ? args[0].toString() : "null";
                String pkgName = args[1] != null ? args[1].toString() : "null";

                Log.d(TAG, "[*] 拦截 registerThirdApp");
                Log.d(TAG, "    AppId: " + appId);
                Log.d(TAG, "    PkgName: " + pkgName);

                if (args.length >= 3 && args[2] != null) {
                    Log.d(TAG, "    Uin: " + args[2]);
                }
                if (args.length >= 6 && args[5] != null) {
                    String signature = args[5].toString();
                    Log.d(TAG, "    签名: " + signature);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "记录 registerThirdApp 参数失败", e);
        }
    }
}
