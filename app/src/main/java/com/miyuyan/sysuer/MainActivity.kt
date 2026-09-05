package com.miyuyan.sysuer

import android.Manifest
import android.app.DownloadManager
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager.Companion.getInstance
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.miyuyan.sysuer.academic.AcademyNotificationRoute
import com.miyuyan.sysuer.academic.CETRoute
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
import com.miyuyan.sysuer.api.HttpManager
import com.miyuyan.sysuer.api.PreferenceViewModel
import com.miyuyan.sysuer.browser.RichTextRoute
import com.miyuyan.sysuer.home.HomeViewModel
import com.miyuyan.sysuer.home.ServiceConfig
import com.miyuyan.sysuer.life.NetPayRoute
import com.miyuyan.sysuer.life.PayRoute
import com.miyuyan.sysuer.nav.AcademyNotification
import com.miyuyan.sysuer.nav.CET
import com.miyuyan.sysuer.nav.CourseDetail
import com.miyuyan.sysuer.nav.CourseSelected
import com.miyuyan.sysuer.nav.Dorm
import com.miyuyan.sysuer.nav.Exam
import com.miyuyan.sysuer.nav.Grade
import com.miyuyan.sysuer.nav.GradeForLevel
import com.miyuyan.sysuer.nav.LeaveSlip
import com.miyuyan.sysuer.nav.MajorInfo
import com.miyuyan.sysuer.nav.NetPay
import com.miyuyan.sysuer.nav.Pay
import com.miyuyan.sysuer.nav.PersonalInformation
import com.miyuyan.sysuer.nav.PersonalTrainingProgram
import com.miyuyan.sysuer.nav.Registration
import com.miyuyan.sysuer.nav.SchoolEnrollment
import com.miyuyan.sysuer.nav.SchoolWorkWarning
import com.miyuyan.sysuer.nav.TrainingProgram
import com.miyuyan.sysuer.nav.Home
import com.miyuyan.sysuer.nav.RichText
import com.miyuyan.sysuer.nav.SysuerNavDisplay
import com.miyuyan.sysuer.theme.SysuerTheme
import com.miyuyan.sysuer.widget.NextClassWidget
import com.miyuyan.sysuer.widget.RecentClassWidget
import com.miyuyan.sysuer.widget.TomorrowClassWidget
import com.miyuyan.sysuer.widget.WidgetUpdateWorker
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File

