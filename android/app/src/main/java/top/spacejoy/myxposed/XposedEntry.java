package top.spacejoy.myxposed;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedEntry implements IXposedHookLoadPackage {
    // 加载Rust编译的so库
    static {
        System.loadLibrary("rust_core");
    }

    // 声明Rust实现的Native方法，用于初始化Hook逻辑
    private native void initXposed(ClassLoader classLoader, XC_LoadPackage.LoadPackageParam lpparam);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 调用Rust的Hook初始化逻辑
        initXposed(lpparam.classLoader, lpparam);
    }
}