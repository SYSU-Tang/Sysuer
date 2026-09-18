package com.miyuyan.sysuer

import android.Manifest
import android.app.DownloadManager
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.alibaba.fastjson2.JSONObject
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.miyuyan.sysuer.academic.AcademyNotificationRoute
import com.miyuyan.sysuer.academic.CETRoute
import com.miyuyan.sysuer.academic.CourseCompletionRoute
import com.miyuyan.sysuer.academic.CourseDetailRoute
import com.miyuyan.sysuer.academic.CourseSelectedRoute
import com.miyuyan.sysuer.academic.DormRoute
import com.miyuyan.sysuer.academic.ExamRoute
import com.miyuyan.sysuer.academic.GradeForLevelRoute
import com.miyuyan.sysuer.academic.GradeRoute
import com.miyuyan.sysuer.academic.LeaveSlipRoute
import com.miyuyan.sysuer.academic.MajorInfoRoute
import com.miyuyan.sysuer.academic.PersonalInformationRoute
import com.miyuyan.sysuer.academic.PersonalTrainingProgramRoute
import com.miyuyan.sysuer.academic.RegistrationRoute
import com.miyuyan.sysuer.academic.SchoolEnrollmentRoute
import com.miyuyan.sysuer.academic.SchoolWorkWarningRoute
import com.miyuyan.sysuer.academic.TrainingProgramRoute
import com.miyuyan.sysuer.api.PreferenceViewModel
import com.miyuyan.sysuer.browser.RichTextRoute
import com.miyuyan.sysuer.extra.AboutRoute
import com.miyuyan.sysuer.extra.UpdateRoute
import com.miyuyan.sysuer.home.ServiceConfig
import com.miyuyan.sysuer.life.NetPayRoute
import com.miyuyan.sysuer.life.PayRoute
import com.miyuyan.sysuer.nav.About
import com.miyuyan.sysuer.nav.AcademyNotification
import com.miyuyan.sysuer.nav.CET
import com.miyuyan.sysuer.nav.CourseCompletion
import com.miyuyan.sysuer.nav.CourseDetail
import com.miyuyan.sysuer.nav.CourseSelected
import com.miyuyan.sysuer.nav.Dorm
import com.miyuyan.sysuer.nav.Exam
import com.miyuyan.sysuer.nav.Grade
import com.miyuyan.sysuer.nav.GradeForLevel
import com.miyuyan.sysuer.nav.Home
import com.miyuyan.sysuer.nav.LeaveSlip
import com.miyuyan.sysuer.nav.MajorInfo
import com.miyuyan.sysuer.nav.NetPay
import com.miyuyan.sysuer.nav.Pay
import com.miyuyan.sysuer.nav.PersonalInformation
import com.miyuyan.sysuer.nav.PersonalTrainingProgram
import com.miyuyan.sysuer.nav.RainClass
import com.miyuyan.sysuer.nav.Registration
import com.miyuyan.sysuer.nav.RichText
import com.miyuyan.sysuer.nav.SchoolEnrollment
import com.miyuyan.sysuer.nav.SchoolWorkWarning
import com.miyuyan.sysuer.nav.SysuerNavDisplay
import com.miyuyan.sysuer.nav.TrainingProgram
import com.miyuyan.sysuer.nav.Update
import com.miyuyan.sysuer.rainClass.RainClassRoute
import com.miyuyan.sysuer.theme.SysuerTheme
import com.miyuyan.sysuer.widget.TomorrowClassWidget
import java.io.File