class MainActivity : BaseActivity() {
	var downloadId: Long = 0
	var receiver: BroadcastReceiver? = null
	var receiverRegistered: Boolean = false
	private lateinit var http: HttpManager
	var path: String = ""
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		http = HttpManager(object : Handler(mainLooper) {
			override fun handleMessage(msg: Message) {
				when (msg.what) {
					-1 -> config.toast(R.string.no_net_connected)
					0 -> config.contextUtil.disposable.add(
						Observable.just(msg.obj).map {
							JSONObject.parseObject(it as String?)
						}.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
							.subscribe({ response: JSONObject ->
								showUpdateDialog(response)
							}, {})
					)
				}
			}
		}).apply {
			setParams(this@MainActivity.config)
		}
		val homeViewModel: HomeViewModel by viewModels()
		val spm: PreferenceViewModel by viewModels()
		initActionMap(homeViewModel.actionMap)
		setContent {
			MainScreen()
		}
		spm.isFirstLaunch = false
		spm.isAgreeLiveData.observe(this) { aBoolean ->
			if (aBoolean) {
				if (spm.update) checkUpdate()
				listOf(
					NextClassWidget::class.java,  /*TodayClassWidget.class, */
					TomorrowClassWidget::class.java, RecentClassWidget::class.java
				).forEach {
					sendBroadcast(
						Intent(this, it).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
							.putExtra(
								AppWidgetManager.EXTRA_APPWIDGET_IDS,
								AppWidgetManager.getInstance(this@MainActivity)
									.getAppWidgetIds(ComponentName(this@MainActivity, it))
							)
					)
				}
				receiver = object : BroadcastReceiver() {
					override fun onReceive(context: Context?, intent: Intent) {
						if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent.action && intent.getLongExtra(
								DownloadManager.EXTRA_DOWNLOAD_ID,
								-1
							) == downloadId
						) {
							config.toast(R.string.download_complete)
							com.miyuyan.sysuer.api.DownloadManager.openFile(this@MainActivity, path)
						}
					}
				}
				ContextCompat.registerReceiver(
					this,
					receiver,
					IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
					ContextCompat.RECEIVER_EXPORTED
				)
				receiverRegistered = true
				getInstance(this).enqueue(
					OneTimeWorkRequest.Builder(WidgetUpdateWorker::class.java).setInputData(
						Data.Builder().putStringArray(
							"components",
							arrayOf("TodayClassWidget", "RecentClassWidget", "NextClassWidget")
						).build()
					).build()
				)
				if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) requestPermissions(
					arrayOf(Manifest.permission.POST_NOTIFICATIONS),
					PackageManager.PERMISSION_GRANTED
				)
			} else {
				val agreementDialog =
					MaterialAlertDialogBuilder(this).setTitle(R.string.user_agreement_and_privacy_policy)
						.setMessage("")
						.setPositiveButton(R.string.agree) { _: DialogInterface?, _: Int ->
							spm.isAgree = true
							spm.setIsAgreeLiveData(true)
						}.setNegativeButton(R.string.disagree) { _: DialogInterface?, _: Int ->
							spm.isAgree = false
							supportFinishAfterTransition()
						}.setCancelable(false).create()
				agreementDialog.show()
				agreementDialog.findViewById<TextView>(android.R.id.message)?.let {
					Markwon.builder(this)
						.usePlugin(StrikethroughPlugin.create())
						.build()
						.setMarkdown(
							it,
							"请认真阅读[用户协议](https://sysu-tang.github.io/sysuer-website/docs/userAgreement)和[隐私政策](https://sysu-tang.github.io/sysuer-website/docs/privacyPolicy)"
						)
				}
			}
		}
	}

	@OptIn(ExperimentalMaterial3Api::class)
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
				})
			}
		}
	}

	fun showUpdateDialog(response: JSONObject) {
		if (PackageInfoCompat.getLongVersionCode(
				this.packageManager.getPackageInfo(
					this.packageName,
					0
				)
			) < response.getInteger("version")
		) {
			path = "${
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
			}/${getString(R.string.app_name)}${response.getString("versionName")}.apk"
			val releaseLink = response.getString(
				"link",
				"https://github.com/SYSU-Tang/Sysuer/releases/latest/download/app-release.apk"
			)
			MaterialAlertDialogBuilder(this).setMessage("")
				.setTitle(R.string.higher_version_detected)
				.setPositiveButton(R.string.download_in_system) { _: DialogInterface?, _: Int ->
					downloadId = (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(
						DownloadManager.Request(Uri.parse(releaseLink))
							.setDestinationUri(Uri.fromFile(File(path)))
							.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
					)
				}.setNegativeButton(R.string.download_in_browser) { _: DialogInterface?, _: Int ->
					startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releaseLink)))
				}.setCancelable(!response.getBoolean("enforce"))
				.setNeutralButton(R.string.download_in_app) { _: DialogInterface?, _: Int ->
					val notificationManager = NotificationManagerCompat.from(this)
					notificationManager.createNotificationChannel(
						NotificationChannelCompat.Builder(
							"update",
							NotificationManagerCompat.IMPORTANCE_DEFAULT
						).setDescription("APP下载通知").setName("下载进度通知").build()
					)
					com.miyuyan.sysuer.api.DownloadManager.downloadFile(
						this, releaseLink, path, true/*, object : com.sysu.edu.api.DownloadManager.DownloadListener {
					override fun onDownloadProgress(
						progress: Long,
						total: Long,
					                               ) {
					}
					
					override fun onDownloadComplete(
						path: String?,
					                               ) {
					}
					
					override fun onDownloadError(
						code: Int,
						message: String?,
					                            ) {
					}
				}*/
					)
				}.create().apply {
					setCancelable(!response.getBoolean("enforce"))
					show()
					findViewById<TextView>(android.R.id.message)?.let {
						Markwon.builder(this@MainActivity).build()
							.setMarkdown(it, response.getString("description"))
					}
				}
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
					}/${getString(R.string.app_name)}-$versionName}apk"
					val previewLink = response.getString(
						"previewLink",
						"https://github.com/SYSU-Tang/Sysuer/releases/download/$versionName/app-release.apk"
					)
					MaterialAlertDialogBuilder(this).setMessage("")
						.setTitle(R.string.beta_version_detected)
						.setPositiveButton(R.string.download_in_system) { _: DialogInterface?, _: Int ->
							downloadId = getSystemService(DownloadManager::class.java).enqueue(
								DownloadManager.Request(Uri.parse(previewLink))
									.setDestinationUri(Uri.fromFile(File(path)))
									.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
							)
						}
						.setNegativeButton(R.string.download_in_browser) { _: DialogInterface?, _: Int ->
							startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(previewLink)))
						}.setCancelable(!response.getBoolean("enforce"))
						.setNeutralButton(R.string.download_in_app) { _: DialogInterface?, _: Int ->
							com.miyuyan.sysuer.api.DownloadManager.downloadFile(this, previewLink, path)
						}.create().apply {
							show()
							findViewById<TextView>(android.R.id.message)?.let {
								Markwon.builder(this@MainActivity).build().setMarkdown(
									it,
									response.getString("previewDescription", "暂无更新描述")
								)
							}
						}
				}
			}
		}
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

	fun checkUpdate() {
		http.getRequest("https://sysu-tang.github.io/latest.json", 0)
	}

	override fun onDestroy() {
		super.onDestroy()
		if (receiverRegistered) {
			unregisterReceiver(receiver)
			receiver = null
			receiverRegistered = false
		}
	}

	fun initActionMap(actionMap: MutableMap<in Int?, View.OnClickListener>) {
		actionMap[302] = View.OnClickListener {
			packageManager.getLaunchIntentForPackage("com.comingx.zanao")?.let {
				startActivity(it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
			} ?: config.toast(R.string.no_app)
		}
		actionMap[601] = View.OnClickListener {
			settingManager.qrCode.takeIf { it.isNotEmpty() }?.let {
				startActivity(Intent(Intent.ACTION_VIEW, it.toUri()))
			} ?: config.toast(R.string.no_app)
		}
		actionMap[602] = View.OnClickListener {
			packageManager.getLaunchIntentForPackage("com.tencent.wework")?.let {
				startActivity(it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
			} ?: config.toast(R.string.no_app)
		}
	}
}

@Composable
fun ServiceSearchResults(
	results: List<ServiceConfig>,
	onResultClick: (ServiceConfig) -> Unit,
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
