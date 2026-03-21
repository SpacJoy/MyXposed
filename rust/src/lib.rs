use jni::objects::{JClass, JObject, JObjectArray, JString, JValue};
use jni::JNIEnv;
use log::{debug, error, info};

/// JNI 入口函数 - 由 Java 的 XposedEntry.initXposed() 调用
#[no_mangle]
pub extern "system" fn Java_top_spacejoy_myxposed_XposedEntry_initXposed(
    mut env: JNIEnv,
    _class: JClass,
    _class_loader: JObject,
    lpparam: JObject,
) {
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Debug)
            .with_tag("XposedRust"),
    );

    info!("Xposed Hook 初始化开始");

    let package_name = match get_string_field(&mut env, &lpparam, "packageName") {
        Ok(name) => name,
        Err(e) => {
            error!("获取包名失败: {:?}", e);
            return;
        }
    };

    info!("目标包名: {}", package_name);

    match package_name.as_str() {
        "com.tencent.mm" => {
            hook_wechat(&mut env, &lpparam);
            bypass_signature_check(&mut env, "com.tencent.mm");
        }
        "com.tencent.mobileqq" => {
            hook_qq(&mut env);
            bypass_signature_check(&mut env, "com.tencent.mobileqq");
        }
        "com.android.phone" => hook_phone(&mut env),
        _ => debug!("未配置对 {} 的 Hook", package_name),
    }

    hook_all_apps(&mut env);

    info!("Xposed Hook 初始化完成");
}

/// Hook 回调 - 在方法执行前调用
#[no_mangle]
pub extern "system" fn Java_top_spacejoy_myxposed_HookCallback_onBeforeHookedMethod(
    mut env: JNIEnv,
    _class: JClass,
    param: JObject,
) {
    let method_name = extract_method_name(&mut env, &param);
    if let Some(name) = method_name {
        debug!("Before: {}", name);
    }
    log_args(&mut env, &param);
}

/// Hook 回调 - 在方法执行后调用
#[no_mangle]
pub extern "system" fn Java_top_spacejoy_myxposed_HookCallback_onAfterHookedMethod(
    mut env: JNIEnv,
    _class: JClass,
    param: JObject,
) {
    let method_name = extract_method_name(&mut env, &param);
    if let Some(name) = method_name {
        debug!("After: {}", name);
    }
    log_result(&mut env, &param);
}

/// 从 MethodHookParam 提取方法名
fn extract_method_name(env: &mut JNIEnv, param: &JObject) -> Option<String> {
    let method = env
        .call_method(param, "getMethod", "()Ljava/lang/reflect/Method;", &[])
        .ok()?
        .l()
        .ok()?;
    let name = env
        .call_method(&method, "getName", "()Ljava/lang/String;", &[])
        .ok()?
        .l()
        .ok()?;
    let jstr = JString::from(name);
    let java_str = env.get_string(&jstr).ok()?;
    let result: String = java_str.into();
    Some(result)
}

/// 获取对象的字符串字段
fn get_string_field(
    env: &mut JNIEnv,
    obj: &JObject,
    field: &str,
) -> Result<String, jni::errors::Error> {
    let value = env.get_field(obj, field, "Ljava/lang/String;")?.l()?;
    let jstr = JString::from(value);
    let java_str = env.get_string(&jstr)?;
    let result: String = java_str.into();
    Ok(result)
}

/// 通用 Hook - 对所有应用生效
fn hook_all_apps(env: &mut JNIEnv) {
    info!("执行通用 Hook");

    let _ = hook_method(
        env,
        "android.app.Activity",
        "onCreate",
        "(Landroid/os/Bundle;)V",
    );
    let _ = hook_method(env, "android.app.Activity", "onResume", "()V");
    let _ = hook_method(env, "android.widget.Toast", "show", "()V");
    let _ = hook_shared_preferences(env);
    let _ = hook_network(env);
}

