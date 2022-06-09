package com.minminaya.sliding

import android.content.Context
import android.graphics.Point
import android.view.WindowManager
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * 前景背景面板布局
 *
 * @param initialPanelState [PanelStateEnum] 当前的面板状态
 * @param swipeableState SwipeableState<PanelStateEnum> swipeable修饰符的状态
 * @param panelStateOffset [@kotlin.ExtensionFunctionType] Function2<PanelStateOffset, Int, Unit> 可以预设面板的各个高度参数
 * @param enabled Boolean 是否开启滑动
 * @param backgroundContent [@androidx.compose.runtime.Composable] [@kotlin.ExtensionFunctionType] Function1<BoxScope, Unit> 背景布局
 * @param foregroundContent [@androidx.compose.runtime.Composable] [@kotlin.ExtensionFunctionType] Function1<BoxScope, Unit> 前景布局
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SlidingUpPanel(
    initialPanelState: PanelStateEnum = PanelStateEnum.COLLAPSED,
    swipeableState: SwipeableState<PanelStateEnum> = rememberSwipeableState(initialPanelState),
    panelStateOffset: PanelStateOffset.(Int) -> Unit = {},
    enabled: Boolean = true,
    backgroundContent: @Composable BoxScope.() -> Unit,
    foregroundContent: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val panelOffset = remember {
        PanelStateOffset(context)
    }

    panelStateOffset.invoke(panelOffset, panelOffset.screenHeight)

    val anchors = remember {
        mapOf(
            0F to PanelStateEnum.EXPANDED,
            panelOffset.anchoredOffset() to PanelStateEnum.ANCHORED,
            panelOffset.collapsedOffset() to PanelStateEnum.COLLAPSED,
            panelOffset.hiddenOffset to PanelStateEnum.HIDDEN,
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            backgroundContent()
        }
        Box(modifier = Modifier
            .offset {
                IntOffset(x = 0, y = swipeableState.offset.value.roundToInt())
            }
            .fillMaxSize()
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                orientation = Orientation.Vertical,
                enabled = enabled
            )) {
            foregroundContent()
        }
    }
}

/**
 *
 * 保存了面板偏移高度相关的参数，偏移量指从上往下
 *
 * @property context Context
 * @property anchoredOffsetRatio Double anchored 状态偏移占屏幕高度的比例
 * @property collapsedOffsetRatio Double collapsed 状态偏移占屏幕高度的比例
 * @property screenHeight Int 屏幕高度
 * @property anchoredOffset Float anchored 状态偏移
 * @property collapsedOffset Float collapsed 状态偏移
 * @property hiddenOffset Float hidden 状态偏移
 * @constructor
 */
data class PanelStateOffset(
    val context: Context,
    var anchoredOffsetRatio: Double = 0.25,
    var collapsedOffsetRatio: Double = 0.75,
    var screenHeight: Int = screenHeight(context),
    private var anchoredOffset: Float = 0f,
    private var collapsedOffset: Float = 0f,
    val hiddenOffset: Float = screenHeight.toFloat(),
) {
    fun anchoredOffset() = (screenHeight * anchoredOffsetRatio).toFloat()
    fun collapsedOffset() = (screenHeight * collapsedOffsetRatio).toFloat()

    fun setOffsetRatio(
        anchoredOffsetRatio: Double, collapsedOffsetRatio: Double, screenHeight: Int = -1
    ) {
        this.anchoredOffsetRatio = anchoredOffsetRatio
        this.collapsedOffsetRatio = collapsedOffsetRatio
        this.screenHeight = if (screenHeight != -1) screenHeight else this.screenHeight
    }

    companion object {
        /**
         * 物理尺寸高度
         *
         * @param context Context
         * @return Int
         */
        @Stable
        fun screenHeight(context: Context): Int {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager? ?: return -1
            val point = Point()
            wm.defaultDisplay.getRealSize(point)
            return point.y
        }
    }
}
