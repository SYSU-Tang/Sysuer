package com.sysu.edu.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.sysu.edu.browser.theme.Pink40
import com.sysu.edu.browser.theme.Pink80
import com.sysu.edu.browser.theme.Purple40
import com.sysu.edu.browser.theme.Purple80
import com.sysu.edu.browser.theme.PurpleGrey40
import com.sysu.edu.browser.theme.PurpleGrey80
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val DarkColorScheme = darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)
private val LightColorScheme = lightColorScheme(primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40	/* Other default colors to override
	background = Color(0xFFFFFBFE),
	surface = Color(0xFFFFFBFE),
	onPrimary = Color.White,
	onSecondary = Color.White,
	onTertiary = Color.White,
	onBackground = Color(0xFF1C1B1F),
	onSurface = Color(0xFF1C1B1F),
	*/)

class MarkdownActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			MiuixTheme (colors = MiuixTheme.colorScheme){
				MiuixMainScreen()
			}
		}
	}
}

@Composable fun Greeting(name: String, modifier: Modifier = Modifier) {
	Text(text = "Hello $name!", modifier = modifier)
}

@OptIn(ExperimentalFoundationApi::class) @Composable
fun MiuixMainScreen() {    // 1. 初始化 Pager 状态（假设有 3 个页面）
	val pageCount = 3
	val pagerState = rememberPagerState(pageCount = { pageCount })
	val coroutineScope = rememberCoroutineScope()    // 2. 初始化 Miuix TopAppBar 的滚动行为（实现小米特色的标准/大标题折叠效果）
	val topAppBarState = rememberTopAppBarState()
	val scrollBehavior: ScrollBehavior = MiuixScrollBehavior(state = topAppBarState) // 定义页面数据
	val items = listOf("首页", "发现", "我的")
	val icons = listOf(MiuixIcons.Settings, MiuixIcons.Settings, MiuixIcons.Scan)
	Scaffold(modifier = Modifier
		.fillMaxSize()                // 关键：将滚动行为注入到嵌套滚动中，这样内部列表滚动时 TopAppBar 才会折叠
		.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {                // Miuix 的 TopAppBar
		TopAppBar(
			title = items[pagerState.currentPage], // 标题跟随当前页面变化
			scrollBehavior = scrollBehavior, // 滚动行为),                    // 如果需要左侧返回键或右侧操作栏，可以在这里配置 navigationIcon 和 actions
		         )
	}, bottomBar = {                // Miuix 的底部导航栏
		NavigationBar(modifier = Modifier.navigationBarsPadding() //			.drawBackdrop(
			//				backdrop = backdrop(),
			//				shape = { RoundedCornerShape(24.dp) },
			//				effects = { blur(20.dp.toPx()) },
			//				highlight = { Highlight.GlassStrokeMiddleLight.copy(alpha = pressProgress) },
			//			             )			// 直接通过 Modifier 赋予液态玻璃模糊，参数包括半径、蒙版颜色、噪点等
			//			.blur(radius = 25.dp, maskColor = Color.White.copy(alpha = 0.4f), cornerRadius = 16.dp // 如果底栏需要圆角)
		              , containerColor = Color.Transparent, // 依旧保持容器透明
		              tonalElevation = 0.dp) {
			items.forEachIndexed { index, title ->
				NavigationBarItem(selected = pagerState.currentPage == index, onClick = {                                // 点击切换 Pager
					coroutineScope.launch {
						pagerState.animateScrollToPage(index)
					}
				}, icon = {
					Icon(imageVector = icons[index], contentDescription = title)
				}, label = { Text(text = title) })
			}
		}
	}) { innerPadding ->            // 3. 页面主体：使用 HorizontalPager 代替传统的 FragmentViewPager
		HorizontalPager(state = pagerState, modifier = Modifier
			.fillMaxSize()
			.padding(innerPadding) // 注意：必须应用 Scaffold 的 innerPadding
		               ) { page ->                // 根据 page 索引渲染不同的内容
			when (page) {
				0 -> HomePage()
				1 -> DiscoverPage()
				2 -> ProfilePage()
			}
		}
	}
} // --- 以下为测试页面组件 ---

@Composable fun HomePage() {    // 页面内必须包含可滚动组件（如 LazyColumn），TopAppBar 的折叠效果才会生效
	LazyColumn(modifier = Modifier.fillMaxSize()) {
		items(50) { index ->
			Box(modifier = Modifier
				.fillMaxSize()
				.padding(top = 24.dp, start = 16.dp)) {
				Text(text = "首页列表项 - $index")
			}
		}
	}
}

@Composable fun DiscoverPage() {
	LazyColumn(modifier = Modifier.fillMaxSize()) {
		items(50) { index ->
			Box(modifier = Modifier
				.fillMaxSize()
				.padding(top = 24.dp, start = 16.dp)) {
				Text(text = "发现列表项 - $index")
			}
		}
	}
}

@Composable fun ProfilePage() {
	Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
		Text(text = "个性设置（不可滚动页面）")
	}
}