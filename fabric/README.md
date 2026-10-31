# YSM 体验优化 — Fabric 1.2.0

[English](README.en.md)

针对官方 YSM 2.6.5 的客户端补丁。核心逻辑与 Forge / NeoForge 共用，两个 Fabric 版本分别适配、测试和打包。

| Minecraft | 官方 YSM | 安装包 | Java | 最低 Fabric Loader |
|---|---|---|---|---|
| 1.20.1 | 2.6.5 Fabric | `ysm-enhancements-1.2.0-fabric-mc1.20.1.jar` | 17 或更高 | 0.15.0 |
| 1.21.1 | 2.6.5 Fabric | `ysm-enhancements-1.2.0-fabric-mc1.21.1.jar` | 21 或更高 | 0.16.0 |

截至 2026-11-01，官方 YSM 2.6.5 的 Fabric 支持范围就是这两档，最低为 1.20.1。历史上的 YSM 2.4.1 支持到 1.16.5，属于另一套适配目标。支持的是表中的具体版本，并非 1.20.1 到 1.21.1 之间的所有小版本。[官方支持列表](https://yesstevemodel.github.io/en/wiki/faq/)

## 完成的功能和修复

- 移植 1.1.0 的按钮面板：自动排布、完整目录路径、模型预览旋转缩放、数值输入、键盘导航及错误回退。
- 修复动作锁定长按重复切换；阻止按键状态重同步、窗口失焦和 GUI 中的误触发。保留键盘、扫描码和鼠标绑定，并遵循快捷键提供方的修饰键状态。
- 清理窗口失焦、打开 GUI、释放鼠标捕获和物理松键后残留的 YSM 按键状态。
- 修复原轮盘隐藏控件仍可点击、滚动箭头状态过期、科学计数法读取、负数开关状态、搜索光标/选区丢失和旧页面回调叠加。
- 按作者明确的缩放回退表达式初始化超出滑条范围的零初值；仅修正显示，不修改模型变量。
- 分别适配两版 Fabric 自带的滑块，输入数值和方向键均只触发一次模型回调。

Fabric 原版已支持鼠标快捷键，因此不加入 Forge 的鼠标事件转发，也不提供重复的 `mouseShortcuts` 配置项。

## 安装与配置

保留对应版本的官方 YSM 2.6.5 和它要求的 Fabric API，将表中匹配的一个补丁 JAR 放入客户端 `mods` 文件夹。升级时先移除旧的 YSM Enhancements 补丁，两个版本的包不能同时安装；服务器无需安装本补丁。

最低加载器版本按当前官方原包及其自带依赖确定：两版 YSM 本体都要求 Loader ≥ 0.15.0；1.21.1 包内的 Forge Config API Port 21.1.3 另外要求 Loader ≥ 0.16.0。因此 1.21.1 的实际最低要求为 0.16.0，1.20.1 为 0.15.0。如果另行安装的 Fabric API 或其他模组要求更高版本，仍需满足它们的要求。

配置通过 YSM 已依赖的 Forge Config API Port 注册，保存在 `config/ysm_enhancements-client.toml`。两个版本使用相同的配置键；本次 Fabric 构建通过编辑配置文件调整功能，建议退出游戏后编辑。`interface.buttonPanel = false` 恢复官方轮盘，其余修复可独立开启。

## 验证范围

两版均使用对应的官方 YSM 2.6.5 JAR 与 Minecraft 类文件进行测试：真实 Sponge Mixin 注入、字节码操作数验证、反射接口检查、实际锁定回调、实际滑块运算/输入、实际模型表达式解析及配置读写。分别使用 Loader 0.15.0 / 0.16.0 的 API 编译，并检查它们配套的 Mixin 0.8.5 / 0.8.7 和 ASM 9.6 注入环境。面板导航与输入状态使用隔离回归测试。

另已在两个独立实例中启动完整客户端，日志确认两版都成功进入单人世界并加载配置。使用的 Fabric API 分别为 0.90.7+1.20.1 与 0.102.0+1.21.1。尚未完成全部面板操作、整合包、联机和第三方键位模组组合验证；修饰键回归测试模拟提供方的状态过滤，不代表已运行所有 ModernKeyBinding / Amecs 版本。

本地首次启动曾遇到 Fabric API 的 Mixin 配置资源读取失败。检查原包和生成缓存后，重启均恢复，补丁 JAR 未改动；根因尚未确认，不将其列为本补丁已修复的问题。详细结果见 [验证记录](VERIFICATION.txt)。

## 构建与测试

在仓库根目录使用 JDK 21：

```powershell
.\gradlew.bat -p fabric assemble
```

产物分别在 `fabric/mc1201/build/libs/` 与 `fabric/mc1211/build/libs/`。两个包共用 `src/main/java` 中的核心代码和 `fabric/common` 中的 Fabric 钩子；各版本的反射映射表独立。构建面向 Fabric 的生产环境 intermediary 命名空间，不使用 Loom 的 named 开发环境。

完整检查需要 Python 3 下载只读测试输入（约 180 MB，放在被 Git 忽略的 `build/fabric-fixtures`）：

```powershell
python tools/prepare_fabric_fixtures.py
.\gradlew.bat -p fabric build
python tools/verify_fabric_artifacts.py
```

准备脚本验证官方 YSM 的 SHA-512 与 Minecraft 下载的 SHA-1，并提取 YSM 自带的配置 API 作为测试依赖。测试不修改用户的游戏实例、模型或存档。成品仅包含本补丁代码和精简的名称映射，不包含 Minecraft / YSM 类、原生库或测试替身。
