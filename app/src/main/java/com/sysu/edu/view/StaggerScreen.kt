package com.sysu.edu.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sysu.edu.R

data class RowData(
	val key: String?,
	var value: String?,
	var onClick: (() -> Unit)? = null,
                  )

enum class RowOrientation { Horizontal, Vertical }
data class SectionData(
	val title: String?,
	val icon: Int? = null,
	val rows: SnapshotStateList<RowData> = mutableStateListOf(),
	val rowOrientation: RowOrientation = RowOrientation.Horizontal,
	val footerMenus: SnapshotStateList<MenuItem> = mutableStateListOf(),
	var footer: (@Composable ColumnScope.() -> Unit)? = null,
                      )

@Composable fun SectionCard(
	section: SectionData,
	isExpandable: Boolean = true,
	defaultExpanded: Boolean = true,
	isHideNull: Boolean = false,
                           ) {
	var expanded by rememberSaveable { mutableStateOf(defaultExpanded) }
	
	ElevatedCard(modifier = Modifier.fillMaxWidth()) {
		Column(modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = dimensionResource(R.dimen.vertical_padding))) {
			Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
				.fillMaxWidth()
				.clickable(enabled = isExpandable) {
					expanded = !expanded
				}
				.padding(horizontal = dimensionResource(R.dimen.horizontal_padding), vertical = dimensionResource(R.dimen.vertical_padding))) {
				section.icon?.let {
					Icon(painter = painterResource(id = it), contentDescription = null, modifier = Modifier.size(dimensionResource(R.dimen.icon_size)), tint = MaterialTheme.colorScheme.primary)
					Spacer(modifier = Modifier.width(dimensionResource(R.dimen.icon_text_gap)))
				}
				section.title?.let { Text(text = it, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)) }
				if (isExpandable) {
					val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "ExpandIconRotation")
					Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = if (expanded) "折叠" else "展开", modifier = Modifier.rotate(rotation))
				}
			}
			AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
				Column {
					section.rows.forEach { row ->
						if (!isHideNull || !row.value.isNullOrEmpty()) {
							KeyValueRow(row, section.rowOrientation)
						}
					}
				}
			}
			
			if (section.footerMenus.isNotEmpty()) {
				Row(modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = dimensionResource(R.dimen.horizontal_padding), vertical = dimensionResource(R.dimen.vertical_padding)),
				    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin))) {
					section.footerMenus.forEach { item ->
						FilledTonalButton(onClick = { item.onClick() }, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes(), enabled = item.enabled) {
							item.icon?.let {
								Icon(it, contentDescription = item.title, modifier = Modifier.size(dimensionResource(R.dimen.icon_size)))
								Spacer(modifier = Modifier.width(dimensionResource(R.dimen.icon_text_gap)))
							}
							item.title?.let {
								Text(it)
							}
						}
					}
				}
			}
			section.footer?.let {
				Column(modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = dimensionResource(R.dimen.horizontal_padding), vertical = dimensionResource(R.dimen.vertical_padding)), content = it)
			}
		}
	}
}

@Composable fun KeyValueRow(row: RowData, orientation: RowOrientation = RowOrientation.Horizontal) {
	val context = LocalContext.current
	val modifier = Modifier
		.fillMaxWidth()
		.clickable {
			if (row.onClick != null) {
				row.onClick?.invoke()
			}
			else {
				val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
				clip.setPrimaryClip(ClipData.newPlainText(row.key, row.value))
			}
		}
		.padding(horizontal = dimensionResource(R.dimen.horizontal_padding), vertical = dimensionResource(R.dimen.vertical_padding))
	if (orientation == RowOrientation.Horizontal) {
		Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
			SelectionContainer {
				Text(text = row.key ?: "", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
			}
			if (row.value != null && row.key != null) Spacer(modifier = Modifier.width(dimensionResource(R.dimen.horizontal_margin)))
			SelectionContainer {
				Text(text = row.value ?: "", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.primary)
			}
		}
	}
	else {
		Column(modifier = modifier) {
			SelectionContainer {
				Text(text = row.key ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
			}
			if (row.value != null && row.key != null) Spacer(modifier = Modifier.height(dimensionResource(R.dimen.vertical_margin)))
			SelectionContainer {
				Text(text = row.value ?: "", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth())
			}
		}
	}
}

@Composable fun StaggerScreen(
	sections: SnapshotStateList<SectionData> = mutableStateListOf(),
	isHideNull: Boolean = false,
	isNestedEnabled: Boolean = true,
	onScrollBottom: (() -> Unit)? = null,
	onScrollTopChanged: ((Boolean) -> Unit)? = null,
                             ) {
	val state = rememberLazyStaggeredGridState()
	val isTop = remember { derivedStateOf { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 } }
	
	LaunchedEffect(isTop.value) {
		onScrollTopChanged?.invoke(isTop.value)
	}
	val canScrollForward = remember { derivedStateOf { state.canScrollForward } }
	LaunchedEffect(canScrollForward.value) {
		if (!canScrollForward.value && sections.isNotEmpty()) {
			onScrollBottom?.invoke()
		}
	}
	val nestedScrollConnection = rememberNestedScrollInteropConnection()
	if (!isNestedEnabled) {
		FlowRow(modifier = Modifier
			.fillMaxWidth()
			.padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding)), verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin))) {
			sections.forEach { section ->
				SectionCard(section, isHideNull = isHideNull)
			}
		}
		return
	}
	
	LazyVerticalStaggeredGrid(state = state,
	                          columns = StaggeredGridCells.Adaptive(240.dp),
	                          modifier = Modifier
		                          .fillMaxSize()
		                          .nestedScroll(nestedScrollConnection),
	                          contentPadding = PaddingValues(dimensionResource(R.dimen.horizontal_margin)),
	                          verticalItemSpacing = dimensionResource(R.dimen.vertical_margin),
	                          horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap))) {
		itemsIndexed(sections) { _, section ->
			SectionCard(section, isHideNull = isHideNull)
		}
	}
}

fun List<SectionData>.toMarkdown(): String {
	val markdown = StringBuilder()
	var keys: List<String?> = emptyList()
	forEachIndexed { i, section ->
		val sectionKeys = section.rows.map { it.key }
		if (keys.isEmpty() || !sectionKeys.containsAll(keys)) {
			keys = sectionKeys
			markdown.append("\n").append("序号 | ").append(keys.joinToString(" | ") { it?.trim() ?: "" }).append("\n").append("--- | ".repeat(keys.size + 1)).append("\n")
		}
		markdown.append(i + 1).append(" | ").append(section.rows.map { it.value }.joinToString(" | ") { it?.trim() ?: "" }).append("\n")
	}
	return "$markdown"
}

fun List<SectionData>.toHtml(): String {
	val html = StringBuilder()
	html.append("<table border=\"1\" style=\"border-collapse: collapse; width: 100%;\">")
	var keys: List<String?> = emptyList()
	
	forEachIndexed { i, section ->
		val sectionKeys = section.rows.map { it.key }
		if (keys.isEmpty() || !sectionKeys.containsAll(keys)) {
			keys = sectionKeys
			html.append("<tr><th>序号</th>")
			keys.forEach { key ->
				html.append("<th>").append(key?.trim() ?: "").append("</th>")
			}
			html.append("</tr>")
		}
		html.append("<tr><td align=\"center\">").append(i + 1).append("</td>")
		section.rows.forEach { row ->
			html.append("<td>").append(row.value?.trim() ?: "").append("</td>")
		}
		html.append("</tr>")
	}
	html.append("</table>")
	return "$html"
}