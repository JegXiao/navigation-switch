# 导航切换 | Navigation Switch

> 一键在三键导航和手势导航之间切换，支持桌面小组件和快捷设置磁贴。

**中文** · [ English](#english)

---

## 兼容性（先看这里）

| 设备 / 系统 | 状态 | 说明 |
|---|---|---|
| 荣耀（MagicOS） | **已实机验证** | 荣耀 DV-AN00（Android 16 / MagicOS 10）验证通过；需要 Shizuku |
| 华为（EMUI / HarmonyOS，兼容 Android 应用） | 代码按同一路径处理，**未实机验证** | 需要 Shizuku；取值或行为可能与荣耀不同 |
| HarmonyOS NEXT（纯血鸿蒙） | **不适用** | 不运行 Android APK，本项目无法安装 |
| AOSP / Pixel / Android One | 按 AOSP 机制实现，**未在 Pixel 实机验证** | 通过 `WRITE_SECURE_SETTINGS` 写 `navigation_mode`；只校验设置写入，不校验导航栏实际变化 |
| 小米 / Redmi（MIUI / HyperOS） | 未适配 | 可能使用私有设置（例如 `force_fsg_nav_bar`）或私有服务，不保证有效 |
| 三星 One UI | 未适配 | 可能忽略 AOSP 设置 |
| OPPO / realme / vivo / 一加 | 未适配 | 可能使用私有设置或服务 |
| Android 10 以下 | 不支持手势 | 手势导航从 Android 10（API 29）开始提供；App 会提示系统不支持，无法切换为手势 |
| 平板 / 折叠屏 / 桌面模式 | 未适配 | 导航栏形态、取值和 overlay 可能不同 |
| 多用户 / 平行空间 / 分身应用 | 需分别授权 | 设置和 Shizuku 授权按用户隔离 |
| 企业 MDM / 定制 ROM | 可能被策略锁定 | 系统可能忽略或禁止修改 |

> 本项目只在荣耀 DV-AN00 上做过完整实机验证。其他品牌请先按上表判断，不要默认“装上就能用”。

## 功能特性

- 三键导航 ⇄ 手势导航一键切换
- 1×1 桌面小组件，点按即切换
- 快捷设置磁贴，下拉控制中心即可切换
- 自动识别荣耀 / 华为，使用 Shizuku 执行系统命令
- AOSP 类设备通过 adb 授权 `WRITE_SECURE_SETTINGS`（其他厂商 ROM 不保证）
- 无网络权限、无统计、无广告、无常驻后台服务

## 快速开始

### 安装 APK

从 [Releases](https://github.com/JegXiao/navigation-switch/releases/latest) 下载最新的 `navigation-switch-<版本>.apk` 并安装。首次安装需允许浏览器或文件管理器安装未知来源应用。

> 从 v1.2 起，Release 使用固定证书签名，后续版本可以直接覆盖安装。v1.0 / v1.1 的 CI debug 包签名不同，升级到 v1.2 需要先卸载一次；卸载会清除 adb 授权、Shizuku 授权和桌面小组件。

Release APK 签名证书 SHA-256：

```text
31:96:FF:52:57:7B:B2:4B:F8:37:13:D8:6D:45:81:6B:BC:B5:46:12:C0:63:7F:2C:9F:59:C1:8C:C5:A8:FB:EB
```

安装后需要完成一次授权，见下一节。

### 从源码编译

```bash
git clone git@github.com:JegXiao/navigation-switch.git
cd navigation-switch
./gradlew assembleDebug
```

生成的 APK 在 `app/build/outputs/apk/debug/app-debug.apk`。

也可以用 Android Studio：打开项目 → 等待 Gradle Sync → 连接手机 → Run。

编译环境（实测）：

- Android Studio / Gradle Wrapper 9.6.0
- AGP 9.5.1
- JDK 21（CI）/ JDK 25（本机）
- compileSdk 37，minSdk 24，targetSdk 37
- Android SDK Platform 37.0（CI 同时安装 37.1）

`local.properties` 由 Android Studio 自动生成（`sdk.dir`），已被 `.gitignore` 忽略；项目不需要任何 API Key。

> debug APK 使用本机 debug keystore 签名，不能覆盖 Release 包，反之亦然。两者切换时必须先卸载。

## 授权

权限只需要完成一次；卸载重装后需要重新做。

### A. AOSP / Pixel 等设备（理论路径）

该路径按 Android 10+ 的 AOSP 机制实现，**未在 Pixel 实机验证**。如果你的 ROM 忽略 `navigation_mode`，会表现为“提示成功但导航栏不变”。

1. 打开开发者选项：设置 → 关于手机 → 连续点击“版本号”7 次。
2. 开发者选项 → 打开“USB 调试”。
3. 用数据线连接电脑，在手机上允许 USB 调试授权。
4. 确认设备连接：

   ```bash
   adb devices
   ```

   应看到状态为 `device`，而不是 `unauthorized` 或 `offline`。
5. 授予权限：

   ```bash
   adb shell pm grant me.jegxiao.navigationswitch android.permission.WRITE_SECURE_SETTINGS
   ```

6. 打开 App，授权卡片应消失；也可以点“重新检查”。
7. 撤销（可选）：

   ```bash
   adb shell pm revoke me.jegxiao.navigationswitch android.permission.WRITE_SECURE_SETTINGS
   ```

说明：

- `WRITE_SECURE_SETTINGS` 是受保护权限，安装界面无法授予，只能通过 adb 或 root。
- 它允许修改系统安全设置，请只在自己的、信任的设备上授权。
- 如果 `pm grant` 报 `Unknown package`，先安装 APK。
- 小米等系统可能要求额外的“USB 调试（安全设置）”和账号登录；本项目未适配这些限制。

### B. 荣耀 / 华为设备（Shizuku）

这两类系统的 SystemUI 不监听 AOSP 的 `navigation_mode`。系统设置页是通过 signature 权限直接开关导航栏 overlay 的，普通 App 拿不到这个权限，所以需要 Shizuku 提供 shell 身份。

#### 1. 安装 Shizuku

- 官网：https://shizuku.rikka.app/
- GitHub Releases：https://github.com/RikkaApps/Shizuku/releases
- 也可以通过应用商店、酷安等渠道安装。

安装后打开 Shizuku，按 App 内引导启动服务。

#### 2. 启动 Shizuku

**有 root：**

1. Shizuku 首页选择“通过 root 启动”；
2. 可以在 Shizuku 设置里开启“开机自启”，避免每次重启手动启动。

**没有 root（Android 11+ 推荐无线调试）：**

1. 打开开发者选项 → 无线调试；
2. 在 Shizuku 里选择“通过无线调试启动”；
3. 按 Shizuku 提示输入配对码并配对；
4. 配对成功后启动服务；
5. 看到 Shizuku 首页显示服务正在运行即可。

Android 10 或更早的设备，按 Shizuku 内的 ADB 启动引导操作。

> 关闭无线调试通常不会立刻停止已经启动的 Shizuku 服务；但重启手机、强制停止 Shizuku、系统清理后台后，服务会停止，需要重新启动。

#### 3. 给本 App 授权

1. 打开“导航切换”；
2. 页面出现“需要 Shizuku”卡片；
3. 状态显示“Shizuku 已运行，尚未授权”时，点“授予权限”；
4. 在 Shizuku 的授权弹窗里允许；
5. 状态变成“已授权，可以一键切换”即可。

如果显示“Shizuku 未运行”：

- 先启动 Shizuku；
- 如果还没安装 Shizuku，点“打开 Shizuku”会跳转官网。

#### 4. 验证

- 点主按钮“切换为三键导航 / 手势导航”；
- 底部导航栏应立即变化；
- 再点一次应切回；
- 桌面小组件和快捷磁贴应在授权后同样可用。

#### 5. 重启手机 / 重装 App 后

- **重启手机**：Shizuku 服务停止，需要重新启动；App 的 Shizuku 授权通常保留，启动服务后即可继续用。
- **卸载重装 App**：adb 授权、Shizuku 授权、桌面小组件都会丢失，需要重新授权/添加。
- **清除 Shizuku 数据 / 在 Shizuku 里撤销授权**：需要重新授予。
- **平行空间 / 分身 / 多用户**：每个空间独立安装、独立授权。

#### 6. 不想用 Shizuku

荣耀 / 华为上如果不启动 Shizuku，App 无法修改导航方式，只能手动进系统设置切换。其他品牌如果忽略 AOSP 设置，也属于“本 App 不支持”，不是安装失败。

## 使用

1. 打开 App，主界面显示当前导航方式；
2. 点主按钮在“三键导航”和“手势导航”之间切换；
3. 桌面长按空白处 → 小组件 → 搜索“导航切换” → 添加 1×1 小组件；
4. 下拉控制中心 → 编辑 → 添加“导航方式”磁贴；
5. 点击底部版本号可复制版本信息，方便反馈问题。

切换后系统界面会短暂重载，属于正常现象。

## 调试与排查

### 查看当前状态

```bash
adb shell settings get secure navigation_mode
adb shell settings get secure secure_gesture_navigation
adb shell cmd overlay list
```

- 荣耀 / 华为：在 `cmd overlay list` 里看 `com.android.internal.systemui.navbar.gestural`：
  - `[x]` = 手势 overlay 开启；
  - `[ ]` = 关闭（三键）。
- `navigation_mode`：AOSP 是 `0/1/2`；荣耀 / 华为是 `101/105`。
- Windows PowerShell 没有 `grep`，可以直接看输出，或用 `Select-String`。

### 查看授权状态

```bash
adb shell dumpsys package me.jegxiao.navigationswitch
```

在输出里搜索 `WRITE_SECURE_SETTINGS`，应看到 granted。

### 查看日志

```bash
adb logcat
```

也可以使用 Android Studio 的 Logcat，筛选包名 `me.jegxiao.navigationswitch`。App 主要通过界面状态和 Toast 提示错误。

### 校验 APK 签名

```bash
apksigner verify --print-certs navigation-switch-<版本>.apk
```

证书 SHA-256 应与 README 里的指纹一致。

### 常见问题

| 现象 | 可能原因 | 处理 |
|---|---|---|
| 授权卡片一直不消失 | 命令包名不对 / USB 调试未授权 / 设备未连接 | `adb devices` 确认 `device`；重新执行 grant；点“重新检查” |
| `Unknown package` | APK 尚未安装 | 先安装 APK，再执行 grant |
| “Shizuku 未运行” | Shizuku 服务未启动 | 启动 Shizuku；root 可开启开机自启 |
| “Shizuku 已运行，尚未授权” | 未在 Shizuku 里允许本 App | 点“授予权限”，在弹窗里允许 |
| 切换失败：系统未采用新的导航方式 | overlay 命令失败 / 权限变化 / ROM 不兼容 | 确认 Shizuku 已授权；用 `cmd overlay list` 检查；重试；仍失败则该 ROM 不支持 |
| 提示成功但导航栏没变 | 非荣耀/华为机型忽略了 `navigation_mode` | 当前版本不支持该机型，不要反复重试 |
| 组件/磁贴点按后打开 App | 还没授权 | 按 App 内卡片完成授权后再点 |
| 重启后组件/磁贴不能直接切换 | Shizuku 服务已停止 | 重新启动 Shizuku |
| 安装报签名不一致 | debug 包与 release 包证书不同 | 卸载旧包再装；会丢授权和小组件 |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 同上 | 同上 |

### 调试构建

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- debug APK 用本机 debug keystore 签名，不能覆盖 release 包，反之亦然。
- release 包由维护者通过 `RELEASE_KEYSTORE_PATH` 等环境变量注入签名；keystore 不入库。

## 工作原理

- AOSP 的导航方式存储在 `Settings.Secure.navigation_mode`：`0` 三键、`1` 二键、`2` 手势。
- 荣耀 / 华为使用 `101`（三键）/ `105`（手势），并配合 `secure_gesture_navigation`；实际切换通过 OverlayManager 开关 `com.android.internal.systemui.navbar.gestural`。
- App 用 Shizuku 提供的 shell 身份执行：

```bash
settings put secure navigation_mode 105
settings put secure secure_gesture_navigation 1
cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.gestural
```

切回三键时写入 `101` / `0`，并关闭 gestural overlay。

- 荣耀 / 华为路径会解析 `cmd overlay list`，校验 overlay 实际状态；AOSP 路径只校验设置值是否写入成功。
- 桌面小组件和快捷磁贴都调用同一个 `NavModeManager`，状态自动同步。

## 权限与隐私

- `WRITE_SECURE_SETTINGS`：Android 不允许普通应用申请，需要用 adb 手动授予；它可以修改系统安全设置，请只在信任的设备上使用。
- Shizuku：仅荣耀 / 华为设备需要，用于以 shell 身份执行固定的系统命令；Shizuku 是第三方应用，请从官方渠道安装并自行判断是否信任。
- App 没有声明 `INTERNET` 权限，不联网、不收集数据、没有统计和广告；打开 Shizuku 官网时通过系统浏览器跳转。
- Release APK 使用固定证书签名，证书 SHA-256 见上文；keystore 只保存在本地安全目录和 GitHub Secrets，不在仓库中。

## 已知限制

- **只在荣耀 DV-AN00（Android 16 / MagicOS 10）上完整实测**；华为、Pixel/AOSP、其他品牌均未实机验证。
- 荣耀 / 华为的导航取值（`101/105`）和 overlay 名称来自实测；如果厂商在新 ROM 中修改，路径可能失效。
- AOSP 路径只校验 `navigation_mode` 写入，不校验导航栏实际变化；其他厂商 ROM 可能提示成功但没切换。
- Android 10 以下没有手势导航；二键模式会被视为非手势模式。
- 平板、折叠屏、桌面模式、企业 MDM、多用户 / 平行空间 / 分身环境未做专门适配。
- 重新安装 App 后需要重新执行 adb 授权 / Shizuku 授权；桌面小组件需要重新添加。
- Shizuku 服务在重启手机后需要重新启动（root 模式可在 Shizuku 内开启开机自启）。
- 首次从 debug 包切换到固定签名 Release 包必须卸载一次，会丢失授权和小组件。
- 当前 Shizuku 适配基于 13.1.5 API 的 `newProcess`；Shizuku 未来大版本若移除该接口，荣耀 / 华为路径可能需要更新 App。
- 切换导航方式会让 SystemUI 短暂重载；部分 ROM 可能伴随闪屏或桌面重载。

## 项目结构

```text
app/src/main/java/me/jegxiao/navigationswitch/
├── MainActivity.kt              # 主界面、授权引导
├── NavModeManager.kt            # 读取/切换导航方式（AOSP + 荣耀/华为）
├── ShizukuHelper.kt             # Shizuku 权限与 shell 执行
├── NavigationWidgetProvider.kt  # 1×1 桌面小组件
├── NavTileService.kt            # 快捷设置磁贴
└── RootShell.kt                 # 可选：用 root 授予 WRITE_SECURE_SETTINGS

app/src/main/java/rikka/shizuku/ShizukuAccess.java
                                  # 兼容 Shizuku 13.1.5 的非公开 newProcess
```

---

# English

> One-tap switch between 3-button and gesture navigation, with a home screen widget and a Quick Settings tile.

## Compatibility (read this first)

| Device / ROM | Status | Notes |
|---|---|---|
| Honor (MagicOS) | **Verified on a real device** | Tested on Honor DV-AN00 (Android 16 / MagicOS 10); requires Shizuku |
| Huawei (EMUI / HarmonyOS with Android app support) | Same code path, **not tested on a real device** | Requires Shizuku; values or behavior may differ from Honor |
| HarmonyOS NEXT | **Not applicable** | It does not run Android APKs, so this app cannot be installed |
| AOSP / Pixel / Android One | Implemented via AOSP mechanism, **not tested on a real Pixel** | Writes `navigation_mode` with `WRITE_SECURE_SETTINGS`; only verifies the setting, not the actual navigation bar |
| Xiaomi / Redmi (MIUI / HyperOS) | Not adapted | May use private settings such as `force_fsg_nav_bar` or private services |
| Samsung One UI | Not adapted | May ignore the AOSP setting |
| OPPO / realme / vivo / OnePlus | Not adapted | May use private settings or services |
| Android 10 or older | No gesture navigation | Gesture navigation starts with Android 10 (API 29); the app reports that it is unsupported and cannot switch to gesture |
| Tablets / foldables / desktop mode | Not adapted | Navigation bar shape, values and overlays may differ |
| Multi-user / Parallel Space / app clones | Separate authorization required | Settings and Shizuku grants are per user |
| Enterprise MDM / custom ROM | May be locked down | The system may ignore or forbid the change |

> The project has only been fully verified on an Honor DV-AN00. Check the table above before assuming another device will work.

## Features

- Toggle between 3-button and gesture navigation with one tap
- 1×1 home screen widget
- Quick Settings tile
- Detects Honor / Huawei and uses Shizuku to run the required system commands
- AOSP-like devices use `WRITE_SECURE_SETTINGS` granted over adb (other vendor ROMs are not guaranteed)
- No internet permission, no analytics, no ads, no persistent background service

## Quick Start

### Install the APK

Download the latest `navigation-switch-<version>.apk` from [Releases](https://github.com/JegXiao/navigation-switch/releases/latest). The first install requires allowing your browser or file manager to install apps from unknown sources.

> Starting with v1.2, release APKs use a fixed release certificate and can update each other. The v1.0 / v1.1 CI debug builds use a different signature; upgrading to v1.2 requires uninstalling once, which clears the adb grant, Shizuku authorization and widgets.

Release APK signing certificate SHA-256:

```text
31:96:FF:52:57:7B:B2:4B:F8:37:13:D8:6D:45:81:6B:BC:B5:46:12:C0:63:7F:2C:9F:59:C1:8C:C5:A8:FB:EB
```

Then grant the required permission once; see the next section.

### Build from source

```bash
git clone git@github.com:JegXiao/navigation-switch.git
cd navigation-switch
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

You can also open the project in Android Studio, wait for Gradle Sync, connect a device, and click Run.

Verified toolchain:

- Gradle Wrapper 9.6.0
- AGP 9.5.1
- JDK 21 (CI) / JDK 25 (local)
- compileSdk 37, minSdk 24, targetSdk 37
- Android SDK Platform 37.0 (CI also installs 37.1)

`local.properties` is generated by Android Studio and ignored by Git. No API keys are required.

> A debug APK is signed with the local debug keystore and cannot update a release build, or vice versa. Uninstall before switching between them.

## Authorization

Authorization is needed once. Reinstalling the app requires doing it again.

### A. AOSP / Pixel-like devices (theoretical path)

This path follows the Android 10+ AOSP mechanism and has **not been verified on a real Pixel**. If your ROM ignores `navigation_mode`, the app may report success while the navigation bar does not change.

1. Enable Developer options: Settings → About phone → tap “Build number” seven times.
2. Open Developer options → enable “USB debugging”.
3. Connect the phone over USB and allow the debugging authorization on the phone.
4. Confirm the connection:

   ```bash
   adb devices
   ```

   The state must be `device`, not `unauthorized` or `offline`.
5. Grant the permission:

   ```bash
   adb shell pm grant me.jegxiao.navigationswitch android.permission.WRITE_SECURE_SETTINGS
   ```

6. Open the app; the permission card should disappear. You can also tap “重新检查” (Recheck).
7. Revoke it if needed:

   ```bash
   adb shell pm revoke me.jegxiao.navigationswitch android.permission.WRITE_SECURE_SETTINGS
   ```

Notes:

- `WRITE_SECURE_SETTINGS` is a protected permission. It cannot be granted from the install UI; adb or root is required.
- It allows changing secure system settings. Grant it only on devices you trust.
- If `pm grant` reports `Unknown package`, install the APK first.
- Some Xiaomi ROMs additionally require “USB debugging (Security settings)” and an account login; this project does not adapt to those restrictions.

### B. Honor / Huawei devices (Shizuku)

Their SystemUI does not observe the AOSP `navigation_mode` value. The system Settings app toggles the navigation bar overlay with a signature-level permission that normal apps cannot obtain, so the app uses Shizuku for a shell identity.

#### 1. Install Shizuku

- Website: https://shizuku.rikka.app/
- GitHub Releases: https://github.com/RikkaApps/Shizuku/releases
- App stores or other trusted channels also work.

Open Shizuku and follow the in-app guide to start its service.

#### 2. Start Shizuku

**With root:**

1. Choose “Start via root” on the Shizuku home screen.
2. Enable “Start on boot” in Shizuku settings if you do not want to start it manually after every reboot.

**Without root (wireless debugging on Android 11+):**

1. Enable Developer options → Wireless debugging.
2. In Shizuku, choose “Start via wireless debugging”.
3. Enter the pairing code shown by Android when Shizuku asks.
4. Start the service after pairing.
5. The Shizuku home screen should show that the service is running.

On Android 10 or older, follow Shizuku's ADB start guide instead.

> Turning wireless debugging off usually does not immediately stop an already running Shizuku service. After a reboot, a force-stop of Shizuku, or system background cleanup, the service stops and must be started again.

#### 3. Authorize this app

1. Open Navigation Switch.
2. The “需要 Shizuku” (Shizuku required) card appears.
3. When the status says “Shizuku 已运行，尚未授权” (running, not authorized), tap “授予权限” (Grant permission).
4. Allow the request in Shizuku's dialog.
5. The status changes to “已授权，可以一键切换” (authorized).

If the status says “Shizuku 未运行” (not running):

- Start Shizuku first.
- If Shizuku is not installed, “打开 Shizuku” (Open Shizuku) opens the official website.

#### 4. Verify

- Tap the main button to switch between 3-button and gesture navigation.
- The navigation bar should change immediately.
- Tap again to switch back.
- The home screen widget and Quick Settings tile work after authorization as well.

#### 5. After a reboot or reinstall

- **Reboot:** the Shizuku service stops and must be started again. The app's Shizuku authorization is normally retained.
- **Reinstall the app:** the adb grant, Shizuku authorization and widgets are lost; grant them again and re-add widgets.
- **Clear Shizuku data / revoke the app in Shizuku:** grant permission again.
- **Parallel Space / app clones / multi-user:** each space needs its own installation and authorization.

#### 6. If you do not want to use Shizuku

On Honor / Huawei, the app cannot change the navigation mode without Shizuku; use the system Settings manually instead. On other brands that ignore the AOSP setting, the app is simply not supported; this is not an installation failure.

## Usage

1. Open the app to see the current navigation mode.
2. Tap the main button to switch between 3-button and gesture navigation.
3. Long-press the home screen → Widgets → search for “导航切换” → add the 1×1 widget.
4. Pull down the Quick Settings panel → edit → add the “导航方式” tile.
5. Tap the version text at the bottom to copy version info for bug reports.

SystemUI reloads briefly after switching. This is expected.

## Debugging & Troubleshooting

### Inspect the current state

```bash
adb shell settings get secure navigation_mode
adb shell settings get secure secure_gesture_navigation
adb shell cmd overlay list
```

- On Honor / Huawei, check `com.android.internal.systemui.navbar.gestural` in the overlay list:
  - `[x]` = gesture overlay enabled;
  - `[ ]` = disabled (3-button).
- `navigation_mode`: AOSP uses `0/1/2`; Honor / Huawei use `101/105`.
- Windows PowerShell has no `grep`; read the output directly or use `Select-String`.

### Check authorization

```bash
adb shell dumpsys package me.jegxiao.navigationswitch
```

Search the output for `WRITE_SECURE_SETTINGS`; it should be granted.

### Logs

```bash
adb logcat
```

Android Studio's Logcat works too; filter by `me.jegxiao.navigationswitch`. The app mainly reports errors through UI state and Toasts.

### Verify the APK signature

```bash
apksigner verify --print-certs navigation-switch-<version>.apk
```

The certificate SHA-256 must match the fingerprint in this README.

### Common problems

| Symptom | Likely cause | What to do |
|---|---|---|
| The permission card never disappears | Wrong package name / USB debugging not authorized / device not connected | Check `adb devices` shows `device`; rerun the grant; tap “重新检查” |
| `Unknown package` | The APK is not installed | Install the APK first, then grant |
| “Shizuku 未运行” | Shizuku service is not running | Start Shizuku; root users can enable start-on-boot |
| “Shizuku 已运行，尚未授权” | The app was not allowed in Shizuku | Tap “授予权限” and allow the dialog |
| “系统未采用新的导航方式” | Overlay command failed / permission changed / ROM incompatible | Confirm Shizuku is authorized; check `cmd overlay list`; retry; if it still fails, the ROM is not supported |
| Success toast but the navigation bar does not change | A non-Honor/Huawei ROM ignored `navigation_mode` | Not supported on that device; do not keep retrying |
| Widget/tile opens the app instead of switching | Not authorized yet | Complete authorization in the app first |
| Widget/tile stops switching after a reboot | The Shizuku service stopped | Start Shizuku again |
| Install reports a signature mismatch | Debug and release builds use different certificates | Uninstall the old build first; this clears grants and widgets |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Same as above | Same as above |

### Debug builds

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- A debug APK uses the local debug keystore and cannot update a release build, or vice versa.
- Release builds are signed by the maintainer through `RELEASE_KEYSTORE_PATH` and related environment variables; the keystore is never committed.

## How It Works

- AOSP stores the navigation mode in `Settings.Secure.navigation_mode`: `0` = 3-button, `1` = 2-button, `2` = gesture.
- Honor / Huawei use `101` (3-button) / `105` (gesture) plus `secure_gesture_navigation`, and apply the change through OverlayManager by toggling `com.android.internal.systemui.navbar.gestural`.
- With Shizuku, the app runs:

```bash
settings put secure navigation_mode 105
settings put secure secure_gesture_navigation 1
cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.gestural
```

Switching back to 3-button writes `101` / `0` and disables the gestural overlay.

- The Honor / Huawei path parses `cmd overlay list` to verify the actual overlay state; the AOSP path only verifies that the setting value was written.
- The widget and Quick Settings tile call the same `NavModeManager`, so their state stays in sync.

## Permissions & Privacy

- `WRITE_SECURE_SETTINGS` is not grantable to normal apps; it must be granted over adb. It can modify secure system settings, so use it only on devices you trust.
- Shizuku is only used on Honor / Huawei devices to execute a fixed set of shell commands. Shizuku is a third-party app; install it from official channels and decide whether you trust it.
- The app does not declare the `INTERNET` permission. It does not collect data and contains no analytics or ads. Opening the Shizuku website is delegated to the system browser.
- Release APKs use a fixed signing certificate; its SHA-256 is shown above. The keystore is stored only in a local secure directory and GitHub Secrets, never in this repository.

## Known Limitations

- **Only fully tested on Honor DV-AN00 (Android 16 / MagicOS 10)**. Huawei, Pixel/AOSP and other brands have not been tested on real devices.
- The Honor / Huawei values (`101/105`) and overlay names come from this device. If a vendor changes them in a new ROM, the path may stop working.
- The AOSP path verifies only that `navigation_mode` was written, not the actual navigation bar; vendor ROMs may report success without changing anything.
- Gesture navigation does not exist below Android 10. A 2-button mode is treated as non-gesture.
- Tablets, foldables, desktop modes, enterprise MDM, and multi-user / Parallel Space / app clone environments are not specifically adapted.
- Reinstalling the app requires granting adb / Shizuku permission again and re-adding widgets.
- The Shizuku service must be restarted after a reboot (root mode can enable start-on-boot inside Shizuku).
- Switching from an older debug build to the fixed-signature release requires uninstalling once, which clears grants and widgets.
- The current Shizuku integration uses the 13.1.5 API's `newProcess`. If a future major Shizuku release removes that interface, the Honor / Huawei path may require an app update.
- Changing the navigation mode reloads SystemUI briefly; some ROMs may also flash the screen or reload the launcher.

## Project Layout

```text
app/src/main/java/me/jegxiao/navigationswitch/
├── MainActivity.kt              # UI and permission setup
├── NavModeManager.kt            # Read/switch navigation mode (AOSP + Honor/Huawei)
├── ShizukuHelper.kt             # Shizuku permission and shell execution
├── NavigationWidgetProvider.kt  # 1×1 home screen widget
├── NavTileService.kt            # Quick Settings tile
└── RootShell.kt                 # Optional: root grant for WRITE_SECURE_SETTINGS

app/src/main/java/rikka/shizuku/ShizukuAccess.java
                                  # Bridges Shizuku 13.1.5's non-public newProcess
```