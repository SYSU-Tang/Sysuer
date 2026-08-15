package com.sysu.edu.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.sysu.edu.R

@Preview(showBackground = true) @Composable fun WarningCard() {
	Card(modifier = Modifier.fillMaxWidth(), onClick = {}) {
		Row(modifier = Modifier
			.fillMaxWidth()
			.padding(dimensionResource(R.dimen.content_padding)), verticalAlignment = Alignment.CenterVertically) {
			Icon(imageVector = Icons.Rounded.Warning, contentDescription = "warning", tint = MaterialTheme.colorScheme.error)
			Spacer(modifier = Modifier.width(dimensionResource(R.dimen.icon_text_gap)))
			Text(text = stringResource(R.string.undeveloped_warning), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
		}
	}
}