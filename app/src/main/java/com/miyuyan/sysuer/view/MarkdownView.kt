package com.miyuyan.sysuer.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownTable
import com.mikepenz.markdown.compose.elements.MarkdownTableHeader
import com.mikepenz.markdown.compose.elements.MarkdownTableRow
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import com.miyuyan.sysuer.api.SettingManager
import com.miyuyan.sysuer.theme.SysuerTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MarkdownView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
	private val composeView = ComposeView(context)
	private val content = MutableStateFlow("")

	init {
		addView(composeView)
		val settingManager = SettingManager(context)
		composeView.setContent({
			SysuerTheme(
					darkTheme = when (settingManager.getTheme()) {
						0 -> false
						1 -> true
						else -> isSystemInDarkTheme()
					}
			) {
				val markdownText by content.collectAsStateWithLifecycle()
				CompositionLocalProvider(LocalInspectionMode provides true) {
					Markdown(
							rememberMarkdownState(markdownText),
							colors = markdownColor(),
							typography = markdownTypography(
									h1 = MaterialTheme.typography.headlineMedium,
									h2 = MaterialTheme.typography.titleLargeEmphasized,
									h3 = MaterialTheme.typography.titleMediumEmphasized
							),
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
				}

			}
		})
	}

	/**
	 * 供 Java 代码调用的方法：设置并渲染 Markdown 文本
	 */
	fun setMarkdown(text: String, isHtml: Boolean = false) {
		content.value = if (isHtml) {
			AnnotatedString.fromHtml(text).text
		} else {
			text
		}
	}

}