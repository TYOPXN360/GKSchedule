# ClassApp Agent Memory — 完整项目记忆

## 一、项目概述

**课程表 App** — 专为广东科技学院 (GDUST) 设计，使用 Kotlin + Jetpack Compose + Material 3 构建。

- **包名**: `com.classapp.schedule`
- **应用名**: 课程表 / Schedule
- **minSdk**: 31 (Android 12)
- **targetSdk**: 36 (Android 16)
- **compileSdk**: 36
- **语言**: Kotlin 2.0.21 + Jetpack Compose (BOM 2026.06.00)
- **设计规范**: Material Design 3 Expressive

---

## 二、开发环境路径

| 工具 | 路径 |
|------|------|
| **JDK 17** | `/mnt/TY/android/android-project/classapp/source/jdk-17` |
| **Android SDK** | `/mnt/TY/android/android-project/classapp/source/android-sdk` |
| **Gradle** | 9.3.1，wrapper jar 在 `gradle/wrapper/gradle-wrapper.jar`，本地 zip 在 `source/gradle-9.3.1-bin-fixed.zip` |
| **ADB** | `/mnt/f/platform-tools/adb.exe`（Windows 端，不要用 Linux 的 adb） |
| **项目根目录** | `/mnt/TY/android/android-project/classapp` |
| **APK 输出** | `app/build/outputs/apk/debug/app-debug.apk` |
| **私有仓库** | https://github.com/TYOPXN360/classapp |

### 环境变量设置（每次构建前执行）

```bash
cd /mnt/TY/android/android-project/classapp
export JAVA_HOME="$PWD/source/jdk-17"
export ANDROID_HOME="$PWD/source/android-sdk"
export ANDROID_USER_HOME="$PWD/.android"
export GRADLE_USER_HOME="$PWD/.gradle"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/build-tools/36.0.0:$PATH"
```

**注意**: 
- `GRADLE_USER_HOME` 必须设在项目内，因为 `/home/tyopxn360` 是只读文件系统
- `ANDROID_USER_HOME` 也要设，避免写入 `/home/tyopxn360/.android`

### 构建命令

```bash
./gradlew assembleDebug --no-daemon 2>&1 | tail -3
```

### ADB 使用

```bash
# 杀掉 Linux adb（会占用 5037 端口）
killall adb 2>/dev/null; sleep 1

# 查看设备
/mnt/f/platform-tools/adb.exe devices

# 安装 APK（注意用 wslpath 转换路径）
/mnt/f/platform-tools/adb.exe install -r "$(wslpath -w /mnt/TY/android/android-project/classapp/app/build/outputs/apk/debug/app-debug.apk)"

# 保留数据卸载
/mnt/f/platform-tools/adb.exe uninstall -k com.classapp.schedule

# 查看日志
PID=$(/mnt/f/platform-tools/adb.exe shell pidof com.classapp.schedule | tr -d '\r\n')
/mnt/f/platform-tools/adb.exe logcat -d --pid=$PID 2>&1 | grep "GdustApi" | tail -20

# 清空日志
/mnt/f/platform-tools/adb.exe logcat -c

# 截图
/mnt/f/platform-tools/adb.exe exec-out screencap -p > /mnt/TY/android/android-project/classapp/source/screenshot.png

# 启动 App
/mnt/f/platform-tools/adb.exe shell am start -n com.classapp.schedule/.MainActivity
```

---

## 三、技术栈依赖

| 组件 | 版本 | 用途 |
|------|------|------|
| AGP | 8.7.3 | Android 构建插件 |
| Kotlin | 2.0.21 | 语言 |
| Compose BOM | 2026.06.00 | Compose UI 框架 |
| Material 3 | BOM 内置 | Material Design 3 组件 |
| material-components-android | 1.12.0 | MDC-Android (github.com/material-components/material-components-android) |
| Room | 2.6.1 | 本地数据库 |
| KSP | 2.0.21-1.0.27 | 注解处理 |
| Navigation Compose | 2.8.5 | 页面导航 |
| DataStore | 1.1.1 | 键值存储 |
| OkHttp | 4.12.0 | 网络请求 |
| kotlinx-serialization | 1.7.3 | JSON 解析 |
| ZXing | 3.5.3 | QR 码生成 |
| AppCompat | 1.7.0 | Activity 语言切换支持 |
| WorkManager | 2.9.1 | 后台任务调度（同步、心跳） |
| Coroutines | 1.9.0 | 异步处理 |

