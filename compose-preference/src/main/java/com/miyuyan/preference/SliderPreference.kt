package com.miyuyan.preference

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import java.util.Locale
import kotlin.math.roundToInt

/** 浮点值转摘要文本：整值不带小数，否则保留 1 位小数。 */
internal fun Float.sliderText(): String =
	if (this == roundToInt().toFloat()) roundToInt().toString()
	else String.format(Locale.getDefault(), "%.1f", this)

/**
 * 统一的滑杆拇指尺寸：expressive 默认是 4x44dp 的竖长胶囊，这里把高度压到 28dp，
 * [SliderPreference] 与 [RangeSliderPreference] 共用。
 */
internal val SliderThumbSize = DpSize(4.dp, 28.dp)

/**
 * 数值滑杆偏好项：拖动滑杆即时调整一个数值。
 *
 * 外观与 [Preference] 一致（同一分组的圆角衔接），标题位于上方，全宽滑杆位于下方，
 * 尾部实时显示当前值（拖动过程中跟随变化）。拖动结束时可经 [onValueChangeFinished]
 * 做持久化等收尾操作。
 *
 *
 */
@Composable
fun SliderPreference(
	title: String,
	onValueChange: (Float) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	valueText: String? = null,
	onValueChangeFinished: (() -> Unit)? = null,
	state: SliderState,
) {
	Preference(
			onClick = null,
			title = title,
			enabled = enabled,
			modifier = modifier,
			icon = icon,
			trailing = {
				Text(
						valueText ?: state.value.sliderText(),
						style = MaterialTheme.typography.labelMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			},
			content = {
				val interactionSource = remember { MutableInteractionSource() }
				Slider(
						state = state,
						onValueChange = onValueChange,
						modifier = Modifier.fillMaxWidth(),
						enabled = enabled,
						onValueChangeFinished = onValueChangeFinished,
						colors = SliderDefaults.colors(),
						interactionSource = interactionSource,
						thumb = {
							SliderDefaults.Thumb(
									interactionSource = interactionSource,
									colors = SliderDefaults.colors(),
									enabled = enabled,
									thumbSize = SliderThumbSize,
							)
						},
				)
			})
}

/**
 * 自动管理数值状态的 [SliderPreference] 重载:[initialValue] 作为初始值,
 * 拖动时更新内部状态并经 [onValueChange] 通知父级。
 *
 * 传入 [key] 时数值经 [rememberPreference] 存入 DataStore——拖动过程中仅更新滑杆,
 * 松手([onValueChangeFinished])时一次性落盘;DataStore 异步加载完成后自动同步到滑杆。
 *
 * @param valueText 数值格式化函数，传入当前值返回尾部文本；为 null 时自动格式化。
 */
@Composable
fun SliderPreference(
	title: String,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
	steps: Int = 0,
	initialValue: Float = valueRange.start,
	key: Preferences.Key<Float>? = null,
	valueText: ((Float) -> String)? = null,
	onValueChange: ((Float) -> Unit)? = null,
	onValueChangeFinished: (() -> Unit)? = null,
) {
	val stored = key?.let { rememberPreference(it, initialValue) }
	val state = rememberSliderState(
		value = stored?.value ?: initialValue,
		steps = steps,
		trackRange = valueRange,
	)

	// DataStore 异步加载完成(或被外部修改)后同步到滑杆
	LaunchedEffect(stored?.value) {
		val persisted = stored?.value ?: return@LaunchedEffect
		if (state.value != persisted) state.value = persisted
	}

	SliderPreference(
			title = title,
			onValueChange = {
				state.value = it
				onValueChange?.invoke(it)
			},
			modifier = modifier,
			enabled = enabled,
			icon = icon,
			valueText = valueText?.invoke(state.value),
			state = state,
			onValueChangeFinished = {
				// 拖动过程中不逐帧写 DataStore,松手时一次性落盘
				stored?.value = state.value
				onValueChangeFinished?.invoke()
			},
	)
}
