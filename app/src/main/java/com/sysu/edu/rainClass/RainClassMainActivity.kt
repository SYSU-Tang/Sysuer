package com.sysu.edu.rainClass

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import kotlinx.coroutines.launch

class RainClassMainActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			MaterialTheme {
				val view = LocalView.current
				SideEffect {
					view.transitionName = "miniapp"
				}
				RainClassMainContent()
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class) @Composable fun RainClassMainContent() {
	val pagerState = rememberPagerState(pageCount = { 3 })
	val scope = rememberCoroutineScope()
	val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
	val context = LocalContext.current
	
	Scaffold(modifier = Modifier
		.fillMaxSize()
		.nestedScroll(scrollBehavior.nestedScrollConnection),
	         topBar = {
		         TopAppBar(title = {
			         Text(stringResource(when (pagerState.currentPage) {
				                             0 -> R.string.course
				                             1 -> R.string.exam
				                             2 -> R.string.account
				                             else -> R.string.course
			                             }))
		         }, scrollBehavior = scrollBehavior, navigationIcon = {
			         IconButton(onClick = { (context as? android.app.Activity)?.finish() }) {
				         Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,
				              contentDescription = "返回")
			         }
		         })
	         },
	         bottomBar = {
		         NavigationBar {
			         NavigationBarItem(selected = pagerState.currentPage == 0, onClick = {
				         scope.launch {
					         pagerState.animateScrollToPage(0)
				         }
			         }, icon = {
				         Icon(painter = painterResource(id = R.drawable.course),
				              contentDescription = stringResource(R.string.course))
			         }, label = { Text(stringResource(R.string.course)) })
			         NavigationBarItem(selected = pagerState.currentPage == 1, onClick = {
				         scope.launch {
					         pagerState.animateScrollToPage(1)
				         }
			         }, icon = {
				         Icon(painter = painterResource(id = R.drawable.exam),
				              contentDescription = stringResource(R.string.exam))
			         }, label = { Text(stringResource(R.string.exam)) })
			         NavigationBarItem(selected = pagerState.currentPage == 2, onClick = {
				         scope.launch {
					         pagerState.animateScrollToPage(2)
				         }
			         }, icon = {
				         Icon(painter = painterResource(id = R.drawable.account),
				              contentDescription = stringResource(R.string.account))
			         }, label = { Text(stringResource(R.string.account)) })
		         }
	         }) { innerPadding ->
		HorizontalPager(state = pagerState,
		                modifier = Modifier
			                .fillMaxSize()
			                .padding(innerPadding)) { page ->
			when (page) {
				0 -> CourseScreen(onRequestScrollToAccount = {
					scope.launch {
						pagerState.animateScrollToPage(2)
					}
				})
				1 -> ExamScreen(onRequestScrollToAccount = {
					scope.launch {
						pagerState.animateScrollToPage(2)
					}
				})
				2 -> AccountScreen()
			}
		}
	}
}