### 关键依赖说明

- **material-components-android**: 用户明确要求使用 `github.com/material-components/material-components-android`
- **emoji2**: 不需要，已在 AndroidManifest 中禁用 `EmojiCompatInitializer`（否则 ClassNotFoundException）
- **WorkManager**: 用于定时同步课程和 Token 心跳保活

---

## 四、AndroidManifest 注意事项

```xml
<!-- 必须声明的权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- 禁用 emoji2 自动初始化（避免 ClassNotFoundException） -->
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="androidx.emoji2.text.EmojiCompatInitializer"
        tools:node="remove" />
</provider>

<!-- 预测性返回手势支持 -->
<application android:enableOnBackInvokedCallback="true" ...>

<!-- 允许 HTTP 明文访问教务系统 (172.16.254.1) -->
<application android:networkSecurityConfig="@xml/network_security_config" ...>
```

### network_security_config.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">172.16.254.1</domain>
    </domain-config>
</network-security-config>
```

---

## 五、项目架构

### 目录结构

```
app/src/main/java/com/classapp/schedule/
├── MainActivity.kt              # 主 Activity (AppCompatActivity)
├── ScheduleApp.kt               # 导航框架 (NavHost + 底部导航)
├── ScheduleViewModel.kt         # ViewModel (状态管理 + 业务逻辑)
├── api/
│   ├── GdustApi.kt              # 广科教务系统 API 客户端 (portal + JWXT)
│   └── CourseImporter.kt        # API 数据 → Course 实体转换
├── data/
│   ├── Course.kt                # Room 实体 (含 isManuallyEdited 字段)
│   ├── CourseDao.kt             # Room DAO
│   ├── CourseDatabase.kt        # Room 数据库 (v1→v2→v3 migration)
│   ├── CredentialStore.kt       # AES-256-GCM 加密凭据存储
│   └── SettingsDataStore.kt     # DataStore Preferences 存储
├── notification/
│   ├── ReminderReceiver.kt      # 课程提醒 BroadcastReceiver
│   ├── ReminderScheduler.kt     # AlarmManager 定时提醒
│   └── BootReceiver.kt          # 开机重新设置提醒
├── sync/
│   ├── AutoSyncWorker.kt        # WorkManager 定时同步课程
│   └── HeartbeatWorker.kt       # WorkManager Token 心跳保活 (15min)
├── ui/
│   ├── theme/
│   │   ├── Color.kt             # 颜色定义
│   │   ├── Theme.kt             # Material 3 主题（动态取色）
│   │   └── Type.kt              # 排版
│   ├── today/TodayScreen.kt     # 今日课程页（进度波浪动画+近期考试）
│   ├── weekly/WeeklyScheduleScreen.kt  # 周课表页（核心页面+考试条）
│   ├── course/CourseEditScreen.kt # 课程编辑页
│   ├── exam/ExamScreen.kt       # 考试安排页（卡片列表）
│   ├── manage/CourseManageScreen.kt # 全局课程管理
│   ├── settings/SettingsScreen.kt # 设置页（NavHost 子导航）
│   ├── settings/SettingsActivity.kt # 设置独立 Activity
│   ├── login/LoginScreen.kt     # 密码登录（支持密码管理器填充）
│   ├── login/WebViewLoginScreen.kt # 扫码登录
│   └── about/AboutScreen.kt     # 我的页面（账号+学期信息+考试入口）
└── util/
    ├── CourseColors.kt          # 4种颜色引擎 + 动态分配 + getBackgroundStatic
    ├── HapticFeedback.kt        # 差异化震动反馈
    ├── ImageExport.kt           # 图片导出到相册
    └── JsonImportExport.kt      # JSON/ICS 导入导出
