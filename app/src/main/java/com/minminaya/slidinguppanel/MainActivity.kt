package com.minminaya.slidinguppanel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.minminaya.sliding.PanelStateEnum
import com.minminaya.sliding.SlidingUpPanel
import com.minminaya.slidinguppanel.ui.theme.SlidingUpPanelcomposeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            SlidingUpPanelcomposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    Home()
                }
            }
        }
    }
}

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

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun ButtonColumn(
    scope: CoroutineScope, swipeableState: SwipeableState<PanelStateEnum>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        val animateTo = fun(panelStateEnum: PanelStateEnum) {
            scope.launch {
                swipeableState.animateTo(panelStateEnum)
            }
        }
        Button(onClick = {
            animateTo.invoke(PanelStateEnum.EXPANDED)
        }) {
            Text(text = "EXPANDED")
        }
        Button(onClick = {
            animateTo.invoke(PanelStateEnum.ANCHORED)
        }) {
            Text(text = "ANCHORED")
        }
        Button(onClick = {
            animateTo.invoke(PanelStateEnum.COLLAPSED)
        }) {
            Text(text = "COLLAPSED")
        }
        Button(onClick = {
            animateTo.invoke(PanelStateEnum.HIDDEN)
        }) {
            Text(text = "HIDDEN")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SlidingUpPanelcomposeTheme {
        Home()
    }
}