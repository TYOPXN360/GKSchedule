# ClassApp 课程表

广东科技学院 (GDUST) 专属课程表 App。

## 功能

### 课表
- 周课表网格（左右滑动切换周次）
- 合并/分割/详细分割三种显示模式
- 冲突课程混色显示
- 4 种颜色引擎（Monet 动态 / 鲜艳 / 经典 / HSL 旋转）
- 下拉刷新、回到当前周悬浮按钮
- FAB 折叠/展开、课表截图

### 教务登录
- 统一身份认证（密码登录 + 扫码登录）
- Token 心跳保活（15 分钟自动刷新，防止过期）
- 快速重新登录（仅需验证码）

### 今日页
- 今日课程进度波浪动画（从 0 加载到实际百分比）
- 多课程按进度高低交错动画
- 近期考试显示

### 课程管理
- 全局课程管理、编辑、删除
- 支持手动添加课程和自定义时间
- 手动编辑过的课程同步时不会被覆盖

### 考试安排
- 从教务系统获取考试信息
- 自动匹配当前学年学期
- 学年/学期手动选择
- 考试显示在课表网格中（红色色块 + "考"标签）
- 今日页面显示近期考试
- 考试周不被"隐藏无课周"跳过

### 数据管理
- JSON 导入/导出（兼容 SchedU 格式）
- ICS 日历导出
- 考试数据缓存

### 个性化
- 深色/浅色/跟随系统
- 中文/英文
- 同科不同地点颜色区分
- 课程提醒（5/10/15/30 分钟）
- 隐藏无课周

### 自动同步
- 启动时自动同步
- 定时同步（分钟/小时/天可选）
- Token 心跳保活（15 分钟间隔，低功耗）

## 技术栈

| 组件 | 版本 |
|------|------|
| Kotlin | 2.0.21 |
| Jetpack Compose BOM | 2026.06.00 |
| Material 3 | BOM 内置 |
| Room | 2.6.1 |
| DataStore | 1.1.1 |
| Navigation | 2.8.5 |
| OkHttp | 4.12.0 |
| WorkManager | 2.9.1 |

## 开发环境

- JDK 17
- Android SDK（compileSdk 36，minSdk 31）
- Gradle 9.3.1

```bash
export JAVA_HOME="$PWD/source/jdk-17"
export ANDROID_HOME="$PWD/source/android-sdk"
export ANDROID_USER_HOME="$PWD/.android"
export GRADLE_USER_HOME="$PWD/.gradle"
./gradlew assembleDebug
```

## 架构

```
app/src/main/java/com/classapp/schedule/
├── MainActivity.kt              # 主 Activity
├── ScheduleApp.kt               # 导航框架
├── ScheduleViewModel.kt         # ViewModel 状态管理
├── api/
│   ├── GdustApi.kt              # 教务系统 API 客户端
│   └── CourseImporter.kt        # API 数据转换
├── data/
│   ├── Course.kt                # Room 实体
│   ├── CourseDao.kt             # Room DAO
│   ├── CourseDatabase.kt        # Room 数据库
│   ├── CredentialStore.kt       # AES-256-GCM 加密凭据存储
│   └── SettingsDataStore.kt     # DataStore 设置
├── notification/
│   ├── ReminderReceiver.kt      # 课程提醒
│   └── ReminderScheduler.kt     # AlarmManager 调度
├── sync/
│   ├── AutoSyncWorker.kt        # 定时同步
│   └── HeartbeatWorker.kt       # Token 心跳保活
├── ui/
│   ├── about/AboutScreen.kt     # 我的页面
│   ├── course/CourseEditScreen.kt # 课程编辑
│   ├── exam/ExamScreen.kt       # 考试安排
│   ├── login/LoginScreen.kt     # 教务登录
│   ├── manage/CourseManageScreen.kt # 课程管理
│   ├── settings/SettingsScreen.kt # 设置
│   ├── today/TodayScreen.kt     # 今日课程
│   ├── weekly/WeeklyScheduleScreen.kt # 周课表
│   └── theme/                   # Material 3 主题
└── util/
    ├── CourseColors.kt          # 颜色引擎
    ├── HapticFeedback.kt        # 震动反馈
    └── JsonImportExport.kt      # JSON/ICS 导入导出
```