/// Hook 指定方法
fn hook_method(
    env: &mut JNIEnv,
    class_name: &str,
    method_name: &str,
    _method_sig: &str,
) -> Result<(), jni::errors::Error> {
    let target_class = env.find_class(class_name.replace('.', "/").as_str())?;
    let method_name_jstr = env.new_string(method_name)?;

    // 先查找方法
    let method = {
        let methods = env
            .call_method(
                &target_class,
                "getMethods",
                "()[Ljava/lang/reflect/Method;",
                &[],
            )?
            .l()?;
        let methods_array: JObjectArray = JObjectArray::from(methods);
        let len = env.get_array_length(&methods_array)?;
        let target_name: String = env.get_string(&method_name_jstr)?.into();

        let mut found_method = JObject::null();
        for i in 0..len {
            let method = env.get_object_array_element(&methods_array, i)?;
            let name_result = env
                .call_method(&method, "getName", "()Ljava/lang/String;", &[])?
                .l()?;
            let name_jstr = JString::from(name_result);
            let java_name = env.get_string(&name_jstr)?;
            let m_name: String = java_name.into();
            if m_name == target_name {
                found_method = method;
                break;
            }
        }
        found_method
    };

    if method.is_null() {
        error!("未找到方法: {}.{}", class_name, method_name);
        return Ok(());
    }

    // 创建回调并注册
    let callback = {
        let callback_class = env.find_class("top/spacejoy/myxposed/HookCallback")?;
        env.new_object(&callback_class, "()V", &[])?
    };

    {
        let callback_class = env.find_class("top/spacejoy/myxposed/HookCallback")?;
        env.call_static_method(
            &callback_class,
            "hookMethod",
            "(Ljava/lang/reflect/Member;Ltop/spacejoy/myxposed/HookCallback;)Lde/robv/android/xposed/XC_MethodHook$Unhook;",
            &[JValue::Object(&method), JValue::Object(&callback)],
        )?;
    }

    info!("Hook 成功: {}.{}", class_name, method_name);
    Ok(())
}

/// 绕过签名验证
fn bypass_signature_check(env: &mut JNIEnv, _target_package: &str) {
    info!("启用签名校验绕过");

    // Hook PackageManager.getPackageInfo
    let _ = bypass_get_package_info(env);

    // Hook Signature.equals
    let _ = bypass_signature_equals(env);

    // Hook Signature.toByteArray
    let _ = bypass_signature_to_byte_array(env);
}

/// 绕过 getPackageInfo 签名校验
fn bypass_get_package_info(env: &mut JNIEnv) -> Result<(), jni::errors::Error> {
    info!("Hook PackageManager.getPackageInfo");

    let pm_class = env.find_class("android/app/ApplicationPackageManager")?;
    let method_name_jstr = env.new_string("getPackageInfo")?;

    // 查找 getPackageInfo 方法
    let method = {
        let methods = env
            .call_method(
                &pm_class,
                "getDeclaredMethods",
                "()[Ljava/lang/reflect/Method;",
                &[],
            )?
            .l()?;
        let methods_array: JObjectArray = JObjectArray::from(methods);
        let len = env.get_array_length(&methods_array)?;
        let target_name: String = env.get_string(&method_name_jstr)?.into();

        let mut found_method = JObject::null();
        for i in 0..len {
            let method = env.get_object_array_element(&methods_array, i)?;
            let name_result = env
                .call_method(&method, "getName", "()Ljava/lang/String;", &[])?
                .l()?;
            let name_jstr = JString::from(name_result);
            let java_name = env.get_string(&name_jstr)?;
            let m_name: String = java_name.into();
            if m_name == target_name {
                found_method = method;
                break;
            }
        }
        found_method
    };

    if method.is_null() {
        error!("未找到 getPackageInfo 方法");
        return Ok(());
    }

    // 创建回调并注册
    let callback = {
        let callback_class = env.find_class("top/spacejoy/myxposed/SignatureBypassCallback")?;
        env.new_object(&callback_class, "(I)V", &[JValue::Int(0)])?
    };

    {
        let callback_class = env.find_class("top/spacejoy/myxposed/HookCallback")?;
        env.call_static_method(
            &callback_class,
            "hookMethod",
            "(Ljava/lang/reflect/Member;Ltop/spacejoy/myxposed/HookCallback;)Lde/robv/android/xposed/XC_MethodHook$Unhook;",
            &[JValue::Object(&method), JValue::Object(&callback)],
        )?;
    }

    info!("getPackageInfo Hook 完成");
    Ok(())
}

