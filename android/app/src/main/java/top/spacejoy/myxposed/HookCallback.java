package top.spacejoy.myxposed;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;

public class HookCallback extends XC_MethodHook {
    private static final Map<String, HookCallback> callbacks = new HashMap<>();
    
    private native void onBeforeHookedMethod(MethodHookParam param);
    private native void onAfterHookedMethod(MethodHookParam param);

    static {
        System.loadLibrary("rust_core");
    }

    public HookCallback() {
        super();
    }

    public HookCallback(int priority) {
        super(priority);
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        onBeforeHookedMethod(param);
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        onAfterHookedMethod(param);
    }

    public static Unhook hookMethod(Member method, HookCallback callback) {
        return XposedBridge.hookMethod(method, callback);
    }

    public static void registerCallback(String key, HookCallback callback) {
        callbacks.put(key, callback);
    }

    public static HookCallback getCallback(String key) {
        return callbacks.get(key);
    }
}
