package com.ty.gkschedule.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * Material Design 3 Motion Constants
 *
 * 基于 m3.material.io/styles/motion/transitions 规范
 *
 * 过渡模式:
 * - Container Transform: 卡片/列表 → 详情页
 * - Shared Axis: X/Y/Z 轴方向过渡
 * - Fade Through: 无空间关系的目标切换
 * - Fade: 对话框/菜单/Snackbar
 */
object M3Motion {

    // =========================================================================
    // 缓动曲线 (Easing)
    // =========================================================================

    /** Standard: 实用动画，开始和结束都在屏幕上 */
    val StandardEasing = FastOutSlowInEasing

    /** Standard Decelerate: 实用动画，进入屏幕 */
    val StandardDecelerateEasing = LinearOutSlowInEasing

    /** Standard Accelerate: 实用动画，离开屏幕 */
    val StandardAccelerateEasing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    /** Emphasized: M3 风格动画，开始和结束都在屏幕上 */
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Emphasized Decelerate: M3 风格动画，进入屏幕 */
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Emphasized Accelerate: M3 风格动画，离开屏幕 */
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    // =========================================================================
    // 持续时间 (Duration)
    // =========================================================================

    // Short (小组件: 开关、按钮)
    const val Short1 = 50
    const val Short2 = 100
    const val Short3 = 150
    const val Short4 = 200

    // Medium (中等组件: 导航栏、侧边栏)
    const val Medium1 = 250
    const val Medium2 = 300
    const val Medium3 = 350
    const val Medium4 = 400

    // Long (全屏动画: 过渡效果)
    const val Long1 = 450
    const val Long2 = 500
    const val Long3 = 550
    const val Long4 = 600

    // ExtraLong (复杂动画)
    const val ExtraLong1 = 700
    const val ExtraLong2 = 800
    const val ExtraLong3 = 900
    const val ExtraLong4 = 1000

    // =========================================================================
    // Tab 切换过渡 (Shared Axis X)
    // =========================================================================

    /** Tab 进入: 从右侧滑入 */
    fun tabSlideInSpec() = tween<IntOffset>(Medium4, easing = EmphasizedDecelerateEasing)

    /** Tab 退出: 向左侧滑出 */
    fun tabSlideOutSpec() = tween<IntOffset>(Short4, easing = EmphasizedAccelerateEasing)

    /** Tab 淡入 */
    fun fadeInSpec() = tween<Float>(Medium2, easing = EmphasizedDecelerateEasing)

    /** Tab 淡出 */
    fun fadeOutSpec() = tween<Float>(Short3, easing = EmphasizedAccelerateEasing)

    // =========================================================================
    // 子页面过渡 (Fade Through)
    // =========================================================================

    /** 子页面进入: 淡入 */
    fun subPageEnterSpec() = tween<Float>(Medium4, easing = EmphasizedDecelerateEasing)

    /** 子页面退出: 淡出 */
    fun subPageExitSpec() = tween<Float>(Short4, easing = EmphasizedAccelerateEasing)

    // =========================================================================
    // FAB 动画 (Fade)
    // =========================================================================

    /** FAB 进入 */
    fun fabEnterSpec() = tween<Float>(Short4, easing = EmphasizedDecelerateEasing)

    /** FAB 退出 */
    fun fabExitSpec() = tween<Float>(Short3, easing = EmphasizedAccelerateEasing)
}
