package com.sysu.edu.browser

import android.content.ClipData
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation3.runtime.NavKey
import com.halilibo.richtext.ui.material3.RichText
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownTable
import com.mikepenz.markdown.compose.elements.MarkdownTableHeader
import com.mikepenz.markdown.compose.elements.MarkdownTableRow
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import com.sysu.edu.R
import com.sysu.edu.api.DataStoreManager
import com.sysu.edu.nav.RichText
import com.sysu.edu.nav.navigateBack
import com.sysu.edu.view.ActivityPager
import kotlinx.coroutines.launch

@Composable
fun RichTextRoute(
	backStack: MutableList<NavKey>,
	navKey: RichText? = backStack.lastOrNull() as? RichText,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	var content by remember { mutableStateOf(navKey?.content ?: "") }
	val activity = LocalActivity.current
	val clipboard = LocalClipboard.current
	val coroutine = rememberCoroutineScope()
	val title = navKey?.title ?: ""
	val contentType = navKey?.contentType ?: DataStoreManager.ContentType.MARKDOWN.name
	if (content.isEmpty()) DataStoreManager.loadContent(LocalContext.current, title) {
		content = it
	}
	ActivityPager(
		title = title,
		onNavigationClick = { backStack.navigateBack(activity) },
		pageContent = {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(
						dimensionResource(R.dimen.horizontal_padding),
						dimensionResource(R.dimen.vertical_padding)
					)
			) {
				if (contentType == DataStoreManager.ContentType.MARKDOWN.name) Markdown(
					markdownState = rememberMarkdownState(content),
					colors = markdownColor(),
					typography = markdownTypography(),
					modifier = Modifier.fillMaxWidth(),
					components = markdownComponents(table = { model ->
						MarkdownTable(
							content = model.content,
							node = model.node,
							style = model.typography.table,
							headerBlock = { content, header, tableWidth, style ->
								MarkdownTableHeader(
									content = content,
									header = header,
									tableWidth = tableWidth,
									style = style,
									maxLines = Int.MAX_VALUE,
									overflow = TextOverflow.Clip,
								)
							},
							rowBlock = { content, row, tableWidth, style ->
								MarkdownTableRow(
									content = content,
									header = row,
									tableWidth = tableWidth,
									style = style,
									maxLines = Int.MAX_VALUE,
									overflow = TextOverflow.Clip,
								)
							},
						)
					}),
				)
				else RichText {
					Text(AnnotatedString(content))
				}
			}
		},
		floatingActionButton = {
			SmallExtendedFloatingActionButton(icon = {
				Icon(Icons.Filled.ContentCopy, stringResource(R.string.copy))
			}, text = { Text(stringResource(R.string.copy)) }, onClick = {
				coroutine.launch {
					clipboard.setClipEntry(
						ClipData.newPlainText(title, content).toClipEntry()
					)
				}
			})
		},
		sharedTransitionScope = sharedTransitionScope,
		animatedVisibilityScope = animatedVisibilityScope
	)
}