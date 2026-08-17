package com.sysu.edu.academic

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.DataStoreManager
import com.sysu.edu.browser.RichTextActivity
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.toMarkdown
import kotlin.math.abs

class TrainingProgramActivity : BaseActivity() {
	private val viewModel: TrainingProgramViewModel by viewModels()
	
	@OptIn(ExperimentalSharedTransitionApi::class) override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			BackHandler {
				if (viewModel.showResults) viewModel.navigateBack()
				else supportFinishAfterTransition()
			}
			LaunchedEffect(viewModel.viewDetailProgramId) {
				viewModel.viewDetailProgramId?.let {
					startActivity(Intent(this@TrainingProgramActivity, PersonalTrainingProgramActivity::class.java).putExtra("programId", it))
					viewModel.viewDetailProgramId = null
				}
			}
			SharedTransitionLayout {
				ActivityPager(title = if (viewModel.showResults) stringResource(R.string.result) else stringResource(R.string.training_program_query), onNavigationClick = {
					if (viewModel.showResults) viewModel.navigateBack()
					else supportFinishAfterTransition()
				}, isNestedScrollEnabled = false, actions = {
					if (viewModel.showResults) {
						IconButton(onClick = {
							val markdown = viewModel.resultSections.toMarkdown()
							DataStoreManager.saveContent(this@TrainingProgramActivity, getString(R.string.result), markdown) {
								startActivity(Intent(this@TrainingProgramActivity, RichTextActivity::class.java).putExtra("type", DataStoreManager.ContentType.MARKDOWN.name).putExtra("title", getString(R.string.result)))
							}
						}) {
							Icon(painter = painterResource(R.drawable.export), contentDescription = stringResource(R.string.export))
						}
					}
				}) {
					AnimatedContent(targetState = viewModel.showResults, label = "query_to_result", transitionSpec = {
						fadeIn() togetherWith fadeOut()
					}) { showResults ->
						if (showResults) with(this@SharedTransitionLayout) {
							Box(modifier = Modifier
								.fillMaxSize()
								.sharedBounds(rememberSharedContentState(key = "query_button"), animatedVisibilityScope = this@AnimatedContent)) {
								StaggerScreen(sections = viewModel.resultSections, onScrollBottom = { viewModel.loadMore() })
							}
						}
						else TrainingProgramForm(viewModel, this@SharedTransitionLayout, this@AnimatedContent)
					}
				}
			}
		}
	}
}

