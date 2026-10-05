package com.miyuyan.sysuer.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.R.dimen.horizontal_padding
import com.miyuyan.sysuer.R.dimen.vertical_padding
import com.miyuyan.sysuer.nav.Browser
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.StatePage
import com.miyuyan.sysuer.view.UiState

@Composable
fun AcademyNotificationRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope,
	animatedVisibilityScope: AnimatedVisibilityScope,
) {
	val viewModel: AcademyNotificationViewModel = viewModel()
	val academicNotices by viewModel.academicNotices.collectAsStateWithLifecycle()
	val schoolNotices by viewModel.schoolNotices.collectAsStateWithLifecycle()
	val activity = LocalActivity.current

	val textFieldState1 = rememberTextFieldState()
	val textFieldState2 = rememberTextFieldState()

	val academicNoticesUiState by viewModel.academicNoticesUiState.collectAsStateWithLifecycle()
	val schoolNoticesUiState by viewModel.schoolNoticesUiState.collectAsStateWithLifecycle()

	var id by remember { mutableStateOf("") }

//	LaunchedEffect(textFieldState1.text) {
//		snapshotFlow { textFieldState1.text.toString() }.debounce(300L.milliseconds)
//			.collect { keyword -> viewModel.fetchAcademicNotice(keyword) }
//	}
//
//	LaunchedEffect(textFieldState2.text) {
//		snapshotFlow { textFieldState2.text.toString() }.debounce(300L.milliseconds)
//			.collect { keyword -> viewModel.fetchSchoolNotice(keyword) }
//	}

	LaunchedEffect(Unit) {
		if (viewModel.academicNoticesUiState.value == UiState.Unstarted || viewModel.academicNoticesUiState.value == UiState.Error) {
			viewModel.fetchAcademicNotice()
		}
		if (viewModel.schoolNoticesUiState.value == UiState.Unstarted || viewModel.schoolNoticesUiState.value == UiState.Error) {
			viewModel.fetchSchoolNotice()
		}
		viewModel.noticeContent.collect { noticeContent ->
			noticeContent?.let { content ->
				backStack.add(
						Browser(
								url = "https://jwxt.sysu.edu.cn/jwxt/#/notice/${id}",
								content = """<!DOCTYPE html><html><head><style>
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
                                            </style></head><body>$content</body></html>""".trimIndent()
						)
				)
			}
		}
	}

	ActivityPager(
			title = stringResource(id = R.string.academic_affair_notice),
			tabs = mutableListOf(
					MenuItem(stringResource(id = R.string.academic_affair_notice)),
					MenuItem(stringResource(id = R.string.school_affair_notice))
			),
			isTopBarContentFixed = true,
			sharedKey = "AcademyNotification",
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			onNavigationClick = { backStack.navigateBack(activity) },
			topBarContent = {
				val setQuery = when (it) {
					0 -> textFieldState1::setTextAndPlaceCursorAtEnd
					else -> textFieldState2::setTextAndPlaceCursorAtEnd
				}

				fun getQuery(): TextFieldState = when (it) {
					0 -> textFieldState1
					else -> textFieldState2
				}

				val searchBarState = rememberSearchBarState()
				SearchBarDefaults.InputField(
						textFieldState = getQuery(),
						searchBarState = searchBarState,
						onSearch = {},
						placeholder = {
							Text(
									modifier = Modifier.clearAndSetSemantics {},
									text = stringResource(R.string.search)
							)
						},
						leadingIcon = {
							IconButton(onClick = {}) {
								Icon(
										Icons.Rounded.Search,
										contentDescription = stringResource(R.string.search)
								)
							}
						},
						trailingIcon = {
							if (getQuery().text.isNotEmpty()) IconButton(onClick = {
								setQuery("")
							}) {
								Icon(
										Icons.Rounded.Close,
										contentDescription = stringResource(R.string.clear)
								)
							}
						},
				)
//			AppBarWithSearch(
//				scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(),
//				windowInsets = SearchBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
//				state = searchBarState,
//				colors = appBarWithSearchColors(
//					appBarContainerColor = Color.Transparent,
//				),
//				inputField = @Composable {
//					SearchBarDefaults.InputField(
//						textFieldState = getQuery(),
//						searchBarState = searchBarState,
//						onSearch = {},
//						placeholder = {
//							Text(
//								modifier = Modifier.clearAndSetSemantics {},
//								text = stringResource(R.string.search)
//							)
//						},
//						leadingIcon = {
//							IconButton(onClick = {
//
//							}) {
//								Icon(
//									Icons.Rounded.Search,
//									contentDescription = stringResource(R.string.search)
//								)
//							}
//						},
//						trailingIcon = {
//							if (getQuery().text.isNotEmpty()) IconButton(onClick = {
//								setQuery("")
//							}) {
//								Icon(
//									Icons.Rounded.Close,
//									contentDescription = stringResource(R.string.clear)
//								)
//							}
//						},
//					)
//				},
//			)
			},
			pageContent = {
				if (it == 0) StatePage(
						state = academicNoticesUiState
				) {
					NewsList(
							newsList = academicNotices,
							sharedTransitionScope = sharedTransitionScope,
							animatedVisibilityScope = animatedVisibilityScope
					) { notice ->
						viewModel.fetchContent(notice.getString("id"))
					}
				} else if (it == 1) StatePage(
						state = schoolNoticesUiState
				) {
					NewsList(
							newsList = schoolNotices,
							sharedTransitionScope = sharedTransitionScope,
							animatedVisibilityScope = animatedVisibilityScope
					) { notice ->
						viewModel.fetchContent(notice.getString("id"))
						id = notice.getString("id")
					}
				}
			})
}

@Composable
fun NewsList(
	newsList: List<JSONObject>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
	onItemClick: (JSONObject) -> Unit,
) {
	LazyVerticalStaggeredGrid(
			columns = StaggeredGridCells.Adaptive(240.dp),
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(dimensionResource(R.dimen.content_padding)),
			horizontalArrangement = Arrangement.spacedBy(dimensionResource(horizontal_padding)),
			verticalItemSpacing = dimensionResource(vertical_padding)
	) {
		items(newsList) { item ->
			Card(
					onClick = { onItemClick(item) },
					modifier = (if (sharedTransitionScope != null && animatedVisibilityScope != null) {
						with(sharedTransitionScope) {
							Modifier.sharedBounds(
									sharedContentState = rememberSharedContentState(
											key = "https://jwxt.sysu.edu.cn/jwxt/#/notice/${
												item.getString(
														"id"
												)
											}"
									),
									animatedVisibilityScope = animatedVisibilityScope,
							)
						}
					} else Modifier).fillMaxWidth()) {
				Column(
						modifier = Modifier
							.padding(
									dimensionResource(horizontal_padding),
									dimensionResource(vertical_padding)
							)
							.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(
						dimensionResource(vertical_padding)
				)
				) {
					Text(
							text = item.getString("title", ""),
							style = MaterialTheme.typography.titleMedium
					)
					Text(
							text = item.getString("deliveryDate", ""),
							style = MaterialTheme.typography.bodySmall,
					)
				}
			}
		}
	}
}

