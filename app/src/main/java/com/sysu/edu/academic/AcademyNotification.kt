package com.sysu.edu.academic

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem

class AcademyNotification : BaseActivity() {
    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = ViewModelProvider(this)[AcademyNotificationViewModel::class.java]
        setContent {
            val academicNotices by viewModel.academicNotices.observeAsState(emptyList())
            val schoolNotices by viewModel.schoolNotices.observeAsState(emptyList())
            val noticeContent by viewModel.noticeContent.observeAsState()
            var sharedElementBounds by remember { mutableStateOf<Rect?>(null) }

            LaunchedEffect(Unit) {
                viewModel.fetchNotices()
            }

            LaunchedEffect(noticeContent) {
                noticeContent?.let { content ->
                    val intent = Intent(this@AcademyNotification,
                                        BrowserActivity::class.java).putExtra("data",
                                                                              ("""<!DOCTYPE html><html><head>
																			  <style>
                                            body{
                                            padding: 24px !important;
                                            }
                                            a,body,p,span{
                                            font-size: 2.5rem !important;
                                            line-height: 2.0 !important;
                                             }
                                             table{
                                            table-layout: auto !important;
                                            width: 100% !important;
                                             }
                                             table,th, td
                                                    {
                                            font-size: 1.0rem !important;
                                            line-height: 1.0 !important;
                                                    border-collapse: collapse !important;
                                                    border: 2px solid windowtext !important;
                                                    }
                                            </style></head><body>""".trimIndent() + content + "</body></html>").trim())
                    
                    val options = sharedElementBounds?.let { bounds ->
                        val view = android.view.View(this@AcademyNotification).apply {
                            x = bounds.left
                            y = bounds.top
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                bounds.width.toInt(),
                                bounds.height.toInt()
                            )
                            transitionName = "miniapp"
                        }
                        (window.decorView as android.view.ViewGroup).addView(view)
                        val opt = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            this@AcademyNotification,
                            view,
                            "miniapp"
                        ).toBundle()
                        window.decorView.postDelayed({ (window.decorView as android.view.ViewGroup).removeView(view) }, 1000)
                        opt
                    } ?: ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this@AcademyNotification,
                        android.view.View(this@AcademyNotification),
                        "miniapp"
                    ).toBundle()

                    startActivity(intent, options)
                    viewModel.clearNoticeContent()
                }
            }

            SharedTransitionLayout {
                ActivityPager(
                    title = stringResource(id = R.string.academic_affair_notice),
                    tabs = mutableListOf(MenuItem(stringResource(id = R.string.academic_affair_notice)),
                                         MenuItem(stringResource(id = R.string.school_affair_notice))
                    ),
                    onNavigationClick = { supportFinishAfterTransition() },
                    pageContent = { page ->
                        AnimatedContent(targetState = page, label = "page_transition") { targetPage ->
                            val notices = if (targetPage == 0) academicNotices else schoolNotices
                            NewsList(
                                newsList = notices,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@AnimatedContent
                            ) { notice, bounds ->
                                sharedElementBounds = bounds
                                viewModel.fetchContent(notice.getString("id"))
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NewsList(
	newsList: List<JSONObject>,
	sharedTransitionScope: SharedTransitionScope,
	animatedVisibilityScope: AnimatedVisibilityScope,
	onItemClick: (JSONObject, Rect) -> Unit,
            ) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(240.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(dimensionResource(R.dimen.content_padding)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_padding)),
        verticalItemSpacing = dimensionResource(R.dimen.vertical_padding)
    ) {
        items(newsList, key = { it.getString("id") }) { item ->
            NewsItem(
                item = item,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            ) { bounds ->
                onItemClick(item, bounds)
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NewsItem(
	item: JSONObject,
	sharedTransitionScope: SharedTransitionScope,
	animatedVisibilityScope: AnimatedVisibilityScope,
	onClick: (Rect) -> Unit,
            ) {
    var currentBounds by remember { mutableStateOf(Rect.Zero) }
    with(sharedTransitionScope) {
        Card(
            onClick = { onClick(currentBounds) },
            modifier = Modifier
	            .fillMaxWidth()
	            .onGloballyPositioned { currentBounds = it.boundsInWindow() }
	            .sharedBounds(rememberSharedContentState(key = "news_${item.getString("id")}"), animatedVisibilityScope = animatedVisibilityScope)
        ) {
            Column(
                modifier = Modifier
	                .padding(dimensionResource(R.dimen.content_padding))
	                .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_padding))
            ) {
                Text(
                    text = item.getString("title") ?: "",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = item.getString("deliveryDate") ?: "",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