@OptIn(ExperimentalSharedTransitionApi::class) @Composable private fun TrainingProgramForm(viewModel: TrainingProgramViewModel, sharedTransitionScope: SharedTransitionScope, animatedVisibilityScope: AnimatedVisibilityScope) {
	Column(modifier = Modifier
		.fillMaxSize()
		.verticalScroll(rememberScrollState())
		.padding(horizontal = dimensionResource(R.dimen.horizontal_margin), vertical = dimensionResource(R.dimen.vertical_padding)),
	       verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin))) {
		Text(text = stringResource(R.string.college))
		CollegeDropdown(viewModel)
		
		Text(text = stringResource(R.string.grade))
		GradePicker(viewModel)
		
		Text(text = stringResource(R.string.profession))
		ProfessionDropdown(viewModel)
		
		Text(text = stringResource(R.string.type))
		TypeChips(viewModel)
		
		HorizontalDivider(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.vertical_margin)))
		
		with(sharedTransitionScope) {
			Row(modifier = Modifier
				.fillMaxWidth()
				.sharedBounds(rememberSharedContentState(key = "query_button"), animatedVisibilityScope = animatedVisibilityScope),
			    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin))) {
				OutlinedButton(onClick = { viewModel.reset() }, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) {
					Text(stringResource(R.string.reset))
				}
				FilledTonalButton(onClick = { viewModel.query() }, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) {
					Text(stringResource(R.string.query))
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class) @Composable private fun CollegeDropdown(viewModel: TrainingProgramViewModel) {
	var expanded by remember { mutableStateOf(false) }
	val maxMenuHeight = LocalConfiguration.current.screenHeightDp.dp * 0.5f
	
	ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
		OutlinedTextField(value = viewModel.selectedCollegeName ?: "",
		                  onValueChange = {
			                  viewModel.selectedCollegeName = it
			                  viewModel.fetchColleges(it)
			                  viewModel.next()
			                  expanded = true
		                  },
		                  label = { Text(stringResource(R.string.college)) },
		                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
		                  modifier = Modifier
			                  .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
			                  .fillMaxWidth(),
		                  singleLine = true)
		ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier
			.fillMaxWidth()
			.heightIn(max = maxMenuHeight)) {
			viewModel.collegeNames.forEachIndexed { index, name ->
				DropdownMenuItem(text = { Text(name) }, onClick = {
					viewModel.onCollegeSelected(index)
					expanded = false
				})
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class) @Composable private fun ProfessionDropdown(viewModel: TrainingProgramViewModel) {
	var expanded by remember { mutableStateOf(false) }
	val maxMenuHeight = LocalConfiguration.current.screenHeightDp.dp * 0.5f
	
	ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
		OutlinedTextField(value = viewModel.selectedProfessionName ?: "",
		                  onValueChange = {
			                  viewModel.selectedProfessionName = it
			                  viewModel.fetchProfessions(it)
			                  viewModel.next()
			                  expanded = true
		                  },
		                  label = { Text(stringResource(R.string.profession)) },
		                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
		                  modifier = Modifier
			                  .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
			                  .fillMaxWidth(),
		                  singleLine = true)
		ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier
			.fillMaxWidth()
			.heightIn(max = maxMenuHeight)) {
			viewModel.professionNames.forEachIndexed { index, name ->
				DropdownMenuItem(text = { Text(name) }, onClick = {
					viewModel.onProfessionSelected(index)
					expanded = false
				})
			}
		}
	}
}

@OptIn(ExperimentalFoundationApi::class) @Composable private fun GradePicker(viewModel: TrainingProgramViewModel) {
	if (viewModel.gradeNames.isEmpty()) return
	val pagerState = rememberPagerState(pageCount = { viewModel.gradeNames.size }, initialPage = viewModel.selectedGradeIndex.coerceIn(0, viewModel.gradeNames.lastIndex))
	
	LaunchedEffect(pagerState.currentPage) {
		viewModel.onGradeSelected(pagerState.currentPage)
	}
	LaunchedEffect(viewModel.selectedGradeIndex) {
		if (pagerState.currentPage != viewModel.selectedGradeIndex) {
			pagerState.animateScrollToPage(viewModel.selectedGradeIndex)
		}
	}
	
	Box(modifier = Modifier
		.fillMaxWidth()
		.height(120.dp), contentAlignment = Alignment.Center) {
		VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
			val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
			val scale = 1f - 0.3f * abs(offset)
			val alpha = 1f - 0.5f * abs(offset)
			Box(modifier = Modifier
				.fillMaxWidth()
				.graphicsLayer {
					scaleY = scale
					this.alpha = alpha
				}, contentAlignment = Alignment.Center) {
				Text(text = viewModel.gradeNames[page],
				     style = if (page == pagerState.currentPage) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall,
				     color = if (page == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
				     textAlign = TextAlign.Center,
				     modifier = Modifier.fillMaxWidth())
			}
		}
	}
}

@Composable private fun TypeChips(viewModel: TrainingProgramViewModel) {
	FlowRow(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin))) {
		viewModel.typeNames.forEachIndexed { index, name ->
			FilterChip(selected = viewModel.typeIds.getOrNull(index) == viewModel.selectedTypeId, onClick = { viewModel.onTypeSelected(index) }, label = { Text(name) })
		}
	}
}