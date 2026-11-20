# YSM 体验优化 — Fabric 1.2.0

[English](README.en.md)

在客户端安装与 Minecraft 版本匹配的增强包，再选择表中的一种模型模组。官方 YSM 和 OpenYSM 共用模组 ID `yes_steve_model`。

| Minecraft | 模型模组 | 安装包 | Java | 最低 Fabric Loader |
|---|---|---|---|---|
| 1.20.1 | 官方 YSM 2.6.5 | `ysm-enhancements-1.2.0-fabric-mc1.20.1.jar` | 17 | 0.15.0 |
| 1.20.1 | OpenYSM 2.6.6.6 | `ysm-enhancements-1.2.0-fabric-mc1.20.1.jar` | 17 | 0.16.10 |
| 1.21.1 | 官方 YSM 2.6.5 | `ysm-enhancements-1.2.0-fabric-mc1.21.1.jar` | 21 | 0.16.0 |

1.2.0 新增 OpenYSM 的 Z 面板、模型菜单、参数和鼠标快捷键适配。关闭面板会恢复 OpenYSM 设置中的原生轮盘。搭配东方女仆 Fabric 移植版 Orihime，官方 YSM 的两个版本都能通过女仆原生界面换肤。OpenYSM 2.6.6.6 Fabric 的玩家选模页可用，女仆换肤仍缺少上游联动。[OpenYSM Fabric 发布包](https://modrinth.com/mod/openysm/version/8Yg50ABK) · [上游女仆联动](https://github.com/OpenYSM/OpenYSM/issues/72)

## 相对官方 YSM 2.6.5 的新增功能

- **Z 按钮面板**：可替换官方 Z 轮盘，读取当前模型作者定义的菜单、分类、参数和动作。按可用空间自动排布，宽屏最多三列，放得下时不分页；参数多时支持滚动与滚动条拖动。
- **模型预览操作**：支持拖动旋转整个模型、滚轮及 ± 按钮缩放，点击百分比复位预览视角和缩放；预览带接地阴影。
- **参数输入与键盘操作**：滑条支持直接输入数值，原值预先全选，清理冗长浮点尾数，无效输入标红；Enter 确认、Esc 取消。支持 Tab、方向键和 PageUp／PageDown，焦点会自动滚动到可视区域。数值输入和左右方向键只触发一次模型回调，面板操作不会误触被隐藏的原轮盘控件。
- **面板导航与状态显示**：深色半透明面板、蓝色强调色、滑动开关和悬停过渡动画。标题显示完整目录路径，以 `›`、`‹` 标识子目录和返回入口；从子目录返回时保留原分页，根目录返回按钮不再意外关闭面板；选项组和键盘焦点有独立状态提示。
- **独立功能配置**：按钮面板与各项修复默认开启，可在配置文件中分别控制；关闭按钮面板即可使用官方轮盘，其他修复继续独立生效。
- **错误回退与开销优化**：面板绘制或输入处理出错时记录错误并退回原轮盘；缓存反射查询、菜单文字和文字截断结果，降低每帧开销。

## 修复的官方问题

以下 9 项修复以官方 YSM 2.6.5 Fabric 为对比基准：

1. **按键状态残留**：切换窗口、打开背包等界面、释放鼠标捕获或松开按键后，清理 YSM 中残留的按住状态，避免 Ctrl 等按键继续生效。
2. **动作锁定快捷键误触发**：每次物理按下只切换一次锁定，阻止长按重复事件、按键状态重同步、窗口失焦及 GUI 中的误触发，并遵循快捷键提供方的修饰键状态。支持键盘、扫描码和鼠标绑定。
3. **隐藏控件仍可点击**：阻止官方轮盘滚动区域外的隐藏模型控件响应点击。
4. **滚动箭头状态错误**：滚动或异步加载控件后，及时更新上下箭头是否可用。
5. **科学计数法读取错误**：正确读取 `1e-5` 等格式的模型变量。
6. **缩放滑条首次显示错误**：当零初值超出滑条可选范围时，根据模型缩放表达式中明确的回退常数初始化显示。例如 `v.roaming.player_size==0?1:v.roaming.player_size` 的首次显示为 1。兼容官方轮盘和按钮面板；打开界面时不修改模型变量或发送同步。已有非零值、可选的 0、复杂条件及冲突规则保留原逻辑。
7. **负数开关状态错误**：将负数且非零的模型变量正确显示为已勾选。
8. **搜索光标和选区丢失**：模型搜索列表重建时，保留光标、选区及文本显示位置。
9. **延迟回调造成控件叠加**：丢弃旧配置页或已关闭界面的异步控件回调，避免旧控件叠到当前界面。

## 与 Forge / NeoForge 版的适配差异

| 项目 | Forge / NeoForge 1.2.0 | Fabric 1.2.0 |
|---|---|---|
| 核心界面 | 模型驱动的 Z 按钮面板 | 同一套面板功能，分别适配两版 Fabric 接口 |
| 官方 YSM 修复项 | 10 项 | 上述 9 项，官方 YSM Fabric 自带鼠标快捷键 |
| 动作锁定 | 修饰键与操作环境检查 | 针对 Fabric 回调额外处理长按重复与状态重同步 |
| 配置入口 | Mods 内的配置界面 | 编辑 `config/ysm_enhancements-client.toml`，当前未提供游戏内配置界面 |

官方 YSM Fabric 使用自带的鼠标绑定。搭配 OpenYSM 时，可以用 `mouseShortcuts` 配置增强包提供的鼠标快捷键转发。

## 安装与配置

保留上表中匹配的模型模组和它要求的依赖，将对应的增强包 JAR 放入客户端实例的 `mods` 文件夹。升级时先移除旧版增强包。

表中的最低 Loader 版本已计入模型模组自带的依赖。另行安装的 Fabric API 或整合包可能要求更高版本。

配置通过 YSM 已依赖的 Forge Config API Port 注册，保存在 `config/ysm_enhancements-client.toml`。两个版本使用相同的配置键；本次 Fabric 构建通过编辑配置文件调整功能，建议退出游戏后编辑。`interface.buttonPanel = false` 恢复官方轮盘，其余修复可独立开启。

## 验证范围

两个游戏版本都通过了官方 YSM 的实际类文件、Mixin、输入、滑条、模型表达式和配置读写回归测试。OpenYSM 2.6.6.6 通过了独立的接口、滑条和模型表达式检查。

本地客户端实测中，官方 YSM 的女仆皮肤在返回世界后仍可见，并能从存档读回对应模型 ID。OpenYSM 通过了玩家模型搜索、Z 面板、模型参数开关、鼠标侧键和恢复原生轮盘测试。[验证记录](VERIFICATION.txt)

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
