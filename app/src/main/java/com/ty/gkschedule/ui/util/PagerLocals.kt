package com.ty.gkschedule.ui.util

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

// ponytail: ReSukiSU同款分页CompositionLocal——底栏/子页共享pager状态
val LocalPagerState = compositionLocalOf<PagerState> { error("No pager state") }
val LocalPagerPage = staticCompositionLocalOf<Int?> { null }
val LocalHandlePageChange = compositionLocalOf<(Int) -> Unit> { error("No handle page change") }
val LocalSelectedPage = compositionLocalOf<Int> { error("No selected page") }