/// 绕过 Signature.equals
fn bypass_signature_equals(env: &mut JNIEnv) -> Result<(), jni::errors::Error> {
    info!("Hook Signature.equals");

    let sig_class = env.find_class("android/content/pm/Signature")?;

    // 查找 equals 方法
    let method = {
        let methods = env
            .call_method(
                &sig_class,
                "getMethods",
                "()[Ljava/lang/reflect/Method;",
                &[],
            )?
            .l()?;
        let methods_array: JObjectArray = JObjectArray::from(methods);
        let len = env.get_array_length(&methods_array)?;

        let mut found_method = JObject::null();
        for i in 0..len {
            let method = env.get_object_array_element(&methods_array, i)?;
            let name_result = env
                .call_method(&method, "getName", "()Ljava/lang/String;", &[])?
                .l()?;
            let name_jstr = JString::from(name_result);
            let java_name = env.get_string(&name_jstr)?;
            let m_name: String = java_name.into();
            if m_name == "equals" {
                found_method = method;
                break;
            }
        }
        found_method
    };

    if method.is_null() {
        error!("未找到 Signature.equals 方法");
        return Ok(());
    }

    // 创建回调并注册
    let callback = {
        let callback_class = env.find_class("top/spacejoy/myxposed/SignatureBypassCallback")?;
        env.new_object(&callback_class, "(I)V", &[JValue::Int(1)])?
    };

    {
        let callback_class = env.find_class("top/spacejoy/myxposed/HookCallback")?;
        env.call_static_method(
            &callback_class,
            "hookMethod",
            "(Ljava/lang/reflect/Member;Ltop/spacejoy/myxposed/HookCallback;)Lde/robv/android/xposed/XC_MethodHook$Unhook;",
            &[JValue::Object(&method), JValue::Object(&callback)],
        )?;
    }

    info!("Signature.equals Hook 完成");
    Ok(())
}

/// 绕过 Signature.toByteArray
fn bypass_signature_to_byte_array(env: &mut JNIEnv) -> Result<(), jni::errors::Error> {
    info!("Hook Signature.toByteArray");

    let sig_class = env.find_class("android/content/pm/Signature")?;

    // 查找 toByteArray 方法
    let method = {
        let methods = env
            .call_method(
                &sig_class,
                "getMethods",
                "()[Ljava/lang/reflect/Method;",
                &[],
            )?
            .l()?;
        let methods_array: JObjectArray = JObjectArray::from(methods);
        let len = env.get_array_length(&methods_array)?;

        let mut found_method = JObject::null();
        for i in 0..len {
            let method = env.get_object_array_element(&methods_array, i)?;
            let name_result = env
                .call_method(&method, "getName", "()Ljava/lang/String;", &[])?
                .l()?;
            let name_jstr = JString::from(name_result);
            let java_name = env.get_string(&name_jstr)?;
            let m_name: String = java_name.into();
            if m_name == "toByteArray" {
                found_method = method;
                break;
            }
        }
        found_method
    };

    if method.is_null() {
        error!("未找到 Signature.toByteArray 方法");
        return Ok(());
    }

    // 创建回调并注册
    let callback = {
        let callback_class = env.find_class("top/spacejoy/myxposed/SignatureBypassCallback")?;
        env.new_object(&callback_class, "(I)V", &[JValue::Int(2)])?
    };

    {
        let callback_class = env.find_class("top/spacejoy/myxposed/HookCallback")?;
        env.call_static_method(
            &callback_class,
            "hookMethod",
            "(Ljava/lang/reflect/Member;Ltop/spacejoy/myxposed/HookCallback;)Lde/robv/android/xposed/XC_MethodHook$Unhook;",
            &[JValue::Object(&method), JValue::Object(&callback)],
        )?;
    }

    info!("Signature.toByteArray Hook 完成");
    Ok(())
}

/// Hook 微信
fn hook_wechat(env: &mut JNIEnv, lpparam: &JObject) {
    info!("执行微信 Hook");

    let _ = hook_method(
        env,
        "com.tencent.mm.ui.LauncherUI",
        "onCreate",
        "(Landroid/os/Bundle;)V",
    );

    // 绕过微信内部第三方 APP 分享签名校验
    let _ = bypass_wechat_internal_signature(env, lpparam);
}

/// 绕过微信内部的第三方 APP 签名校验
/// Hook com.tencent.mm.pluginsdk.model.app.s.a(Context, g, String, boolean) -> true
/// 参考: https://github.com/icespite/WXHook
fn bypass_wechat_internal_signature(
    env: &mut JNIEnv,
    lpparam: &JObject,
) -> Result<(), jni::errors::Error> {
    info!("[+] 启用微信内部签名校验绕过");

    // 从 lpparam 获取 classLoader
    let class_loader = env
        .get_field(lpparam, "classLoader", "Ljava/lang/ClassLoader;")?
        .l()?;

    // 调用 Java 静态方法: WechatSignatureBypass.hookAppSignatureCheck(classLoader)
    let helper_class = env.find_class("top/spacejoy/myxposed/WechatSignatureBypass")?;
    env.call_static_method(
        &helper_class,
        "hookAppSignatureCheck",
        "(Ljava/lang/ClassLoader;)V",
        &[JValue::Object(&class_loader)],
    )?;

    info!("[+] 微信内部签名校验绕过完成");
    Ok(())
}

