import 'package:flutter/material.dart';

class SettingPage extends StatefulWidget {
  const SettingPage({super.key});

  @override
  State<SettingPage> createState() => _SettingPageState();
}

class _SettingPageState extends State<SettingPage> {
  bool _hookEnabled = true;
  bool _logEnabled = true;
  bool _networkHook = false;
  bool _spHook = true;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("模块设置")),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 功能开关区域
          const Padding(
            padding: EdgeInsets.only(bottom: 8),
            child: Text(
              "功能开关",
              style: TextStyle(fontSize: 14, color: Colors.grey),
            ),
          ),
          Card(
            child: Column(
              children: [
                SwitchListTile(
                  title: const Text("启用 Hook"),
                  subtitle: const Text("总开关"),
                  value: _hookEnabled,
                  onChanged: (v) => setState(() => _hookEnabled = v),
                ),
                const Divider(height: 1),
                SwitchListTile(
                  title: const Text("启用日志"),
                  subtitle: const Text("输出调试日志"),
                  value: _logEnabled,
                  onChanged: (v) => setState(() => _logEnabled = v),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // Hook 目标区域
          const Padding(
            padding: EdgeInsets.only(bottom: 8),
            child: Text(
              "Hook 目标",
              style: TextStyle(fontSize: 14, color: Colors.grey),
            ),
          ),
          Card(
            child: Column(
              children: [
                SwitchListTile(
                  title: const Text("网络请求"),
                  subtitle: const Text("Hook HttpURLConnection/OkHttp"),
                  value: _networkHook,
                  onChanged: (v) => setState(() => _networkHook = v),
                ),
                const Divider(height: 1),
                SwitchListTile(
                  title: const Text("SharedPreferences"),
                  subtitle: const Text("监控配置读写"),
                  value: _spHook,
                  onChanged: (v) => setState(() => _spHook = v),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 应用列表入口
          const Padding(
            padding: EdgeInsets.only(bottom: 8),
            child: Text(
              "应用管理",
              style: TextStyle(fontSize: 14, color: Colors.grey),
            ),
          ),
          Card(
            child: ListTile(
              title: const Text("选择应用"),
              subtitle: const Text("配置需要 Hook 的应用"),
              trailing: const Icon(Icons.chevron_right),
              onTap: () {
                // TODO: 跳转到应用选择页面
              },
            ),
          ),
          const SizedBox(height: 24),

          // 保存按钮
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: _saveSettings,
              child: const Padding(
                padding: EdgeInsets.symmetric(vertical: 12),
                child: Text("保存设置", style: TextStyle(fontSize: 16)),
              ),
            ),
          ),
          const SizedBox(height: 12),

          // 重启按钮
          SizedBox(
            width: double.infinity,
            child: OutlinedButton(
              onPressed: _restartFramework,
              child: const Padding(
                padding: EdgeInsets.symmetric(vertical: 12),
                child: Text("重启框架", style: TextStyle(fontSize: 16)),
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _saveSettings() {
    // TODO: 调用 Rust 保存配置
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text("设置已保存")),
    );
  }

  void _restartFramework() {
    // TODO: 调用 Rust 重启框架
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text("框架已重启")),
    );
  }
}
