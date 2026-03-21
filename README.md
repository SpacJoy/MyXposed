# MyXposed

基于 Flutter + Rust 开发的 Xposed 模块，提供高性能的 Hook 能力。

## 技术栈

- **Flutter** - UI 界面
- **Rust** - 核心 Hook 逻辑（通过 JNI 调用 Xposed API）
- **Xposed Framework** - Android Hook 框架

## 项目结构

```
MyXposed/
├── android/                    # Android 原生代码
│   └── app/src/main/java/
│       └── top/spacejoy/myxposed/
│           ├── XposedEntry.java    # Xposed 入口
│           └── HookCallback.java   # Hook 回调桥接
├── lib/                        # Flutter 代码
│   ├── main.dart               # 主页面
│   └── pages/
│       └── setting_page.dart   # 设置页面
└── rust/                       # Rust 核心代码
    └── src/
        └── lib.rs              # JNI + Hook 实现
```

## 环境要求

- Flutter SDK 3.10+
- Rust 1.70+
- Android SDK (API 36)
- NDK 28.2+
- Java 17

## 编译

### 1. 安装依赖

```bash
# Flutter 依赖
flutter pub get

# Rust 依赖（自动）
cd rust && cargo build
```

### 2. 编译 APK

```bash
# 仅编译 arm64 架构（推荐，体积小、速度快）
flutter build apk --target-platform android-arm64

# 编译所有架构
flutter build apk

# Debug 模式
flutter build apk --debug
```

### 3. 输出位置

```
android/build/app/outputs/flutter-apk/app-release.apk
```

## 网络代理配置

如果遇到 Gradle 下载失败，配置代理：

**android/gradle.properties**
```properties
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7890
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7890
```

## 功能特性

### 已实现

- [x] Activity 生命周期 Hook
- [x] SharedPreferences 读写监控
- [x] 网络请求 Hook (HttpURLConnection/OkHttp)
- [x] Toast 显示 Hook
- [x] 按包名分发 Hook 逻辑

### 计划中

- [ ] 微信特定功能 Hook
- [ ] QQ 特定功能 Hook
- [ ] 通话记录 Hook
- [ ] 配置持久化
- [ ] 日志导出

## 使用方法

1. 安装 Xposed Framework (LSPosed 推荐)
2. 安装编译好的 APK
3. 在 Xposed/LSPosed 中启用本模块
4. 重启目标应用

## 开发说明

### 添加新的 Hook

编辑 `rust/src/lib.rs`：

```rust
fn hook_wechat(env: &mut JNIEnv) {
    let _ = hook_method(
        env,
        "com.tencent.mm.ui.LauncherUI",  // 目标类
        "onCreate",                       // 目标方法
        "(Landroid/os/Bundle;)V",         // 方法签名
    );
}
```

### Hook 回调处理

在 `onBeforeHookedMethod` 和 `onAfterHookedMethod` 中处理：

```rust
#[no_mangle]
pub extern "system" fn Java_top_spacejoy_myxposed_HookCallback_onBeforeHookedMethod(
    mut env: JNIEnv,
    _class: JClass,
    param: JObject,
) {
    // 获取方法名
    let method_name = extract_method_name(&mut env, &param);
    // 获取参数
    log_args(&mut env, &param);
}
```

## License

仅供学习和个人使用。