/// Hook QQ
fn hook_qq(env: &mut JNIEnv) {
    info!("执行 QQ Hook");

    // Hook SplashActivity
    let _ = hook_method(
        env,
        "com.tencent.mobileqq.activity.SplashActivity",
        "onCreate",
        "(Landroid/os/Bundle;)V",
    );

    // QQ OpenAPI 签名校验绕过
    info!("[+] QQ OpenAPI 签名验证 Hook 开始");

    // 方案 1：Hook openapi.a.c 返回 true
    let _ = hook_return_const(env, "com.tencent.mobileqq.openapi.a", "c", true);

    // 方案 2：Hook OpenApiManager.verifyCallingPackage 返回 true
    let _ = hook_return_const(
        env,
        "com.tencent.mobileqq.openapi.OpenApiManager",
        "verifyCallingPackage",
        true,
    );

    // 方案 3：Hook OpenApiManager.registerThirdApp 记录参数
    let _ = hook_register_third_app(env);

    info!("[+] QQ OpenAPI 签名验证 Hook 完成");
}

/// Hook 方法并返回常量值
fn hook_return_const(
    env: &mut JNIEnv,
    class_name: &str,
    method_name: &str,
    return_value: bool,
) -> Result<(), jni::errors::Error> {
    info!(
        "Hook {}.{} -> 返回 {}",
        class_name, method_name, return_value
    );

    let target_class = match env.find_class(class_name.replace('.', "/").as_str()) {
        Ok(c) => c,
        Err(e) => {
            error!("未找到类 {}: {:?}", class_name, e);
            return Ok(());
        }
    };

    // 查找方法
    let method = {
        let methods = env
            .call_method(
                &target_class,
                "getDeclaredMethods",
                "()[Ljava/lang/reflect/Method;",
                &[],
            )?
            .l()?;
        let methods_array: JObjectArray = JObjectArray::from(methods);
        let len = env.get_array_length(&methods_array)?;

        let mut found_method = JObject::null();
        for i in 0..len {
            let method = env.get_object_array_element(&methods_array, i)?;
            let name_result = env
                .call_method(&method, "getName", "()Ljava/lang/String;", &[])?
                .l()?;
            let name_jstr = JString::from(name_result);
            let java_name = env.get_string(&name_jstr)?;
            let m_name: String = java_name.into();
            if m_name == method_name {
                found_method = method;
                break;
            }
        }
        found_method
    };

    if method.is_null() {
        error!("未找到方法: {}.{}", class_name, method_name);
        return Ok(());
    }

    // 创建返回常量的回调
    let callback = {
        let callback_class = env.find_class("top/spacejoy/myxposed/ReturnConstCallback")?;
        let bool_value = if return_value { 1 } else { 0 };
        env.new_object(&callback_class, "(Z)V", &[JValue::Bool(bool_value)])?
    };

    // 注册 Hook
    {
        let callback_class = env.find_class("top/spacejoy/myxposed/HookCallback")?;
        env.call_static_method(
            &callback_class,
            "hookMethod",
            "(Ljava/lang/reflect/Member;Ltop/spacejoy/myxposed/HookCallback;)Lde/robv/android/xposed/XC_MethodHook$Unhook;",
            &[JValue::Object(&method), JValue::Object(&callback)],
        )?;
    }

    info!(
        "Hook 成功: {}.{} -> {}",
        class_name, method_name, return_value
    );
    Ok(())
}

