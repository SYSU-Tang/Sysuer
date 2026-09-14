package com.miyuyan.sysuer.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.view.UiState.Content
import com.miyuyan.sysuer.view.UiState.Empty
import com.miyuyan.sysuer.view.UiState.Error
import com.miyuyan.sysuer.view.UiState.LoadMore
import com.miyuyan.sysuer.view.UiState.Loading
import com.miyuyan.sysuer.view.UiState.Unstarted

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatePage(
	state: UiState,
	modifier: Modifier = Modifier,
	emptyWarning: String = stringResource(R.string.no_data),
	onRetry: () -> Unit = {},
	content: @Composable () -> Unit = {}
) {
	Box(modifier = modifier.fillMaxSize()) {
		val modifier = Modifier
			.align(Alignment.Center)
			.verticalScroll(rememberScrollState())
			.nestedScroll(rememberNestedScrollInteropConnection())
		when (state) {
			Loading -> LoadingView(modifier = modifier)
			Empty -> EmptyView(modifier = modifier, text = emptyWarning)
			Error -> ErrorView(
				modifier = modifier, onRetry = onRetry
			)

			Content -> content()
			Unstarted -> {}
			LoadMore -> {
				Column(
					modifier = Modifier.fillMaxSize(),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center
				) {
					content()
					LoadingIndicator()
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingView(modifier: Modifier = Modifier) {
	Column(
		modifier = modifier
			.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		LoadingIndicator()
		Text(
			text = stringResource(R.string.loading),
			modifier = Modifier.padding(vertical = dimensionResource(R.dimen.vertical_margin)),
			style = MaterialTheme.typography.displaySmall
		)
	}
}

@Composable
private fun EmptyView(
	modifier: Modifier = Modifier, text: String = stringResource(R.string.no_data)
) {
	Column(
		modifier = modifier
			.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Image(
			painter = painterResource(R.drawable.duck),
			contentDescription = null,
			modifier = Modifier.size(100.dp)
		)
		Text(
			text = text, style = MaterialTheme.typography.displaySmall
		)
	}
}

@Composable
private fun ErrorView(
	modifier: Modifier = Modifier, onRetry: () -> Unit
) {
	Column(
		modifier = modifier
			.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Image(
			painter = painterResource(R.drawable.info),
			contentDescription = null,
			modifier = Modifier.size(80.dp),
			colorFilter = ColorFilter.tint(
				MaterialTheme.colorScheme.error
			)
		)
		Text(
			text = stringResource(R.string.load_failed),
			style = MaterialTheme.typography.displaySmall
		)
		OutlinedButton(
			onClick = onRetry,
			modifier = Modifier.padding(vertical = dimensionResource(R.dimen.vertical_margin))
		) {
			Text(stringResource(R.string.retry))
		}
	}
}