```

---

## 六、核心功能清单

### 课表显示
- ✅ 今日课程 + 明日预告（16:00-23:59 显示）
- ✅ 周课表网格（水平翻页 HorizontalPager）
- ✅ 合并/分割/详细分割三种显示模式
- ✅ 左右滑动切换周次
- ✅ 周次快速跳转（底部 Sheet）
- ✅ 冲突课程混色显示（Canvas 绘制重叠区域混合色）
- ✅ 回到当前周悬浮按钮（箭头方向随周次变化，带动画）
- ✅ 表头显示日期开关
- ✅ 显示时间段开关
- ✅ 下拉刷新（Material 3 PullToRefreshBox）
- ✅ 课程详情弹窗（点击）/ 编辑（长按）
- ✅ 当前周显示高亮"今"字标签
- ✅ FAB 折叠/展开按钮（56dp，与 + 号同尺寸）
- ✅ 课表截图（截取课表区域保存到 Pictures/Screenshots）

### 今日页
- ✅ 今日课程列表（进度波浪动画从0加载到实际百分比）
- ✅ 多课程按进度高低交错动画（rememberSaveable 保持状态）
- ✅ 已完成课程全色块填充 + 波浪边界
- ✅ 进行中课程波浪动画进度条
- ✅ 打勾icon在动画完成后才显示（scaleIn + fadeIn）
- ✅ 明日课程预告（16:00-23:59 显示）
- ✅ 近期考试显示（未来5场）
- ✅ 节次显示（1-2节/1节格式）

### 考试安排
- ✅ 从教务系统 (172.16.254.1) 获取考试信息
- ✅ 自动匹配当前学年学期（基于手机日期）
- ✅ 学年/学期手动选择器（2025-2026格式）
- ✅ 考试卡片列表样式（已过期降低透明度+打勾标记）
- ✅ 考试显示在课表网格中（Canvas绘制红色色块+考标签）
- ✅ 今日页面显示近期考试
- ✅ 考试周不被"隐藏无课周"跳过
- ✅ 考试数据缓存到DataStore（重启自动加载）
- ✅ 设置页显示考试安排开关

### 课程管理
- ✅ 全局课程管理页（按课程名分组）
- ✅ 课程编辑（名称/教师/教室/星期/节次/颜色/周次/备注/自定义时间）
- ✅ 课程备注
- ✅ 自定义时间（非节次）
- ✅ 手动编辑过的课程同步时不会被覆盖（isManuallyEdited 字段）

### 教务登录
- ✅ 统一身份认证登录（密码 + 验证码）
- ✅ 扫码登录（SSE 订阅 → QR 码生成 → 钉钉扫码）
- ✅ 登录成功自动导入课程
- ✅ Token 过期非侵入式处理（我的页面显示提示，验证码对话框快速登录）
- ✅ CAS ticket 持久化（用于教务系统 SSO 认证）
- ✅ 登录页支持密码管理器自动填充（Semantics ContentType）
- ✅ 启动 App 自动刷新课程

### 数据管理
- ✅ JSON 导入/导出（兼容 SchedU 格式）
- ✅ ICS 日历导出
- ✅ 课表截图保存到 Pictures/Screenshots

### 个性化
- ✅ 深色/浅色/跟随系统
- ✅ 中文/英文/跟随系统
- ✅ 4种颜色引擎（Monet动态/鲜艳/经典/HSL旋转）
- ✅ 同科不同地点颜色区分（同色/同饱和度/完全不同色）
- ✅ 自适应内容高度 / 手动固定高度
- ✅ 格子圆角/间距可调
- ✅ 节次标号/时间段显示开关
- ✅ 隐藏无课周（考试开关开启时，有考试的周也算非空周）
- ✅ 课程提醒（5/10/15/30分钟，重启后自动重新设置）

### 自动同步
- ✅ 启动时自动同步（可关闭）
- ✅ 定时同步（WorkManager，分钟/小时/天可选，默认每天）
- ✅ Token 心跳保活（WorkManager，15分钟间隔，低功耗+网络可用时运行）

### 系统集成
- ✅ 预测性返回手势（enableOnBackInvokedCallback）
- ✅ 差异化震动反馈
- ✅ 高刷新率支持（preferredRefreshRate）
- ✅ 状态栏深浅色自动切换
- ✅ 语言切换重建 Activity
- ✅ network_security_config 允许教务系统 HTTP 明文访问

---

## 七、教务系统 API

### 登录流程
```
1. GET /cas-api/sse/subscribe          → SSE 流获取 clientId
2. QR 码 URL: https://cas.gdust.edu.cn/cas/mobieAuth?clientId=xxx
3. POST /cas-api/cas/loginByAccount    → {loginName, loginPwd, code, uuid} → ticket
4. GET /cas-api/cas/checkTicket?ticket → 验证 ticket
5. GET /smart-admin-api/user/login?loginCode=GKCASxxx → token
```

### 数据获取
```
GET /smart-admin-api/app/zf/get_school_calendar?jobNumber=xxx
  → {year, semester, week, allWeek, startTime, endTime}

GET /smart-admin-api/app/zf/get_student_course?jobNumber=xxx&week=&year=&semester=
  → {courseList: [{singleOrDoubleWeek, week, dayWeek, whichSection, classroomName, courseName, teacher, courseDate}]}

