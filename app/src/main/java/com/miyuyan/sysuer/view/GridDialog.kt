package com.miyuyan.sysuer.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import com.miyuyan.sysuer.R

/**
 * 网格菜单弹窗：底部弹层内以 [columns] 列网格排布 [MenuItem]，按钮铺满所在网格。
 * [orientation] 控制图标与文字的方向：[RowOrientation.Vertical] 时图标在上、文字在下
 * （正方形格子），[RowOrientation.Horizontal] 时图标在左、文字在右（横向格子）。
 * 是 View 版 [GridMenuDialog] 的 Compose 替代品。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridDialog(
	onDismissRequest: () -> Unit,
	items: List<MenuItem>,
	modifier: Modifier = Modifier,
	columns: Int = 4,
	orientation: RowOrientation = RowOrientation.Vertical,
) {
	ModalBottomSheet(onDismissRequest = onDismissRequest) {
		LazyVerticalGrid(
				columns = GridCells.Fixed(columns),
				modifier = modifier
					.fillMaxWidth()
					.padding(
							horizontal = dimensionResource(R.dimen.horizontal_margin),
							vertical = dimensionResource(R.dimen.vertical_margin)
					),
				horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_padding)),
				verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_padding)),
		) {
			items(items) { item ->
				when (orientation) {
					RowOrientation.Vertical -> TextButton(
							onClick = item.onClick,
							enabled = item.enabled,
							shapes = ButtonDefaults.shapes(),
							modifier = Modifier.fillMaxSize()
					) {
						Column(
								modifier = Modifier.fillMaxSize(),
								horizontalAlignment = Alignment.CenterHorizontally,
								verticalArrangement = Arrangement.Center,
						) {
							item.icon?.invoke()
							Spacer(modifier = Modifier.height(ButtonDefaults.IconSpacing))
							Text(
									text = item.title ?: "",
									style = MaterialTheme.typography.bodyMedium,
									textAlign = TextAlign.Center,
							)
						}
					}

					else -> TextButton(
							onClick = item.onClick,
							enabled = item.enabled,
							shapes = ButtonDefaults.shapes(),
							modifier = Modifier.fillMaxWidth(),
					) {
						item.icon?.invoke()
						Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
						Text(
								text = item.title ?: "",
								style = MaterialTheme.typography.bodyMedium,
								textAlign = TextAlign.Start,
						)
					}
				}
			}
		}
	}
}
