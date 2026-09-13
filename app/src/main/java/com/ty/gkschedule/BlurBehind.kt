package com.ty.gkschedule

import androidx.compose.ui.Modifier

// ponytail: 判死——createBackgroundBlurDrawable糊的是窗口backdrop，同窗口内容永不到那层；
// 真模糊走路A(ScheduleApp内GraphicsLayer离屏纹理+RenderEffect)，此处仅保留兜底空实现防旧引用
fun Modifier.blurBehind(enabled: Boolean, radiusDp: Float = 24f): Modifier = this
