## 背景

前景背景面板的布局在2022年的今天应用市场上应该绝大部分 APP 都采用了，特别是比如地图，打车，购物，直播 APP（带货）下面的面板交互实现

![](https://upload-images.jianshu.io/upload_images/3515789-c673ca548356e99d.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/300)|![](https://upload-images.jianshu.io/upload_images/3515789-cb3a8a8921444215.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/300)
---|---

在 Android View 体系中，需要实现前景背景面板还挺麻烦的，通常的方案如下：

*   1、xml 中实现 FrameLayout，分别放置前景板布局和背景布局
*   2、定义前景面板的拖动状态
*   3、拦截事件分发控制前景面板的拖动，控制滑动范围（或者用 ViewDragHelper 实现自定义拖动和动画控制，也要相当大的代码量才能精确控制）
*   4、实现某个状态点附近的回弹动画

GitHub 中有很多类似的开源方案，其中 Star 最多的是 [AndroidSlidingUpPanel](https://github.com/umano/AndroidSlidingUpPanel)，其核心实现类 `AndroidSlidingUpPanel` 也有将近1500行。

## 今天我们就来挑战下 10分钟能不能用 Compose 版本的 SlidingUpPanel

### 确定方案

Compose 版本实现理论上和 View 体系实现差不多，无非也就是布局，拖动控制，范围控制

*   布局：能画出来就行，通常都是用 Box

*   拖动控制：Modifier的扩展千奇百样，特别是手势相关的，最基础的无非是使用 `Modifier.pointerInput()` 纯控制事件分发来控制布局（类似 View 体系）。而且还有逻辑高度封装的 `draggable` 修饰符或者 `swipeable` 修饰符可以使用，这里我们要拖动并且也要可以动画控制滑动，采用 `swipeable` 修饰符即可，配合 [`SwipeableState`](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#rememberSwipeableState(kotlin.Any,androidx.compose.animation.core.AnimationSpec,kotlin.Function1)) 就可以控制滑动或者动画

    [手势  |  Jetpack Compose  |  Android Developers](https://developer.android.com/jetpack/compose/gestures#swiping)

*   范围控制：`swipeable` 修饰符直接自带！！！

### 实现

#### 布局

上来肯定是先画出布局，这里直接无脑 Box Box Box，Box 三连

```
@Composable
fun SlidingUpPanel(
    backgroundContent: @Composable BoxScope.() -> Unit,
    foregroundContent: @Composable BoxScope.() -> Unit
) {

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            backgroundContent()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            foregroundContent()
        }
    }
}

```

#### 拖动控制

根据官方文档 [手势 | Jetpack Compose | Android Developers](https://developer.android.com/jetpack/compose/gestures#swiping) 的描述和示范例子，还有 [swipeable](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#(androidx.compose.ui.Modifier).swipeable(androidx.compose.material.SwipeableState,kotlin.collections.Map,androidx.compose.foundation.gestures.Orientation,kotlin.Boolean,kotlin.Boolean,androidx.compose.foundation.interaction.MutableInteractionSource,kotlin.Function2,androidx.compose.material.ResistanceConfig,androidx.compose.ui.unit.Dp)) 修饰符的文档描述，惊了大离谱吧，swipeable 不仅可以让布局滑动，还可以通过 anchors 来控制滑动状态点的距离，甚至还可以动画回弹

```
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SlidingUpPanel(
    swipeableState: SwipeableState<PanelStateEnum> = rememberSwipeableState(PanelStateEnum.COLLAPSED),
    enabled: Boolean = true,
    backgroundContent: @Composable BoxScope.() -> Unit,
    foregroundContent: @Composable BoxScope.() -> Unit
) {

    val anchors = remember {
        mapOf(
            0F to PanelStateEnum.EXPANDED,
            200F to PanelStateEnum.ANCHORED,
            500F to PanelStateEnum.COLLAPSED,
            900F to PanelStateEnum.HIDDEN,
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

```

**就这样就已经支持4个状态的滑动控制+边界控制+边界回弹啦！！！**

#### 简单封装下

上述 anchors 其实只需要 ANCHORED点的偏移和 COLLAPSED 偏移值就好了，定义一个类输入和保存传递相关的值

```
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

```

SlidingUpPanel 最终代码变为

```
/**
 * 前景背景面板布局
 *
 * @param initialPanelState [PanelStateEnum] 面板初始化状态
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

```

#### 使用

只需要和传统方式类似 Button，Box 一样使用即可

```
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Home() {
    val scope = rememberCoroutineScope()
    val swipeableState = rememberSwipeableState(PanelStateEnum.COLLAPSED)

    SlidingUpPanel(swipeableState = swipeableState, panelStateOffset = {
        this.collapsedOffsetRatio = 0.9
        this.anchoredOffsetRatio = 0.1
    }, backgroundContent = {
        Box(modifier = Modifier
            .border(width = 1.dp, color = Color.Black)
            .fillMaxSize()
            .background(Color.Green), contentAlignment = Alignment.TopCenter) {
            BasicText(
                "背景面板", style = TextStyle.Default.copy(color = Color.Red, fontSize = 25.sp)
            )
            ButtonColumn(scope, swipeableState)
        }
    }, foregroundContent = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Yellow)
                .border(width = 1.dp, color = Color.Black),
            contentAlignment = Alignment.TopCenter,
        ) {
            BasicText(
                "前景面板", style = TextStyle.Default.copy(color = Color.Red, fontSize = 25.sp)
            )
            ButtonColumn(scope, swipeableState)
        }
    })
}

```

#### 手动控制滑动

有时候我们要根据业务情况来控制面板的移动，只需要通过 `SwipeableState` 的 `animateTo()`【有动画】 或者 `snapTo()`【无动画】控制即可

例如，让面板动画移动到 EXPANDED 状态只需要调用

```
swipeableState.animateTo(PanelStateEnum.EXPANDED)

```

### 有图有真相

Gif图巨大，耐心等待

![](https://upload-images.jianshu.io/upload_images/3515789-f03df214b90e5546.gif?imageMogr2/auto-orient/strip)

* * *
![](https://upload-images.jianshu.io/upload_images/3515789-c88521a58faa1b6b.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

### 开源地址

[minminaya/SlidingUpPanel-compose: SlidingUpPanel layout for Android Compose (github.com)](https://github.com/minminaya/SlidingUpPanel-compose)


### 一键依赖

- 1、添加maven 地址

```Kotlin
allprojects {
    repositories {
      ...
      maven { url 'https://jitpack.io' }
    }
  }
```
- 2、声明依赖

```Kotlin
dependencies {
       implementation 'com.github.minminaya:SlidingUpPanel-compose:Tag'
  }
```

