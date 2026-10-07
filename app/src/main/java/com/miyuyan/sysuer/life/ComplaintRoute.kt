package com.miyuyan.sysuer.life

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.alibaba.fastjson2.JSONObject
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownTable
import com.mikepenz.markdown.compose.elements.MarkdownTableHeader
import com.mikepenz.markdown.compose.elements.MarkdownTableRow
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.FileManager
import com.miyuyan.sysuer.life.ComplaintModel.Companion.isInvalidPhone
import com.miyuyan.sysuer.nav.Browser
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.StatePage
import com.miyuyan.sysuer.view.UiState
import com.miyuyan.sysuer.view.WarningCard

/**
 * 校园投诉：单个 ActivityPager 承载投诉表单（验证码/附件上传）、回应查询与信访广场三页。
 */
@Composable
fun ComplaintRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: ComplaintViewModel = viewModel()
	val context = LocalContext.current
	val activity = LocalActivity.current

	val attachments by viewModel.attachments.collectAsStateWithLifecycle()
	val squares by viewModel.squares.collectAsStateWithLifecycle()
	val responses by viewModel.responses.collectAsStateWithLifecycle()
	val squareState by viewModel.squareState.collectAsStateWithLifecycle()
	val responseState by viewModel.responseState.collectAsStateWithLifecycle()

	var page by remember { mutableIntStateOf(0) }
	val titles = listOf(
			stringResource(R.string.complaint),
			stringResource(R.string.response),
			stringResource(R.string.square)
	)

	val fileLauncher = rememberLauncherForActivityResult(
			ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			result.data?.data?.let { uri ->
				viewModel.uploadAttachment(FileManager.getAttachmentRequestBody(context, uri))
			}
		}
	}

	ActivityPager(
			title = titles[page],
			navs = listOf(
					MenuItem(stringResource(R.string.complaint), iconResource = R.drawable.voice),
					MenuItem(stringResource(R.string.response), iconResource = R.drawable.volume),
					MenuItem(stringResource(R.string.square), iconResource = R.drawable.dashboard),
			),
			onNavigationClick = { backStack.navigateBack(activity) },
			onPageChange = { page = it },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "Complaint",
	) { currentPage ->
		when (currentPage) {
			0 -> ComplaintFormPage(
					viewModel = viewModel,
					attachments = attachments,
					onBrowse = { backStack.add(Browser(it)) },
					onPickFile = {
						fileLauncher.launch(
								Intent(Intent.ACTION_GET_CONTENT)
									.addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
						)
					},
			)

			1 -> ResponsePage(
					viewModel = viewModel,
					responses = responses,
					state = responseState,
			)

			else -> SquarePage(
					squares = squares,
					state = squareState,
					onRetry = { viewModel.loadSquare() },
			)
		}
	}
}