GET /smart-admin-api/app/userInfo/addition
  → {realName, deptName, jobNum}
```

### 考试系统 API (JWXT - 172.16.254.1)
```
# 认证流程 (CAS SSO)
1. GET http://172.16.254.1/sso/lyiotlogin?ticket=CAS_TICKET → 302
2. Follow redirects to establish JWXT session (JSESSIONID cookie)

# 查询考试
POST http://172.16.254.1/kwgl/kscx_cxXsksxxIndex.html?doType=query&gnmkdm=N358105
Body: xnm=2025&xqm=12&queryModel.showCount=50&queryModel.currentPage=1
Response: {items: [{kcmc, kssj, cdmc, ksfs, xf, ...}]}
```

### 认证方式
- Portal API: Header `TOKEN: USER###学号###长字符串token`
- JWXT: CAS SSO + JSESSIONID cookie
- CAS ticket 通过 `loginByAccount` 获取并持久化

### API 响应格式
- Portal: `{"ok": true, "data": {...}}` — 注意是 `ok` 不是 `success`！
- 验证码返回: `{"code":0, "ok":true, "data":{"codeUrl":"data:image/jpeg;base64,xxx", "uuid":"xxx"}}`
- SSE 格式: `data: clientId\n\n`
- JWXT 考试: `{items: [{kcmc, kssj, cdmc, ksfs, xf, ...}], totalCount}`

---

## 八、数据库结构

### Course 表 (Room)
```kotlin
@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val teacher: String = "",
    val classroom: String = "",
    val dayOfWeek: Int,        // 1=Mon ... 7=Sun
    val startPeriod: Int,      // 1-based
    val periods: Int = 1,
    val colorIndex: Int = 0,
    val weekRange: String = "all",  // "all", "odd", "even", "1-16,18"
    val remark: String = "",
    val isCustomTime: Boolean = false,
    val customStartTime: String = "",
    val customEndTime: String = "",
    val isManuallyEdited: Boolean = false // 手动编辑过，同步时不会被覆盖
)
```

### Migration v1→v2→v3
```sql
-- v1→v2
ALTER TABLE courses ADD COLUMN remark TEXT NOT NULL DEFAULT ''
ALTER TABLE courses ADD COLUMN isCustomTime INTEGER NOT NULL DEFAULT 0
ALTER TABLE courses ADD COLUMN customStartTime TEXT NOT NULL DEFAULT ''
ALTER TABLE courses ADD COLUMN customEndTime TEXT NOT NULL DEFAULT ''
-- v2→v3
ALTER TABLE courses ADD COLUMN isManuallyEdited INTEGER NOT NULL DEFAULT 0
```

---

## 九、已知问题和修复记录

| 问题 | 根因 | 修复方案 |
|------|------|----------|
| `emoji2 ClassNotFoundException` | AppCompat 依赖缺失 emoji2 | AndroidManifest 禁用 EmojiCompatInitializer |
| `PredictiveBackHandler crash` | 必须 collect progress flow | 改用 NavHost 原生支持 |
| 验证码图片不显示 | API 返回 `codeUrl` 而非 `image` | 解析 `codeUrl` 字段 |
| SSE 阻塞不返回 clientId | `useLines` 对流式响应阻塞 | 改用 `byteStream().read(buffer)` |
| 课程数据全是同一周 | API 需要 `year`+`semester` 参数 | 从 school_calendar 获取后传入 |
| Token 过期无提示 | 没有检测机制 | 我的页面显示提示+验证码对话框 |
| 设置页双重顶栏 | SettingsActivity + SubPage 都有 TopAppBar | SettingsActivity 移除外层 Scaffold |
| 语言切换不生效 | ComponentActivity 不支持 | 改用 AppCompatActivity + AppCompatDelegate |
| 颜色引擎颜色相似 | alpha=0.15 太淡 | 改用 MD3E container 色 / HSL 色相旋转 |
| 教务系统 HTTP 被拦截 | Android 禁止明文 HTTP | 添加 network_security_config.xml |
| 考试系统 SSO 认证失败 | CAS ticket 未持久化 | 登录时保存 CAS ticket 到 DataStore |
| 课程提醒重启后失效 | AlarmManager 闹钟被系统清除 | 添加 BootReceiver 重新设置 |
| 折叠按钮位置偏移 | SmallFloatingActionButton 内部尺寸变化 | 改用 Box + 固定 40dp |
| 今日页进度动画不生效 | 生命周期问题 | 使用 rememberSaveable 保持状态 |

