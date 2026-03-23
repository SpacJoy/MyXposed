import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:dynamic_color/dynamic_color.dart';
import 'pages/setting_page.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return DynamicColorBuilder(
      builder: (ColorScheme? lightDynamic, ColorScheme? darkDynamic) {
        return MaterialApp(
          title: 'MyXposed',
          theme: ThemeData(
            colorScheme: lightDynamic ?? ColorScheme.fromSeed(seedColor: Colors.blue),
            useMaterial3: true,
          ),
          darkTheme: ThemeData(
            colorScheme: darkDynamic ?? ColorScheme.fromSeed(seedColor: Colors.blue, brightness: Brightness.dark),
            useMaterial3: true,
          ),
          themeMode: ThemeMode.system,
          home: const HomePage(),
        );
      },
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  static const platform = MethodChannel('com.myxposed/status');
  
  String _moduleStatus = "检测中...";
  Color _moduleStatusColor = Colors.grey;
  String _rustStatus = "已加载";
  String _hookCount = "1 个";

  @override
  void initState() {
    super.initState();
    _checkModuleStatus();
  }

  Future<void> _checkModuleStatus() async {
    setState(() {
      _moduleStatus = "检测中...";
      _moduleStatusColor = Colors.grey;
    });

    try {
      final bool isActivated = await platform.invokeMethod('isModuleActivated');
      
      setState(() {
        if (isActivated) {
          _moduleStatus = "已激活";
          _moduleStatusColor = Colors.green;
        } else {
          _moduleStatus = "未激活";
          _moduleStatusColor = Colors.red;
        }
      });
    } on PlatformException catch (e) {
      setState(() {
        _moduleStatus = "检测失败";
        _moduleStatusColor = Colors.orange;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("MyXposed"),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _checkModuleStatus,
            tooltip: "刷新状态",
          ),
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const SettingPage()),
              );
            },
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 状态卡片
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    "模块状态",
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 12),
                  _buildStatusItem("模块激活状态", _moduleStatus, _moduleStatusColor),
                  _buildStatusItem("Rust 核心", _rustStatus, Colors.green),
                  _buildStatusItem("Hook 数量", _hookCount, Colors.orange),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // 快捷操作
          const Padding(
            padding: EdgeInsets.only(bottom: 8),
            child: Text(
              "快捷操作",
              style: TextStyle(fontSize: 14, color: Colors.grey),
            ),
          ),
          Card(
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.android),
                  title: const Text("目标应用"),
                  subtitle: const Text("微信"),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () {},
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.settings),
                  title: const Text("模块设置"),
                  subtitle: const Text("配置 Hook 参数"),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => const SettingPage()),
                    );
                  },
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.article),
                  title: const Text("查看日志"),
                  subtitle: const Text("调试信息"),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () {},
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),

          // 重启按钮
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: () {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text("框架重启中...")),
                );
              },
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

  Widget _buildStatusItem(String label, String value, Color color) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(
              value,
              style: TextStyle(color: color, fontWeight: FontWeight.w500),
            ),
          ),
        ],
      ),
    );
  }
}