class MainActivity : BaseActivity() {
	var downloadId: Long = 0
	var receiver: BroadcastReceiver? = null
	var receiverRegistered: Boolean = false
	var path: String = ""
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val spm: PreferenceViewModel by viewModels()
		spm.isFirstLaunch = false
		setContent {
			SysuerTheme(settingManager) {
				val mainViewModel: MainViewModel = viewModel()
				val isAgree by spm.isAgreeLiveData.observeAsState()
				val updateData by mainViewModel.update.collectAsStateWithLifecycle()
				updateData?.let { UpdateDialog(it) }
				LaunchedEffect(isAgree) {
					if (isAgree == true) {
						if (spm.update) mainViewModel.getLatestVersion()
						listOf(                            /*NextClassWidget::class.java,*/  /*TodayClassWidget.class, */
								TomorrowClassWidget::class.java/*, RecentClassWidget::class.java*/
						).forEach {
							sendBroadcast(
									Intent(
											this@MainActivity, it
									).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(
											AppWidgetManager.EXTRA_APPWIDGET_IDS,
											AppWidgetManager.getInstance(this@MainActivity)
												.getAppWidgetIds(
														ComponentName(
																this@MainActivity, it
														)
												)
									)
							)
						}
						receiver = object : BroadcastReceiver() {
							override fun onReceive(context: Context?, intent: Intent) {
								if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent.action && intent.getLongExtra(
											DownloadManager.EXTRA_DOWNLOAD_ID, -1
									) == downloadId
								) {
									config.toast(R.string.download_complete)
									com.miyuyan.sysuer.api.DownloadManager.openFile(
											this@MainActivity, path
									)
								}
							}
						}
						ContextCompat.registerReceiver(
								this@MainActivity,
								receiver,
								IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
								ContextCompat.RECEIVER_NOT_EXPORTED
						)
						receiverRegistered = true
						if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) requestPermissions(
								arrayOf(Manifest.permission.POST_NOTIFICATIONS),
								PackageManager.PERMISSION_GRANTED
						)
					}
				}
				if (isAgree == true) {
					MainScreen()
				} else {
					AlertDialog(
							title = { Text(stringResource(R.string.user_agreement_and_privacy_policy)) },
							text = {
								Markdown(
										"请认真阅读[用户协议](https://sysu-tang.github.io/sysuer-website/docs/userAgreement)和[隐私政策](https://sysu-tang.github.io/sysuer-website/docs/privacyPolicy)",
										modifier = Modifier
								)
							},
							onDismissRequest = {},
							dismissButton = {
								TextButton(
										onClick = { supportFinishAfterTransition() },
										shapes = ButtonDefaults.shapes()
								) {
									Text(stringResource(R.string.exit))
								}
							},
							confirmButton = {
								TextButton(
										onClick = { spm.isAgree = true },
										shapes = ButtonDefaults.shapes()
								) {
									Text(stringResource(R.string.confirm))
								}
							})
				}
			}
		}
	}

	@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
	@Composable
	private fun MainScreen() {
		val backStack = rememberNavBackStack(Home)
		SysuerTheme(settingManager) {
			SharedTransitionLayout {
				SysuerNavDisplay(backStack = backStack, entryProvider = entryProvider {
					entry<Home> {
						HomeRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<CourseSelected> {
						CourseSelectedRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<CourseDetail> {
						CourseDetailRoute(
								backStack,
								it,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<CourseCompletion> {
						CourseCompletionRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<RichText> {
						RichTextRoute(
								backStack,
								it,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<CET> {
						CETRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<Dorm> {
						DormRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<Exam> {
						ExamRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<Grade> {
						GradeRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<GradeForLevel> {
						GradeForLevelRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<LeaveSlip> {
						LeaveSlipRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<NetPay> {
						NetPayRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<Pay> {
						PayRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<Registration> {
						RegistrationRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<MajorInfo> {
						MajorInfoRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<PersonalInformation> {
						PersonalInformationRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<PersonalTrainingProgram> { key ->
						PersonalTrainingProgramRoute(
								backStack,
								key,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<TrainingProgram> {
						TrainingProgramRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<SchoolWorkWarning> {
						SchoolWorkWarningRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<SchoolEnrollment> {
						SchoolEnrollmentRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<AcademyNotification> {
						AcademyNotificationRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<RainClass> {
						RainClassRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<About> {
						AboutRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
					entry<Update> {
						UpdateRoute(
								backStack,
								sharedTransitionScope = this@SharedTransitionLayout,
								animatedVisibilityScope = LocalNavAnimatedContentScope.current
						)
					}
				})
			}
		}
	}

	@Composable
	fun UpdateDialog(response: JSONObject) {
		var showUpdateDialog by remember { mutableStateOf(false) }
		var dismissed by remember { mutableStateOf(false) }
		var title = R.string.release_version_detected
		var content = ""
		var link = ""
		var enforce = false
		if (dismissed) return
		if (PackageInfoCompat.getLongVersionCode(
					packageManager.getPackageInfo(
							packageName, 0
					)
			) < response.getInteger("version")
		) {
			path = "${
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
			}/${getString(R.string.app_name)}${response.getString("versionName")}.apk"
			link = response.getString(
					"link",
					"https://github.com/SYSU-Tang/Sysuer/releases/latest/download/app-release.apk"
			)
			content = response.getString("description", "暂无更新描述")
			title = R.string.release_version_detected
			enforce = response.getBooleanValue("enforce", false)
			showUpdateDialog = true
		} else if (settingManager.developerMode && settingManager.betaCheck) {
			if (response.containsKey("minorVersion") && response.containsKey("majorVersion") && response.containsKey(
						"generationVersion"
				)
			) {
				val minorVersion = response.getInteger("minorVersion")
				val majorVersion = response.getInteger("majorVersion")
				val generationVersion = response.getInteger("generationVersion")
				val isBeta = response.getBooleanValue("isBeta", true)
				if (generationVersion > BuildConfig.VERSION_GENERATION || (generationVersion == BuildConfig.VERSION_GENERATION && majorVersion > BuildConfig.VERSION_MAJOR) || (generationVersion == BuildConfig.VERSION_GENERATION && majorVersion == BuildConfig.VERSION_MAJOR && minorVersion > BuildConfig.VERSION_MINOR) && isBeta) {
					val versionName = "${generationVersion}.${majorVersion}.${minorVersion}-beta"
					path = "${
						Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
					}/${getString(R.string.app_name)}$versionName.apk"
					link = response.getString(
							"previewLink",
							"https://github.com/SYSU-Tang/Sysuer/releases/download/$versionName/app-release.apk"
					)
					title = R.string.beta_version_detected
					content = response.getString("previewDescription", "暂无更新描述")
					showUpdateDialog = true
				}
			}
		}
		fun close() {
			if (!enforce) {
				dismissed = true
				showUpdateDialog = false
			}
		}

		if (showUpdateDialog) AlertDialog(title = { Text(stringResource(title)) }, text = {
			Markdown(
					content,
					modifier = Modifier.verticalScroll(rememberScrollState()),
					typography = markdownTypography(
							h3 = MaterialTheme.typography.titleLarge
					)
			)
		}, onDismissRequest = { close() }, dismissButton = {
			if (!enforce) TextButton(
					onClick = { close() }, shapes = ButtonDefaults.shapes()
			) {
				Text(stringResource(R.string.cancel))
			}
		}, confirmButton = {
			FlowRow(horizontalArrangement = Arrangement.End) {
				TextButton(
						onClick = {
							startActivity(
									Intent(
											Intent.ACTION_VIEW, link.toUri()
									)
							)
							close()
						}, shapes = ButtonDefaults.shapes()
				) {
					Text(stringResource(R.string.download_in_browser))
				}
				TextButton(
						onClick = {
							downloadId = getSystemService(DownloadManager::class.java).enqueue(
									DownloadManager.Request(link.toUri())
										.setDestinationUri(Uri.fromFile(File(path)))
										.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
							)
							close()
						}, shapes = ButtonDefaults.shapes()
				) {
					Text(stringResource(R.string.download_in_system))
				}
				TextButton(
						onClick = {
							com.miyuyan.sysuer.api.DownloadManager.downloadFile(
									this@MainActivity, link, path
							)
							close()
						}, shapes = ButtonDefaults.shapes()
				) {
					Text(stringResource(R.string.download_in_app))
				}
			}
		})
	}

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<String>,
		grantResults: IntArray,
	) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == PackageManager.PERMISSION_GRANTED) {
			if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) config.toast(
					R.string.permission_granted
			)
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		if (receiverRegistered) {
			unregisterReceiver(receiver)
			receiver = null
			receiverRegistered = false
		}
	}
}

@Composable
fun ServiceSearchResults(
	results: List<ServiceConfig>, onResultClick: (ServiceConfig) -> Unit

) {
	if (results.isEmpty()) Text(
			text = stringResource(R.string.search),
			modifier = Modifier.padding(dimensionResource(R.dimen.content_padding)),
			style = MaterialTheme.typography.bodyMedium,
	)
	else LazyColumn(modifier = Modifier.fillMaxSize()) {
		items(results, key = { it.id }) { item ->
			ListItem(
					overlineContent = {
						item.name?.let {
							Text(
									it,
									maxLines = 1,
									overflow = TextOverflow.Ellipsis,
									style = MaterialTheme.typography.titleMedium,
							)
						}
					},
					modifier = Modifier.clickable(onClick = { onResultClick(item) }),
			) {
				item.description?.let {
					Text(
							it,
							overflow = TextOverflow.Ellipsis,
							style = MaterialTheme.typography.bodySmall,
					)
				}
			}
		}
	}
}