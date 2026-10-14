<p align="center"><img src="app/src/main/res/artwork/C1.jpg" width="160" alt="logo"></p>

# KOReader 锁屏显示

一个极简的 Android 锁屏工具，专为墨水屏/阅读器设备上的 [KOReader](https://github.com/koreader/koreader) 设计，只做两件事：

1. **锁屏显示**：息屏时在锁屏上全屏显示一张图片（默认是 KOReader 生成的屏保图）。
2. **解锁打开 KOReader**：解锁后自动关闭锁屏页并拉起 KOReader。

通过前台服务常驻运行，无需 MacroDroid 等自动化工具。

## 工作原理

- `LockService` 是一个前台服务，动态注册 `SCREEN_OFF` / `USER_PRESENT` 广播：
  - 息屏时启动 `LockScreenActivity`（`showWhenLocked`），全屏显示图片；
  - 解锁后关闭锁屏页并启动 KOReader。
- 点击锁屏图片会调用 `requestDismissKeyguard` 进入系统解锁流程；返回键已屏蔽。
- 图片仅在路径、修改时间或大小变化时重新解码，避免亮屏闪烁。
- `BootReceiver` 开机后自动恢复服务。

## 系统要求

- Android 8.1 及以上（minSdk 27，targetSdk 36）
- 已安装 KOReader（默认包名 `org.koreader.launcher`）

## 使用

1. 安装后打开「锁屏显示」，授予存储权限和通知权限。
2. 设置**图片路径**，默认：`/storage/emulated/0/koreader/.moon/screensaver/compose.png`
3. 设置**解锁后启动的包名**，默认：`org.koreader.launcher`
4. 点击 **保存并启动服务**。可用 **预览锁屏图片** 检查效果，**停止服务** 关闭功能。

## 权限与系统设置

| 权限 / 设置 | 用途 |
| --- | --- |
| 存储权限 | 读取锁屏图片（Android 10 使用 `requestLegacyExternalStorage`） |
| 前台服务 / 通知 | 保持服务常驻 |
| 开机自启 | 开机后恢复服务 |
| 悬浮窗 / 后台弹出界面 | 息屏时从后台拉起锁屏页 |

**MIUI 等国产系统**还需要手动开启：

- 「锁屏显示」权限和「后台弹出界面」权限（也可以用 adb 设置）：
  ```bash
  adb shell appops set net.ankio.koreader 10021 allow   # 锁屏显示
  adb shell appops set net.ankio.koreader 10020 allow   # 后台弹出界面
  ```
- 自启动：允许
- 省电策略：无限制

## 构建

```bash
./gradlew assembleDebug
```

需要 JDK 17+（可以直接用 Android Studio 自带的 JBR）。项目已配置阿里云 Maven 镜像。

## 致谢

本项目基于 [sakarie9/WallSetter](https://github.com/sakarie9/WallSetter) 改写。

## 许可证

[GPL-3.0](LICENSE)
