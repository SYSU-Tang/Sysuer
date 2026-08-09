package com.sysu.edu.browser

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.halilibo.richtext.ui.material3.RichText
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.theme.SysuerTheme
import com.sysu.edu.api.DataStoreManager
import com.sysu.edu.view.ActivityPager
import kotlinx.coroutines.ExperimentalCoroutinesApi

class RichTextActivity : BaseActivity() {
	@OptIn(ExperimentalCoroutinesApi::class) override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val title = intent.getStringExtra("title") ?: ""
		val contentType = intent.getStringExtra("type")
		var content = ""
		DataStoreManager.loadContent(this, title) {
			content = it
		}
		setContent {
			SysuerTheme {
				ActivityPager(title = title, onNavigationClick = { supportFinishAfterTransition() }, pageContent = {
					Column(modifier = Modifier
						.fillMaxWidth()
						.padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding))) {
						if (contentType == DataStoreManager.ContentType.MARKDOWN.name) {
							val markdownState = rememberMarkdownState(content)
							Markdown(markdownState = markdownState, colors = markdownColor(), typography = markdownTypography(), modifier = Modifier.fillMaxWidth())
						}
						else {
							RichText {
								Text(AnnotatedString(content))
							}
						}
					}
				}, floatingActionButton = {
					SmallExtendedFloatingActionButton(icon = {
						Icon(Icons.Filled.ContentCopy, stringResource(R.string.copy))
					}, text = { Text(stringResource(R.string.copy)) }, onClick = { config.copy(title, content) })
				})
			}
		}
	}
}