---

## 十、Git 提交规范

```bash
# 提交格式
git add -A && git commit -m "type: 描述"

# type 类型:
# feat:     新功能
# fix:      修复
# refactor: 重构
# style:    样式
# docs:     文档
# chore:    构建/配置
```

**当前最新提交**: `41f91eb docs: 添加 README 项目说明文档`
**私有仓库**: https://github.com/TYOPXN360/classapp

---

## 十一、APK 临时分享

当 ADB 不可用时，使用 tmpfile.link 上传：

```bash
curl -s -X POST -F "file=@app/build/outputs/apk/debug/app-debug.apk" https://tmpfile.link/api/upload 2>&1 | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['downloadLink'])"
```

返回格式：`https://dN.tfdl.net/public/日期/uuid/app-debug.apk`

---

## 十二、课程颜色系统

### 4种颜色引擎
| 引擎 | 说明 |
|------|------|
| 0 (Monet Dynamic) | 从系统壁纸主色出发，360/count 度均匀色相 |
| 1 (Vibrant) | 固定8个高区分色相（0/30/50/140/180/220/270/330°） |
| 2 (Classic Pastel) | 暖冷交替 15/45/100/160/210/260/300/340° |
| 3 (HSL Rotation) | 同 Monet 但奇偶饱和度交替增加层次 |

### 颜色分配逻辑
- 按 `(课程名 + 教室)` 分组，同组同色
- `colorIndex` 存数据库但实际渲染时在 `renderBlocks` 中动态分配
- `getColors(engine, count)` 生成 `count` 个颜色对 `(背景, 文字)`
- `getBackgroundStatic` 非 Composable 版本，用于 Canvas 绘制
- 重叠课程混合色：Canvas 绘制时对重叠区域 RGB 平均混合

---

## 十三、关键设计决策

1. **动态取色优先**: minSdk=31，全部设备支持 Monet，始终使用 `dynamicColorScheme`
2. **Settings 独立 Activity**: 避免与主 Activity 状态冲突，支持独立语言切换
3. **WebView 扫码**: SSE 保持连接等待扫码结果，非轮询
4. **Overlay 网格**: 课表用 BoxWithConstraints + offset 定位课程块，支持跨行
5. **PullToRefreshBox**: Material 3 原生下拉刷新组件，需垂直滚动才能触发
6. **HorizontalPager**: 课表周次切换，支持预测性返回
7. **CAS SSO**: 教务系统通过 CAS ticket 建立 session，ticket 持久化到 DataStore
8. **WorkManager**: 后台任务用 WorkManager 调度（同步、心跳），低功耗
9. **手动编辑保护**: isManuallyEdited 字段，同步时不覆盖手动编辑的课程
10. **考试数据缓存**: 考试信息缓存到 DataStore，重启自动加载

---

## 十四、常用调试命令

```bash
# 实时看 GdustApi 日志
PID=$(/mnt/f/platform-tools/adb.exe shell pidof com.classapp.schedule | tr -d '\r\n')
/mnt/f/platform-tools/adb.exe logcat -d --pid=$PID 2>&1 | grep "GdustApi" | tail -20

# 查看课程数据库内容
/mnt/f/platform-tools/adb.exe shell "run-as com.classapp.schedule sqlite3 databases/course_database 'SELECT COUNT(*) FROM courses;'"

# 查看崩溃日志
/mnt/f/platform-tools/adb.exe logcat -d -t 300 2>&1 | grep -B 2 -A 20 "FATAL EXCEPTION" | head -30

# 检查 App 是否在运行
/mnt/f/platform-tools/adb.exe shell pidof com.classapp.schedule

# 查看心跳日志
/mnt/f/platform-tools/adb.exe logcat -d --pid=$PID 2>&1 | grep "Heartbeat" | tail -10

# 查看考试查询日志
/mnt/f/platform-tools/adb.exe logcat -d --pid=$PID 2>&1 | grep "GdustApi.*Exam\|GdustApi.*SSO\|GdustApi.*Step" | tail -20
```

---

**记忆创建时间**: 2026-06-25
**最后更新**: 2026-06-27
**对话历史**: 大量迭代开发，包含登录系统、课表渲染、颜色引擎、下拉刷新、设置页面、扫码登录、考试安排、自动同步、Token心跳保活、密码管理器自动填充、课表截图等功能的实现和反复调试。
