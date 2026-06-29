# ClassApp M3 改造计划

## 从 m3.material.io CSS 变量提取的官方规范

### M3 v2 Type Scale（字体比例）
| 类型 | 字号 | 行高 | 字重 | 字间距 | font-variation |
|------|------|------|------|--------|----------------|
| Display XL | 88px | 96px | 475 | 0 | GRAD:0, opsz:18 |
| Display L | 57px | 64px | 475 | 0 | GRAD:0, opsz:18 |
| Display M | 45px | 52px | 475 | 0 | GRAD:0, opsz:18 |
| Display S | 36px | 44px | 475 | 0 | GRAD:0, opsz:18 |
| Headline L | 32px | 40px | 475 | 0 | GRAD:0, opsz:18 |
| Headline M | 28px | 36px | 475 | 0 | GRAD:0, opsz:18 |
| Headline S | 24px | 32px | 475 | 0 | GRAD:0, opsz:18 |
| Title L | 22px | 30px | 400 | 0 | GRAD:0, opsz:17 |
| Title M | 16px | 24px | 500 | 0 | GRAD:0, opsz:17 |
| Title S | 14px | 20px | 500 | 0 | GRAD:0, opsz:17 |
| Label L | 14px | 20px | 500 | 0 | GRAD:0, opsz:17 |
| Label M | 12px | 16px | 500 | 0.1px | GRAD:0, opsz:17 |
| Label S | 11px | 16px | 500 | 0.1px | GRAD:0, opsz:17 |
| Body L | 16px | 24px | 400 | 0 | GRAD:0, opsz:17 |
| Body M | 14px | 20px | 400 | 0 | GRAD:0, opsz:17 |
| Body S | 12px | 16px | 400 | 0.1px | GRAD:0, opsz:17 |
| Code L | 16px | 24px | 400 | 0 | — |
| Code M | 14px | 20px | 400 | 0 | — |

### M3 v2 Color Tokens（从 CSS 变量）
- Primary: #6442D6 (light) / #CBBEFF (dark 80)
- On Primary: #fff / #4B21BD (30)
- Primary Container: #9F86FF
- On Primary Container: #1E0060
- Secondary: #5D5D74
- On Secondary: #fff
- Secondary Container: #DCDAF5
- On Secondary Container: #21182B
- Tertiary Container: #F1D3F9
- On Tertiary Container: #271430
- Surface: #FFFBFF (light) / #1C1B1F (dark)
- Surface Variant: #E8E0E8
- On Surface: #1C1B1D
- On Surface Variant: #4D4256
- Inverse Surface: #303030
- Inverse On Surface: #F5EFF1
- Error: #FF6240
- On Error: #490909
- Error Container: #F9DEDC
- Outline: #787579

### M3 v2 Elevation
- Level 1: 0px 1px 2px rgba(0,0,0,30%), 0px 1px 3px 1px rgba(0,0,0,15%)
- Level 2: 0px 1px 2px rgba(0,0,0,30%), 0px 2px 6px 2px rgba(0,0,0,15%)
- Level 3: 0px 1px 3px rgba(0,0,0,30%), 0px 4px 8px 3px rgba(0,0,0,15%)

### Shape
- Corner: 全站使用 16px 圆角（dialog），12px（card）

---

## 当前 App 与 M3 v2 的差距

### 1. Typography（Type.kt）— 缺失 Display 类型，字重不匹配
- 缺少 Display XL/L/M/S
- Headline 用了 Bold(700)/SemiBold(600)，应为 475
- Title L 用了 SemiBold(600)，应为 400
- Label 字重应为 500
- 缺少 fontVariationSettings（GRAD, opsz）

### 2. Color（Theme.kt）— 已使用 Dynamic Color，基本正确
- 已用 dynamicLightColorScheme/dynamicDarkColorScheme ✅
- 不需要改 color

### 3. Shape（Theme.kt）— 基本正确
- ExpressiveShapes 已定义，圆角合理 ✅

### 4. 组件使用
- TopAppBar ✅
- NavigationBar ✅
- Card ✅
- ListItem ✅
- FilterChip / SuggestionChip ✅
- AlertDialog ✅
- FAB ✅
- OutlinedTextField ✅
- Switch ✅
- Slider ✅
- DropdownMenu ✅
- PullToRefreshBox ✅

### 5. 需要改造的文件
| 文件 | 改动 |
|------|------|
| `Type.kt` | 加 Display 类型，修正字重为 475/400/500，加 fontVariationSettings |
| `Theme.kt` | 无改动（Dynamic Color 已正确） |
| 其他 UI 文件 | 检查并修正字体使用 |

## 执行步骤
1. 修改 Type.kt：加入 Display 类型，修正所有字重
2. 检查所有 UI 文件中的 fontWeight 使用是否符合 M3 v2
3. 构建、安装、验证