/// Hook registerThirdApp 记录参数
fn hook_register_third_app(env: &mut JNIEnv) -> Result<(), jni::errors::Error> {
    info!("Hook OpenApiManager.registerThirdApp");

    let target_class = match env.find_class("com/tencent/mobileqq/openapi/OpenApiManager") {
        Ok(c) => c,
        Err(e) => {
            error!("未找到类 OpenApiManager: {:?}", e);
            return Ok(());
        }
    };

    // 查找 registerThirdApp 方法
    let method = {
        let methods = env
            .call_method(
                &target_class,
                "getDeclaredMethods",
                "()[Ljava/lang/reflect/Method;",
                &[],
            )?
            .l()?;
        let methods_array: JObjectArray = JObjectArray::from(methods);
        let len = env.get_array_length(&methods_array)?;

        let mut found_method = JObject::null();
        for i in 0..len {
            let method = env.get_object_array_element(&methods_array, i)?;
            let name_result = env
                .call_method(&method, "getName", "()Ljava/lang/String;", &[])?
                .l()?;
            let name_jstr = JString::from(name_result);
            let java_name = env.get_string(&name_jstr)?;
            let m_name: String = java_name.into();
            if m_name == "registerThirdApp" {
                found_method = method;
                break;
            }
        }
        found_method
    };

    if method.is_null() {
        error!("未找到 registerThirdApp 方法");
        return Ok(());
    }

    // 创建回调
    let callback = {
        let callback_class = env.find_class("top/spacejoy/myxposed/ReturnConstCallback")?;
        env.new_object(&callback_class, "(I)V", &[JValue::Int(3)])?
    };

    // 注册 Hook
    {
        let callback_class = env.find_class("top/spacejoy/myxposed/HookCallback")?;
        env.call_static_method(
            &callback_class,
            "hookMethod",
            "(Ljava/lang/reflect/Member;Ltop/spacejoy/myxposed/HookCallback;)Lde/robv/android/xposed/XC_MethodHook$Unhook;",
            &[JValue::Object(&method), JValue::Object(&callback)],
        )?;
    }

    info!("registerThirdApp Hook 完成");
    Ok(())
}

/// Hook 电话应用
fn hook_phone(env: &mut JNIEnv) {
    info!("执行电话应用 Hook");
    let _ = hook_method(
        env,
        "com.android.incallui.InCallActivity",
        "onCreate",
        "(Landroid/os/Bundle;)V",
    );
}

/// Hook SharedPreferences
fn hook_shared_preferences(env: &mut JNIEnv) -> Result<(), jni::errors::Error> {
    info!("Hook SharedPreferences");
    let _ = hook_method(
        env,
        "android.app.SharedPreferencesImpl",
        "getString",
        "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
    );
    let _ = hook_method(
        env,
        "android.app.SharedPreferencesImpl$EditorImpl",
        "putString",
        "(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;",
    );
    Ok(())
}

/// Hook 网络请求
fn hook_network(env: &mut JNIEnv) -> Result<(), jni::errors::Error> {
    info!("Hook 网络请求");
    let _ = hook_method(env, "java.net.HttpURLConnection", "connect", "()V");
    let _ = hook_method(
        env,
        "okhttp3.internal.http.RealInterceptorChain",
        "proceed",
        "(Lokhttp3/Request;)Lokhttp3/Response;",
    );
    Ok(())
}

/// 从对象提取字符串（用于日志）
fn to_string(env: &mut JNIEnv, obj: &JObject) -> Option<String> {
    let str_result = env
        .call_method(obj, "toString", "()Ljava/lang/String;", &[])
        .ok()?;
    let str_jvalue = str_result.l().ok()?;
    let str_jstr = JString::from(str_jvalue);
    let java_str = env.get_string(&str_jstr).ok()?;
    Some(java_str.into())
}

/// 记录方法参数
fn log_args(env: &mut JNIEnv, param: &JObject) {
    let args = match env.call_method(param, "getArgs", "()[Ljava/lang/Object;", &[]) {
        Ok(r) => match r.l() {
            Ok(a) => a,
            Err(_) => return,
        },
        Err(_) => return,
    };

    let args_array = JObjectArray::from(args);
    let len = match env.get_array_length(&args_array) {
        Ok(l) => l,
        Err(_) => return,
    };

    for i in 0..len {
        if let Ok(arg) = env.get_object_array_element(&args_array, i) {
            if let Some(s) = to_string(env, &arg) {
                debug!("参数[{}]: {}", i, s);
            }
        }
    }
}

/// 记录方法返回值
fn log_result(env: &mut JNIEnv, param: &JObject) {
    let result = match env.call_method(param, "getResult", "()Ljava/lang/Object;", &[]) {
        Ok(r) => match r.l() {
            Ok(res) => res,
            Err(_) => return,
        },
        Err(_) => return,
    };

    if result.is_null() {
        return;
    }

    if let Some(s) = to_string(env, &result) {
        debug!("返回值: {}", s);
    }
}

#[cfg(test)]
mod tests {
    #[test]
    fn test_basic() {
        assert_eq!(2 + 2, 4);
    }
}
