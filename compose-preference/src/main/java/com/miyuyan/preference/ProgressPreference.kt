package com.miyuyan.preference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 进度偏好项：展示一个任务的进行状态（下载、同步、清理缓存等）。
 *
 * 外观与 [Preference] 一致（同一分组的圆角衔接），标题位于上方，summary 文本与
 * 全宽的进度条位于下方，尾部默认显示百分比。状态完全由 [progress] 驱动，
 * 组件不持有状态。
 *
 * @param progress 进度值，0f..1f，超出范围会被钳制；传 null 显示不定进度的循环动画。
 * @param summary 进度条上方的说明文本（如 "12.3 MB / 45.6 MB"）；为 null 时只显示进度条。
 * @param trailing 自定义尾部 Composable；为 null 且 [progress] 非 null 时默认显示百分比文本，
 *                  [progress] 为 null 时尾部为空。
 */
@Composable
fun ProgressPreference(
	title: String,
	progress: Float?,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	summary: String? = null,
	trailing: (@Composable () -> Unit)? = null,
) {
	Preference(
			onClick = null,
			title = title,
			enabled = enabled,
			modifier = modifier,
			icon = icon,
			trailing = {
				if (trailing != null) {
					trailing()
				} else if (progress != null) {
					Text(
							"${(progress.coerceIn(0f, 1f) * 100).roundToInt()}%",
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
			},
			content = {
				Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
					if (summary != null) {
						Text(
								summary,
								style = MaterialTheme.typography.bodyMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
					if (progress == null) {
						LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
					} else {
						LinearProgressIndicator(
								progress = { progress.coerceIn(0f, 1f) },
								modifier = Modifier.fillMaxWidth(),
						)
					}
				}
			})
}