@Composable
private fun ComplaintFormPage(
	viewModel: ComplaintViewModel,
	attachments: List<JSONObject>,
	onBrowse: (String) -> Unit,
	onPickFile: () -> Unit,
) {
	var visitorName by rememberSaveable { mutableStateOf("") }
	var phone by rememberSaveable { mutableStateOf("") }
	var mobileCheckCode by rememberSaveable { mutableStateOf("") }
	var company by rememberSaveable { mutableStateOf("") }
	var theme by rememberSaveable { mutableStateOf("") }
	var description by rememberSaveable { mutableStateOf("") }
	var checkCode by rememberSaveable { mutableStateOf("") }
	var captchaKey by remember { mutableLongStateOf(0L) }

	Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState())
				.padding(horizontal = dimensionResource(R.dimen.horizontal_margin)),
			verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin)),
	) {
		WarningCard()

		OutlinedTextField(
				value = visitorName,
				onValueChange = { visitorName = it },
				label = { Text(stringResource(R.string.name)) },
				supportingText = { Text(stringResource(R.string.name_helper_text)) },
				leadingIcon = { Icon(painterResource(R.drawable.account), null) },
				singleLine = true,
				modifier = Modifier.fillMaxWidth(),
		)
		OutlinedTextField(
				value = phone,
				onValueChange = { phone = it },
				label = { Text(stringResource(R.string.phone)) },
				leadingIcon = { Icon(painterResource(R.drawable.phone), null) },
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
				singleLine = true,
				modifier = Modifier.fillMaxWidth(),
		)
		OutlinedTextField(
				value = mobileCheckCode,
				onValueChange = { mobileCheckCode = it },
				label = { Text(stringResource(R.string.message_code)) },
				leadingIcon = { Icon(painterResource(R.drawable.message), null) },
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
				singleLine = true,
				trailingIcon = {
					Button(
							onClick = { viewModel.sendMobileCode(phone) },
							shapes = ButtonDefaults.shapes(),
					) {
						Icon(painterResource(R.drawable.send), null)
						Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
						Text(stringResource(R.string.send))
					}
				},
				modifier = Modifier.fillMaxWidth(),
		)
		OutlinedTextField(
				value = company,
				onValueChange = { company = it },
				label = { Text(stringResource(R.string.school_enrollment_work_unit)) },
				supportingText = { Text(stringResource(R.string.optional)) },
				leadingIcon = { Icon(painterResource(R.drawable.home), null) },
				modifier = Modifier.fillMaxWidth(),
		)
		OutlinedTextField(
				value = theme,
				onValueChange = { theme = it },
				label = { Text(stringResource(R.string.theme)) },
				supportingText = { Text(stringResource(R.string.subject_helper_text)) },
				leadingIcon = { Icon(painterResource(R.drawable.text), null) },
				modifier = Modifier.fillMaxWidth(),
		)
		OutlinedTextField(
				value = description,
				onValueChange = { description = it },
				label = { Text(stringResource(R.string.content)) },
				supportingText = { Text(stringResource(R.string.content_helper_text)) },
				modifier = Modifier.fillMaxWidth(),
		)
		Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin)),
		) {
			OutlinedTextField(
					value = checkCode,
					onValueChange = { checkCode = it },
					label = { Text(stringResource(R.string.captcha)) },
					leadingIcon = { Icon(painterResource(R.drawable.version), null) },
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
					singleLine = true,
					modifier = Modifier.weight(1f),
			)
			AsyncImage(
					model = ImageRequest.Builder(LocalContext.current)
						.data("https://${viewModel.host}/servlet/checkcode?t=$captchaKey")
						.memoryCachePolicy(CachePolicy.DISABLED)
						.diskCachePolicy(CachePolicy.DISABLED).build(),
					contentDescription = stringResource(R.string.captcha),
					modifier = Modifier.clickable { captchaKey = System.currentTimeMillis() },
			)
		}

		Card(modifier = Modifier.fillMaxWidth()) {
			Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(dimensionResource(R.dimen.content_padding)),
					verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin)),
			) {
				Text(
						text = stringResource(R.string.attachment),
						style = MaterialTheme.typography.titleMedium,
				)
				attachments.forEachIndexed { index, attachment ->
					AttachmentRow(
							attachment = attachment,
							host = viewModel.host,
							onBrowse = onBrowse,
							onRemove = { viewModel.removeAttachment(index) },
					)
				}
				OutlinedButton(
						onClick = onPickFile,
						shapes = ButtonDefaults.shapes(),
						modifier = Modifier.fillMaxWidth(),
				) {
					Icon(painterResource(R.drawable.up), null)
					Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
					Text(stringResource(R.string.upload_attachment))
				}
			}
		}

//		HorizontalDivider()

		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(vertical = dimensionResource(R.dimen.vertical_margin)),
				horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin)),
		) {
			OutlinedButton(
					onClick = {
						visitorName = ""
						phone = ""
						mobileCheckCode = ""
						company = ""
						theme = ""
						description = ""
						checkCode = ""
						captchaKey = System.currentTimeMillis()
					},
					shapes = ButtonDefaults.shapes(),
					modifier = Modifier.weight(1f),
			) {
				Text(stringResource(R.string.reset))
			}
			OutlinedButton(
					onClick = {
						viewModel.submit(
								mapOf(
										"visitorName" to visitorName,
										"phone" to phone,
										"mobileCheckCode" to mobileCheckCode,
										"name" to theme,
										"company" to company,
										"description" to description,
										"checkCode" to checkCode,
								)
						)
					},
					shapes = ButtonDefaults.shapes(),
					modifier = Modifier.weight(1f),
			) {
				Text(stringResource(R.string.submit))
			}
		}
	}
}

