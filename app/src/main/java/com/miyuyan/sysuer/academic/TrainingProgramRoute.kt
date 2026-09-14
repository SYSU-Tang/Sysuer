package com.miyuyan.sysuer.academic

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.nav.PersonalTrainingProgram
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.StaggerScreen
import com.miyuyan.sysuer.view.exportMarkdownMenuItem
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.squircle.squircleClip
import kotlin.math.abs

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TrainingProgramRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: TrainingProgramViewModel = viewModel()
	val showResults by viewModel.showResults.collectAsState()
	val resultSections by viewModel.resultSections.collectAsState()
	val viewDetailProgramId by viewModel.viewDetailProgramId.collectAsState()

	LaunchedEffect(viewDetailProgramId) {
		viewDetailProgramId?.let {
			backStack.add(PersonalTrainingProgram(it))
			viewModel.clearViewDetailProgramId()
		}
	}

	BackHandler(showResults) {
		viewModel.navigateBack()
	}

	ActivityPager(
		title = stringResource(if (showResults) R.string.result else R.string.training_program_query),
		onNavigationClick = {
			if (showResults) viewModel.navigateBack()
			else backStack.navigateBack()
		},
		isNestedScrollEnabled = false,
		sharedTransitionScope = sharedTransitionScope,
		animatedVisibilityScope = animatedVisibilityScope,
		sharedKey = "TrainingProgram",
		topBarMenus = {
			if (showResults) listOf(
				exportMarkdownMenuItem(
					backStack,
					resultSections,
					stringResource(R.string.training_program_query),
					stringResource(R.string.training_program_query)
				)
			)
			else emptyList()
		}) {
		SharedTransitionLayout {
			AnimatedContent(
				targetState = showResults, label = "query_to_result", transitionSpec = {
					fadeIn() togetherWith fadeOut()
				}) { showResultsState ->
				if (showResultsState) {
					Box(
						modifier = Modifier
							.fillMaxSize()
							.sharedBounds(
								sharedContentState = rememberSharedContentState(key = "query_button"),
								animatedVisibilityScope = this@AnimatedContent,
							)
					) {
						StaggerScreen(
							sections = resultSections,
							onScrollBottom = { viewModel.loadMore() },
							sharedTransitionScope = sharedTransitionScope,
							animatedVisibilityScope = animatedVisibilityScope,
						)
					}
				} else {
					TrainingProgramForm(
						viewModel, this@SharedTransitionLayout, this@AnimatedContent
					)
				}
			}
		}
	}
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TrainingProgramForm(
	viewModel: TrainingProgramViewModel,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState())
			.padding(
				horizontal = dimensionResource(R.dimen.horizontal_margin),
				vertical = dimensionResource(R.dimen.vertical_padding)
			),
		verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin))
	) {
		Text(text = stringResource(R.string.college))
		CollegeDropdown(viewModel)

		Text(text = stringResource(R.string.grade))
		GradePicker(viewModel)

		Text(text = stringResource(R.string.profession))
		ProfessionDropdown(viewModel)

		Text(text = stringResource(R.string.type))
		TypeChips(viewModel)

		HorizontalDivider(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.vertical_margin)))

		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin))
		) {
			OutlinedButton(
				onClick = { viewModel.reset() },
				modifier = Modifier.weight(1f),
				shapes = ButtonDefaults.shapes()
			) {
				Text(stringResource(R.string.reset))
			}
			FilledTonalButton(
				onClick = { viewModel.query() },
				modifier = Modifier
					.weight(1f)
					.then(
						if (sharedTransitionScope != null && animatedVisibilityScope != null) with(
						sharedTransitionScope
					) {
						Modifier.sharedBounds(
							sharedContentState = rememberSharedContentState(key = "query_button"),
							animatedVisibilityScope = animatedVisibilityScope
						)
					}
					else Modifier),
				shapes = ButtonDefaults.shapes()) {
				Text(stringResource(R.string.query))
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollegeDropdown(viewModel: TrainingProgramViewModel) {
	var expanded by remember { mutableStateOf(false) }
	val collegeNames by viewModel.collegeNames.collectAsState()
	val selectedCollegeName by viewModel.selectedCollegeName.collectAsState()
	ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
		OutlinedTextField(
			value = selectedCollegeName,
			onValueChange = {
				viewModel.updateSelectedCollegeName(it)
				viewModel.fetchColleges(it)
				viewModel.next()
				expanded = true
			},
			label = { Text(stringResource(R.string.college)) },
			trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
			modifier = Modifier
				.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
				.fillMaxWidth(),
			singleLine = true
		)
		ExposedDropdownMenu(
			expanded = expanded, onDismissRequest = { expanded = false }) {
			collegeNames.forEachIndexed { index, name ->
				DropdownMenuItem(text = { Text(name) }, onClick = {
					viewModel.onCollegeSelected(index)
					expanded = false
				})
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfessionDropdown(viewModel: TrainingProgramViewModel) {
	var expanded by remember { mutableStateOf(false) }
	val professionNames by viewModel.professionNames.collectAsState()
	val selectedProfessionName by viewModel.selectedProfessionName.collectAsState()
	ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
		OutlinedTextField(
			value = selectedProfessionName,
			onValueChange = {
				viewModel.updateSelectedProfessionName(it)
				viewModel.fetchProfessions(it)
				viewModel.next()
				expanded = true
			},
			label = { Text(stringResource(R.string.profession)) },
			trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
			modifier = Modifier
				.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
				.fillMaxWidth(),
			singleLine = true
		)
		ExposedDropdownMenu(
			expanded = expanded, onDismissRequest = { expanded = false }) {
			professionNames.forEachIndexed { index, name ->
				DropdownMenuItem(text = { Text(name) }, onClick = {
					viewModel.onProfessionSelected(index)
					expanded = false
				})
			}
		}
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GradePicker(
	viewModel: TrainingProgramViewModel
) {
	val gradeNames by viewModel.gradeNames.collectAsState()
	val selectedGradeIndex by viewModel.selectedGradeIndex.collectAsState()

	if (gradeNames.isEmpty()) return

	val listState = rememberLazyListState(
		initialFirstVisibleItemIndex = selectedGradeIndex
	)

	val snapBehavior = rememberSnapFlingBehavior(
		lazyListState = listState
	)

	val scope = rememberCoroutineScope()

	/*
	 * 当前真正吸附到中间的 item
	 */
	val centerIndex by remember {
		derivedStateOf {
			val layoutInfo = listState.layoutInfo

			val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2

			layoutInfo.visibleItemsInfo.minByOrNull {
				abs(
					(it.offset + it.size / 2) - viewportCenter
				)
			}?.index
		}
	}

	/*
	 * Pager -> ViewModel
	 */
	LaunchedEffect(centerIndex) {
		centerIndex?.let { index ->
			if (index in gradeNames.indices && index != selectedGradeIndex) {
				viewModel.onGradeSelected(index)
			}
		}
	}

	/*
	 * ViewModel -> Picker
	 */
	LaunchedEffect(selectedGradeIndex) {
		if (selectedGradeIndex in gradeNames.indices && selectedGradeIndex != centerIndex) {
			scope.launch {
				listState.animateScrollToItem(
					index = selectedGradeIndex
				)
			}
		}
	}

	val itemHeight = 44.dp

	Box(
		modifier = Modifier
			.fillMaxWidth()
			.height(itemHeight * 3),
		contentAlignment = Alignment.Center
	) {

		/*
		 * iOS Picker 中间选中区域
		 */
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(itemHeight)
				.squircleClip(itemHeight / 2)
				.background(
					MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.64f)
				)
		)

		LazyColumn(
			state = listState,
			flingBehavior = snapBehavior,
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(
				vertical = itemHeight
			),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			itemsIndexed(gradeNames) { index, name ->
				val distanceFromCenter = remember {
					derivedStateOf {
						val layoutInfo = listState.layoutInfo
						val viewportCenter =
							(layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
						layoutInfo.visibleItemsInfo.firstOrNull {
							it.index == index
						}?.let { item ->
							abs(
								item.offset + item.size / 2 - viewportCenter
							).toFloat() / item.size
						} ?: 3f
					}
				}

				val distance = distanceFromCenter.value.coerceIn(0f, 2f)
				val scale = 1f - distance * 0.16f
				val alpha = 1f - distance * 0.30f
				val selected = index == centerIndex

				Box(
					modifier = Modifier
						.fillMaxWidth()
						.height(itemHeight)
						.graphicsLayer {
							scaleX = scale
							scaleY = scale
							this.alpha = alpha
						}
						.squircleClip(itemHeight / 2)
						.clickable {
							scope.launch {
								listState.animateScrollToItem(
									index = index
								)
							}
						}, contentAlignment = Alignment.Center
				) {
					Text(
						text = name,
						style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
						fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
						color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
						textAlign = TextAlign.Center
					)
				}
			}
		}
	}
}

@Composable
private fun TypeChips(viewModel: TrainingProgramViewModel) {
	val typeNames by viewModel.typeNames.collectAsState()
	val typeIds by viewModel.typeIds.collectAsState()
	val selectedTypeId by viewModel.selectedTypeId.collectAsState()
	FlowRow(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap))) {
		typeNames.forEachIndexed { index, name ->
			FilterChip(
				selected = typeIds.getOrNull(index) == selectedTypeId,
				onClick = { viewModel.onTypeSelected(index) },
				label = { Text(name) })
		}
	}
}