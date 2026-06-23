> **⚠️ 此仓库已停止单独维护,代码已并入 monorepo [eret9616/pgz110_root](https://github.com/eret9616/pgz110_root),位于 `HideOngoing/` 目录。** 后续更新请到那边;本仓库已归档为只读。

# HideOngoing

隐藏 Android "无法清除" 的常驻通知。基于 `NotificationListenerService`，纯事件驱动，零轮询。

## 原理

| 通知类型 | 处理 |
|---|---|
| `isClearable == true` | `cancelNotification(key)` |
| `isClearable == false`（含前台服务/ongoing） | `snoozeNotification(key, 1 年)` |

按 `(packageName, channelId)` 存规则。重启后 SystemUI 的 snooze 队列清空，靠 `onListenerConnected` 重连那一发把当前活跃通知重新 enforce 一遍。

## 用法

1. 用 Android Studio 打开本目录（File → Open）。
2. 第一次 Sync 会自动下载 Gradle 8.7 + Android SDK 34。
3. 把手机用 USB 或 ADB over Wi-Fi 连上，点 Run（▶）。
4. 在 App 里点"打开通知使用权设置"，给 HideOngoing 打钩。
5. 回到 App，列表里会列出所有当前的常驻通知，每条旁边两个按钮：
   - **屏蔽 channel**：只屏蔽这个 app 的这一个通知 channel
   - **屏蔽 app**：屏蔽这个 app 的所有 ongoing 通知

## 命令行打 APK（不用 AS Run 按钮）

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 关键文件

- `app/src/main/java/dev/local/hideongoing/NotificationListener.kt` — 全部业务逻辑（~60 行）
- `app/src/main/java/dev/local/hideongoing/RuleStore.kt` — SharedPreferences 持久化
- `app/src/main/java/dev/local/hideongoing/MainActivity.kt` — Compose UI

## License

随便用。