@Composable
private fun AttachmentRow(
	attachment: JSONObject,
	host: String,
	onBrowse: (String) -> Unit,
	onRemove: () -> Unit,
) {
	Row(
			modifier = Modifier
				.fillMaxWidth()
				.clickable { onBrowse("https://$host${attachment.getString("path")}") },
			verticalAlignment = Alignment.CenterVertically,
	) {
		Column(modifier = Modifier.weight(1f)) {
			Text(
					text = attachment.getString("name", ""),
					style = MaterialTheme.typography.bodyLarge
			)
			Text(
					text = "${stringResource(R.string.size)}：${attachment.getString("size", "")}|${
						stringResource(R.string.type)
					}：${attachment.getString("mime", "")}",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
		IconButton(onClick = onRemove) {
			Icon(
					imageVector = Icons.Rounded.Close,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.error,
			)
		}
	}
}

@Composable
private fun ResponsePage(
	viewModel: ComplaintViewModel,
	responses: List<JSONObject>,
	state: UiState,
) {
	var queryPhone by rememberSaveable { mutableStateOf("") }
	var phoneError by rememberSaveable { mutableStateOf(false) }

	Column(modifier = Modifier.fillMaxSize()) {
		OutlinedTextField(
				value = queryPhone,
				onValueChange = {
					queryPhone = it
					phoneError = isInvalidPhone(it)
				},
				label = { Text(stringResource(R.string.phone)) },
				leadingIcon = { Icon(painterResource(R.drawable.phone), null) },
				isError = phoneError,
				supportingText = if (phoneError) {
					{ Text(stringResource(R.string.invalid_phone)) }
				} else null,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
				singleLine = true,
				trailingIcon = {
					IconButton(onClick = {
						if (isInvalidPhone(queryPhone)) {
							phoneError = true
						} else {
							phoneError = false
							viewModel.queryResponse(queryPhone)
						}
					}) {
						Icon(painterResource(R.drawable.search), stringResource(R.string.query))
					}
				},
				modifier = Modifier
					.fillMaxWidth()
					.padding(
							dimensionResource(R.dimen.horizontal_margin),
							dimensionResource(R.dimen.vertical_margin),
					),
		)
		StatePage(
				modifier = Modifier.fillMaxSize(),
				state = state,
		) {
			SquareGrid(items = responses)
		}
	}
}

@Composable
private fun SquarePage(
	squares: List<JSONObject>,
	state: UiState,
	onRetry: () -> Unit,
) {
	StatePage(modifier = Modifier.fillMaxSize(), state = state, onRetry = onRetry) {
		SquareGrid(items = squares)
	}
}

@Composable
private fun SquareGrid(items: List<JSONObject>) {
	LazyVerticalStaggeredGrid(
			columns = StaggeredGridCells.Adaptive(240.dp),
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(dimensionResource(R.dimen.horizontal_margin)),
			verticalItemSpacing = dimensionResource(R.dimen.vertical_margin),
			horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap)),
	) {
		items(items) { item ->
			SquareCard(item = item)
		}
	}
}

@Composable
private fun SquareCard(item: JSONObject) {
	ElevatedCard(modifier = Modifier.fillMaxWidth()) {
		Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(dimensionResource(R.dimen.content_padding)),
		) {
			Text(
					text = item.getString("name"),
					style = MaterialTheme.typography.titleMediumEmphasized,
			)
			Text(
					text = "#${item.getString("createDate")}  #${
						item.getString(
								"questionType",
								"未分类"
						)
					}",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			HorizontalDivider(
					modifier = Modifier.padding(vertical = dimensionResource(R.dimen.vertical_margin)),
			)
			SelectionContainer {
				Text(text = item.getString("description", "暂无公开答复内容"))
			}
			Surface(
					color = MaterialTheme.colorScheme.surfaceContainer,
					shape = MaterialTheme.shapes.medium,
					modifier = Modifier.padding(top = 8.dp),
			) {
				MarkdownContent(
						text = item.getString("dfnr", ""),
						isHtml = true,
						modifier = Modifier.padding(8.dp),
				)
			}
		}
	}
}

@Composable
private fun MarkdownContent(text: String, isHtml: Boolean, modifier: Modifier = Modifier) {
	val content = remember(text) { if (isHtml) AnnotatedString.fromHtml(text).text else text }
	Box(modifier = modifier) {
		Markdown(
				rememberMarkdownState(content),
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
