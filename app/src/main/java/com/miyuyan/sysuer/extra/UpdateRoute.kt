package com.miyuyan.sysuer.extra

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.miyuyan.sysuer.BuildConfig
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.ContextUtil
import com.miyuyan.sysuer.api.SettingManager
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import java.io.File
import com.miyuyan.sysuer.api.DownloadManager as SysuerDownloadManager

@Composable
fun UpdateRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
	val context = LocalContext.current
	val settingManager = remember { SettingManager.getInstance(context) }
	val viewModel: UpdateModel = viewModel()
	val updateData by viewModel.update.collectAsStateWithLifecycle()
	val clicks = remember { mutableStateListOf<Long>() }

	LaunchedEffect(Unit) {
		viewModel.getLatestVersion()
	}
	var buttonText = stringResource(R.string.latest_version_installed)
	var changelog = ""
	var link = ""
	var path = ""
	updateData?.let { response ->
		val currentVersionCode = PackageInfoCompat.getLongVersionCode(
			context.packageManager.getPackageInfo(context.packageName, 0)
		)
		val releaseVersionCode = response.getIntValue("version", 0)
		val appName = stringResource(R.string.app_name)
		val releaseVersionName = response.getString("versionName")

		if (releaseVersionCode > currentVersionCode) {
			changelog = "# $releaseVersionName($releaseVersionCode)\n${
				response.getString(
					"description", ""
				)
			}"
			buttonText = stringResource(R.string.release_version_detected)
			path =
				"${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$appName${releaseVersionName}.apk"
			link = response.getString(
				"link",
				"https://github.com/SYSU-Tang/Sysuer/releases/latest/download/app-release.apk"
			)
		} else if (settingManager.developerMode && settingManager.betaCheck && response.containsKey(
				"minorVersion"
			) && response.containsKey("majorVersion") && response.containsKey("generationVersion") && response.getBooleanValue(
				"isBeta", false
			)
		) {
			val minorVersion = response.getIntValue("minorVersion", 0)
			val majorVersion = response.getIntValue("majorVersion", 0)
			val generationVersion = response.getIntValue("generationVersion", 0)
			val previewVersionName = "${generationVersion}.${majorVersion}.${minorVersion}-beta"

			if (generationVersion > BuildConfig.VERSION_GENERATION || (generationVersion == BuildConfig.VERSION_GENERATION && majorVersion > BuildConfig.VERSION_MAJOR) || (generationVersion == BuildConfig.VERSION_GENERATION && majorVersion == BuildConfig.VERSION_MAJOR && minorVersion > BuildConfig.VERSION_MINOR)) {
				changelog = "# $previewVersionName($releaseVersionCode)\n${
					response.getString(
						"previewDescription", ""
					)
				}"
				buttonText = stringResource(R.string.beta_version_detected)
				path =
					"${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$appName$previewVersionName.apk"
				link = response.getString(
					"previewLink",
					"https://github.com/SYSU-Tang/Sysuer/releases/download/$previewVersionName/app-release.apk"
				)
			} else {
				changelog = "# $previewVersionName($releaseVersionCode)\n${
					response.getString(
						"previewDescription", ""
					)
				}"
			}
		} else {
			changelog = "# $releaseVersionName($releaseVersionCode)\n${
				response.getString(
					"description", ""
				)
			}"
		}
	}
	ActivityPager(
		title = stringResource(R.string.check_update),
		expandable = true,
		onNavigationClick = { backStack.navigateBack() },
		sharedTransitionScope = sharedTransitionScope,
		animatedVisibilityScope = animatedVisibilityScope,
		sharedKey = "Update",
		isNestedScrollEnabled = false,
		pageContent = {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(
						dimensionResource(R.dimen.horizontal_padding),
						dimensionResource(R.dimen.vertical_padding)
					), horizontalAlignment = Alignment.CenterHorizontally
			) {
				Column(
					modifier = Modifier
						.fillMaxSize()
						.verticalScroll(rememberScrollState())
						.nestedScroll(rememberNestedScrollInteropConnection())
						.weight(1f),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin))
				) {
					ElevatedCard(
						modifier = Modifier.fillMaxWidth(), onClick = {
							val currentTime = System.currentTimeMillis()
							if (clicks.isEmpty() || currentTime - (clicks.lastOrNull()
									?: 0L) < 500
							) {
								clicks.add(currentTime)
								if (clicks.size == 5) {
									settingManager.developerMode = !settingManager.developerMode
									ContextUtil.getInstance(context).toast(
										if (settingManager.developerMode) R.string.developer_enabled
										else R.string.developer_disabled
									)
									clicks.clear()
								}
							} else {
								clicks.clear()
								clicks.add(currentTime)
							}
						}) {
						Column(
							modifier = Modifier
								.fillMaxWidth()
								.padding(
									dimensionResource(R.dimen.horizontal_padding),
									dimensionResource(R.dimen.vertical_padding)
								),
							horizontalAlignment = Alignment.CenterHorizontally,
						) {
							Image(
								modifier = Modifier.size(96.dp),
								painter = painterResource(id = R.drawable.ic_launcher_foreground),
								contentDescription = stringResource(R.string.app_name)
							)
							val info = context.packageManager.getPackageInfo(context.packageName, 0)
							Text(
								text = stringResource(R.string.app_name),
								style = MaterialTheme.typography.headlineSmall,
								color = MaterialTheme.colorScheme.primary
							)
							Text(
								text = stringResource(
									R.string.version_info,
									info.versionName ?: "",
									PackageInfoCompat.getLongVersionCode(info)
								),
								style = MaterialTheme.typography.bodyMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
						}
					}
					if (updateData == null) LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth()) else Markdown(
						changelog, typography = markdownTypography(
							h1 = MaterialTheme.typography.headlineMedium,
							h2 = MaterialTheme.typography.titleLarge,
							h3 = MaterialTheme.typography.titleMedium
						)
					)
				}
				if (updateData != null) {
					if (link.isNotEmpty() && path.isNotEmpty()) {
						var isDownloaded by remember { mutableStateOf(false) }
						FilledTonalButton(
							modifier = Modifier.fillMaxWidth(), onClick = {
								if (isDownloaded) {
									SysuerDownloadManager.openFile(context, path)
								} else {
									SysuerDownloadManager.downloadFile(
										context,
										link,
										path,
										true,
										object : SysuerDownloadManager.DownloadListener {
											override fun onDownloadProgress(
												progress: Long, total: Long
											) {
											}

											override fun onDownloadComplete(path: String?) {
												isDownloaded = true
											}

											override fun onDownloadError(
												code: Int, message: String?
											) {
											}
										})
								}
							}, shapes = ButtonDefaults.shapes()
						) {
							Text(if (isDownloaded) stringResource(R.string.install) else buttonText)
						}

						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.SpaceEvenly
						) {
							TextButton(onClick = {
								context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
							}, shapes = ButtonDefaults.shapes()) {
								Text(stringResource(R.string.download_in_browser))
							}
							TextButton(onClick = {
								val downloadManager =
									context.getSystemService(DownloadManager::class.java)
								downloadManager.enqueue(
									DownloadManager.Request(link.toUri())
										.setDestinationUri(Uri.fromFile(File(path)))
										.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
								)
							}, shapes = ButtonDefaults.shapes()) {
								Text(stringResource(R.string.download_in_system))
							}
						}
					} else {
						FilledTonalButton(
							modifier = Modifier.fillMaxWidth(),
							onClick = {},
							enabled = false,
							shapes = ButtonDefaults.shapes()
						) {
							Text(buttonText)
						}
					}
				} else {
					FilledTonalButton(
						modifier = Modifier.fillMaxWidth(),
						onClick = {},
						enabled = false,
						shapes = ButtonDefaults.shapes()
					) {
						Text(stringResource(R.string.loading))
					}
				}
			}
		})
}
