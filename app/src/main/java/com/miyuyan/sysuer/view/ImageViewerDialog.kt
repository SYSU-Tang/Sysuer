package com.miyuyan.sysuer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.miyuyan.sysuer.R

/**
 * 全屏图片查看弹窗。
 *
 * 手势:
 * - 双指缩放(1x–8x) + 双指自由旋转 + 拖拽平移
 * - 双击在 1x / 2.5x 间切换,回到 1x 时同时复位旋转与位移
 * - 未放大时单点图片/背景关闭弹窗(放大后单点不关闭,避免误触)
 *
 * 底部提供「打开为 / 分享 / 保存到」三个操作,具体行为由调用方回调决定。
 *
 * @param model       Coil 可加载的模型(本地 [java.io.File]、Uri、路径等)
 * @param fileName    顶部展示的文件名,可为 null
 * @param onDismiss   关闭回调
 * @param onOpenAs    「打开为」回调
 * @param onShare     「分享」回调
 * @param onSave      「保存到」回调
 */
@Composable
fun ImageViewerDialog(
	model: Any?,
	fileName: String? = null,
	contentDescription: String? = null,
	onDismiss: () -> Unit,
	onOpenAs: () -> Unit,
	onShare: () -> Unit,
	onSave: () -> Unit,
) {
	Dialog(
			onDismissRequest = onDismiss,
			properties = DialogProperties(usePlatformDefaultWidth = false),
	) {
		// 切换图片时重置姿态
		key(model) {
			var scale by remember { mutableFloatStateOf(1f) }
			var rotation by remember { mutableFloatStateOf(0f) }
			var offset by remember { mutableStateOf(Offset.Zero) }
			var containerSize by remember { mutableStateOf(IntSize.Zero) }

			// 放大后把拖动范围钳制在溢出部分的一半内,避免图片被拖出视口
			fun clampOffset(candidate: Offset, currentScale: Float): Offset {
				if (currentScale <= 1f) return Offset.Zero
				val maxX = (containerSize.width * (currentScale - 1f)) / 2f
				val maxY = (containerSize.height * (currentScale - 1f)) / 2f
				return Offset(
						candidate.x.coerceIn(-maxX, maxX),
						candidate.y.coerceIn(-maxY, maxY),
				)
			}

			fun reset() {
				scale = 1f
				rotation = 0f
				offset = Offset.Zero
			}

			Box(
					modifier = Modifier
						.fillMaxSize()
						.background(Color.Black)
						.onSizeChanged { containerSize = it }
						.pointerInput(Unit) {
							detectTransformGestures(panZoomLock = false) { _, pan, zoom, rotationDelta ->
								scale = (scale * zoom).coerceIn(1f, 8f)
								rotation += rotationDelta
								offset = clampOffset(offset + pan, scale)
							}
						}
						.pointerInput(Unit) {
							detectTapGestures(
									onTap = {
										// 仅默认姿态下允许单点关闭,放大后避免误触
										if (scale <= 1f) onDismiss()
									},
									onDoubleTap = {
										if (scale > 1f) reset() else scale = 2.5f
									},
							)
						},
			) {
				AsyncImage(
						model = model,
						contentDescription = contentDescription ?: fileName,
						modifier = Modifier
							.fillMaxSize()
							.graphicsLayer {
								scaleX = scale
								scaleY = scale
								rotationZ = rotation
								translationX = offset.x
								translationY = offset.y
							},
						contentScale = ContentScale.Fit,
				)

				Row(
						modifier = Modifier
							.align(Alignment.TopCenter)
							.fillMaxWidth()
							.padding(8.dp),
						verticalAlignment = Alignment.CenterVertically,
				) {
					Text(
							text = fileName.orEmpty(),
							color = Color.White,
							style = MaterialTheme.typography.bodyMedium,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis,
							modifier = Modifier.weight(1f),
					)
					IconButton(onClick = onDismiss) {
						Icon(
								Icons.Rounded.Close,
								contentDescription = stringResource(R.string.close),
								tint = Color.White,
						)
					}
				}

				HorizontalFloatingToolbar(
						expanded = false,
						modifier = Modifier
							.align(Alignment.BottomCenter)
							.offset(y = (-16).dp)
							.zIndex(1f)
				) {
//					Row(
//							modifier = Modifier
//								.fillMaxWidth()
//								.padding(horizontal = 8.dp),
//							horizontalArrangement = Arrangement.SpaceEvenly,
//					) {
//					clickableItem(
//							onClick = { /* doSomething() */ },
//							icon = {
//								Icon(
//										Icons.AutoMirrored.Rounded.OpenInNew,
//										contentDescription = "Localized description",
//								)
//							},
//							label = "Favorite",
//					)
						ImageActionButton(
								icon = Icons.AutoMirrored.Rounded.OpenInNew,
								label = stringResource(R.string.open_as),
								onClick = onOpenAs,
								modifier = Modifier.weight(1f),
						)
						ImageActionButton(
								icon = Icons.Rounded.Share,
								label = stringResource(R.string.share),
								onClick = onShare,
								modifier = Modifier.weight(1f),
						)
						ImageActionButton(
								icon = Icons.Rounded.Download,
								label = stringResource(R.string.save_to),
								onClick = onSave,
								modifier = Modifier.weight(1f),
						)
//					}
				}
			}
		}
	}
}

/** 底部操作按钮:图标在上、文字在下的等宽按钮 */
@Composable
private fun ImageActionButton(
	icon: ImageVector,
	label: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	TextButton(onClick = onClick, shapes = ButtonDefaults.shapes(), modifier = modifier) {
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
			Text(label, style = MaterialTheme.typography.labelSmall)
		}
	}
}