package com.miyuyan.preference

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.RangeSliderState
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberRangeSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * 区间滑杆偏好项：拖动双端滑杆调整一个数值区间（如时间段、价格段筛选）。
 *
 * 外观与 [Preference] 一致（同一分组的圆角衔接），标题位于上方，全宽双端滑杆位于下方，
 * 尾部实时显示当前区间（如 "1~17"）。拖动结束时可经 [onValueChangeFinished]
 * 做持久化等收尾操作。
 *
 *              0 表示连续取值。
 * @param valuesText 尾部区间文本；为 null 时自动格式化为 "start~end"
 *                   （整值不带小数，否则保留 1 位小数）。
 */
@Composable
fun RangeSliderPreference(
	title: String,
	onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	state: RangeSliderState,
	valuesText: String? = null,
	onValueChangeFinished: (() -> Unit)? = null,
) {
	Preference(
			onClick = null,
			title = title,
			enabled = enabled,
			modifier = modifier,
			icon = icon,
			trailing = {
				Text(
						valuesText
							?: "${state.startValue.sliderText()}~${state.endValue.sliderText()}",
						style = MaterialTheme.typography.labelMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			},
			content = {
				val startThumbInteractionSource = remember { MutableInteractionSource() }
				val endThumbInteractionSource = remember { MutableInteractionSource() }
				RangeSlider(
						state = state,
						onValueChange = onValueChange,
						modifier = Modifier.fillMaxWidth(),
						enabled = enabled,
						onValueChangeFinished = onValueChangeFinished,
						colors = SliderDefaults.colors(),
						startThumbInteractionSource = startThumbInteractionSource,
						endThumbInteractionSource = endThumbInteractionSource,
						startThumb = {
							SliderDefaults.Thumb(
									interactionSource = startThumbInteractionSource,
									colors = SliderDefaults.colors(),
									enabled = enabled,
									thumbSize = SliderThumbSize,
							)
						},
						endThumb = {
							SliderDefaults.Thumb(
									interactionSource = endThumbInteractionSource,
									colors = SliderDefaults.colors(),
									enabled = enabled,
									thumbSize = SliderThumbSize,
							)
						},
				)
			})
}

/**
 * 自动管理区间状态的 [RangeSliderPreference] 重载：[initialValues] 作为初始区间，
 * 拖动时更新内部状态并经 [onValueChange] 通知父级。
 *
 * @param valuesText 区间格式化函数，传入当前区间返回尾部文本；为 null 时自动格式化。
 */
@Composable
fun RangeSliderPreference(
	title: String,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
	steps: Int = 0,
	initialValues: ClosedFloatingPointRange<Float> = valueRange,
	valuesText: @Composable ((ClosedFloatingPointRange<Float>) -> String)? = null,
	onValueChange: ((ClosedFloatingPointRange<Float>) -> Unit)? = null,
	onValueChangeFinished: (() -> Unit)? = null,
) {
	val state = rememberRangeSliderState(
			initialValues.start, initialValues.endInclusive, steps = steps, trackRange = valueRange
	)
	RangeSliderPreference(
			title = title,
			onValueChange = {
				state.apply {
					startValue = it.start
					endValue = it.endInclusive
				}
				onValueChange?.invoke(it)
			},
			modifier = modifier,
			enabled = enabled,
			icon = icon,
			state = state,
			valuesText = valuesText?.invoke(state.startValue..state.endValue),
			onValueChangeFinished = onValueChangeFinished,
	)
}
