# GKSchedule

广东科技学院（GDUST）第三方课程表 Android 应用。使用 Kotlin、Jetpack Compose 和 Material 3 构建，支持从教务系统同步课程、手动管理课程、查看考试与接收课程通知。

> 本应用为学生个人开发的第三方工具，与广东科技学院无官方关联。

## 能做什么

### 查看课表

- 今日页：查看当天和明日课程、当前课程进度、下一节课程及近期考试。
- 周课表：左右滑动切换周次，支持周次选择、回到当前周和无课周隐藏。
- 课程块支持连续节次合并、详细分割、单双周/自定义周次和自定义时间。
- 课程冲突会在课表中保留并进行颜色区分。
- 支持课表截图；截图时悬浮底栏会自动隐藏，完成后恢复。

### 签到联动

- 与[随地大小签（ChaoxingSignFaker）](https://github.com/aquamarine5/ChaoxingSignFaker)联动：在设置的"签到联动"子页开启开关后，今日页 / 课表页的课程详情会出现"去签到"按钮，一键跳转到随地大小签对应课程的签到页，完成学习通签到。
- 支持"获取课程ID"：静默读取随地大小签已登录账号的课程列表，按课程名自动匹配并回填 classId / courseId / fid，完成后提示获取与匹配的课程门数。
- 子页展示上次获取结果（持久保存）与已匹配课程的 ID 列表，同名课程只显示一条。
- 总开关与课表页子开关默认关闭，开启后才显示按钮和设置项。
- 未配置 ID、未安装随地大小签或跳转失败时均有明确提示；也可在课程编辑页手动填写超星 ID 作为兜底。
- 配套集成已提交至上游：[aquamarine5/ChaoxingSignFaker#220](https://github.com/aquamarine5/ChaoxingSignFaker/pull/220)。

### 管理课程

- 从教务系统同步课程。
- 手动添加、编辑、删除和隐藏课程。
- 支持教师、教室、备注、单双周/自定义周次和自定义上下课时间。
- 手动编辑过的课程在后续同步时不会被覆盖。
- 课程编辑页支持复制提示词到外部 AI 工具，再将解析结果导入课程。

### 调休日期映射

在设置中的“调休模式”页面添加两个具体日期的映射规则：

- 启用后，两个日期的课程在显示层互换。
- 原始课程数据不会被修改。
- 课表页、今日页、明日课程和课程通知都会使用互换后的结果。
- 每条规则可以单独启用、停用或删除。
- 停用规则后，课表和通知恢复原课程显示。

### 登录与同步

- 教务账号密码登录，支持验证码。
- 支持钉钉扫码登录；二维码为一次性登录方式，过期后需要重新扫码。
- 已保存凭据使用加密存储。
- 支持快速重新登录和 Token 心跳保活。
- 支持启动时自动同步，以及按分钟、小时或天执行定时同步。

### 考试安排

- 从教务系统获取考试安排并缓存。
- 手动添加和编辑考试。
- 可选择是否在课表中显示考试。
- 支持考试方式组合，例如闭卷、开卷、半开卷和机考。
- 今日页显示当天及指定范围内的近期考试。
- 考试编辑页支持复制提示词到外部 AI 工具，再将解析结果导入考试。

### 通知

- 课程提醒：可设置提前 5、10、15 或 30 分钟提醒。
- 课程进度 Live Update：显示上课进度，并在课程结束时结束通知。
- 倒计时模式：课前显示倒计时，并自动衔接课程进度。
- 考试进度通知可单独开启。
- 调休映射会同步作用于课程提醒、倒计时、Live Update、午夜重排和开机恢复提醒。
- Android 13 及以上需要授予通知权限；精确提醒可能需要系统允许精确闹钟。

### 数据管理

- 导出和导入 JSON 课程数据。
- 兼容 SchedU 的裸数组 JSON 格式以及应用自己的包装格式。
- 导出 ICS 日历文件。
- 调休规则和界面设置保存在本机 DataStore 中；课程和考试保存在本机 Room 数据库中。

### 外观与交互

- 浅色、深色和跟随系统主题。
- 中文和英文界面。
- 今日页或课表页作为启动页。
- 多种课程颜色引擎和同科不同教室颜色区分。
- 可配置课表格子高度、圆角、间距、时间标签和课程名显示自适应（默认开启，动态收窄时间列保证课程块每行容纳 3 个字）。
- 支持应用内毛玻璃效果、悬浮底栏折叠和系统预测返回手势。
- 使用 Material 3 主题色，并适配系统明暗模式。

## 快速开始

### 使用 APK

安装 Release APK 后：

1. 打开应用并同意免责声明。
2. 在“我的”页面登录教务系统。
3. 等待课程同步完成。
4. 在设置中按需开启考试显示、通知、自动同步或调休映射。

扫码登录只适合临时一次性登录。需要长期使用时，建议使用账号密码登录，以便保存加密凭据并支持快速重新登录。

### 从源码构建

项目默认使用 `source/` 下的本地工具链。环境要求：

- JDK 17
- Android SDK，`compileSdk 37`、`targetSdk 36`、`minSdk 31`
- Gradle Wrapper 8.13

首次拉取或清理 `source/` 后：

```bash
./scripts/provision-source-toolchain.sh
```

构建 Debug：

```bash
./build.sh debug
```

构建 Release，并在有连接设备时自动安装和校验 APK：

```bash
./build.sh release
```

只执行 Gradle 构建：

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Release APK 输出路径：

```text
app/build/outputs/apk/release/app-release.apk
```

## 技术栈

| 组件 | 用途 |
| --- | --- |
| Kotlin 2.3.21 | 应用开发语言 |
| Jetpack Compose | UI 框架 |
| Material 3 | 主题、控件和动态颜色 |
| Room 2.8.5 | 课程与考试本地数据库 |
| DataStore 1.2.1 | 设置和调休规则持久化 |
| Navigation 2.10.1 | 页面导航 |
| OkHttp 5.5.0 | 教务系统网络请求 |
| WorkManager 2.11.2 | 定时同步任务 |
| AlarmManager | 课程、考试和进度通知调度 |
| Kotlin Serialization | JSON 导入导出 |
| ZXing | 登录二维码生成 |
| miuix-blur | 应用内背景模糊效果 |

## 项目结构

```text
app/src/main/java/com/ty/gkschedule/
├── api/                 教务系统接口与课程导入
├── data/                Room、DataStore、课程模型和课表解析
├── notification/        提醒、Live Update、开机恢复
├── sync/                自动同步和 Token 心跳
├── ui/about/            我的页面与关于信息
├── ui/course/           课程管理与编辑
├── ui/exam/             考试安排与编辑
├── ui/login/            账号登录与钉钉扫码登录
├── ui/manage/           课程列表管理
├── ui/settings/         设置及独立设置子页
├── ui/today/            今日页
├── ui/weekly/           周课表
└── util/                颜色、导入导出、图片和触感反馈
```

关键调休文件：

- `data/ScheduleAdjustment.kt`：映射规则模型与 DataStore 编解码。
- `data/ScheduleResolver.kt`：课表、今日页和通知共用的日期互换解析。
- `notification/ReminderScheduler.kt`：课程提醒和 Live Update 调度。
- `ui/settings/SubSettingsActivities.kt`：独立调休设置活动页。

## 免责声明

本应用为学生个人开发的第三方课程表工具，与广东科技学院无任何官方关联。请在使用通知、自动同步和登录功能前确认系统权限及账号信息正确。

## 鸣谢

- Jetpack Compose 与 Material Design 3
- aquamarine5 的 [ChaoxingSignFaker（随地大小签）](https://github.com/aquamarine5/ChaoxingSignFaker)，签到联动跳转与课程数据支持
- SchedU、拾光课程表、TimeFlow 等参考项目
- 参与测试和反馈的同学

## 许可证

MIT License
