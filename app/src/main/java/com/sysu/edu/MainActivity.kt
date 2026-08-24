package com.sysu.edu

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarDefaults.appBarWithSearchColors
import androidx.compose.material3.Text
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager.Companion.getInstance
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.HttpManager
import com.sysu.edu.api.PreferenceViewModel
import com.sysu.edu.api.TodoManager
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.home.AccountScreen
import com.sysu.edu.home.DashboardScreen
import com.sysu.edu.home.DashboardViewModel
import com.sysu.edu.home.HomeViewModel
import com.sysu.edu.home.ServiceScreen
import com.sysu.edu.home.ServiceViewModel
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.widget.NextClassWidget
import com.sysu.edu.widget.RecentClassWidget
import com.sysu.edu.widget.TomorrowClassWidget
import com.sysu.edu.widget.WidgetUpdateWorker
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : BaseActivity() {
	var downloadId: Long = 0
	val dashboardViewModel: DashboardViewModel by viewModels()
	val homeViewModel: HomeViewModel by viewModels()
	val spm: PreferenceViewModel by viewModels()
	val serviceViewModel: ServiceViewModel by viewModels()
	val todoManager: TodoManager by lazy { TodoManager(this, lifecycleScope) }
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
					0 -> config.contextUtil.disposable.add(Observable.just(msg.obj).map {
						JSONObject.parseObject(it as String?)
					}.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({ response: JSONObject ->
						                                                                                   this@MainActivity.showUpdateDialog(response)
					                                                                                   }, {}))
				}
			}
		}).apply {
			setParams(this@MainActivity.config)
		}
		val viewModel = ViewModelProvider(this).get<HomeViewModel>(HomeViewModel::class.java)
		initActionMap(viewModel.actionMap)
		setContent {
			MainScreen()
		}
//		val spm = ViewModelProvider(this)[PreferenceViewModel::class.java]
		spm.isFirstLaunch = false
		spm.isAgreeLiveData.observe(this) { aBoolean ->
			if (aBoolean) {
				if (spm.update) checkUpdate()
				listOf(NextClassWidget::class.java,  /*TodayClassWidget.class, */
				       TomorrowClassWidget::class.java, RecentClassWidget::class.java).forEach {
					sendBroadcast(Intent(this, it).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
						              .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(this@MainActivity).getAppWidgetIds(ComponentName(this@MainActivity, it))))
				}
				receiver = object : BroadcastReceiver() {
					override fun onReceive(context: Context?, intent: Intent) {
						if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent.action && intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
							config.toast(getString(R.string.download_complete))
							com.sysu.edu.api.DownloadManager.openFile(this@MainActivity, path)
						}
					}
				}
				ContextCompat.registerReceiver(this, receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED)
				receiverRegistered = true
				getInstance(this).enqueue(OneTimeWorkRequest.Builder(WidgetUpdateWorker::class.java).setInputData(Data.Builder().putStringArray("components", arrayOf("TodayClassWidget", "RecentClassWidget", "NextClassWidget")).build()).build())
				if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),
				                                                                                                                                                                                    PackageManager.PERMISSION_GRANTED)
			}
			else {
				val agreementDialog = MaterialAlertDialogBuilder(this).setTitle(R.string.user_agreement_and_privacy_policy).setMessage("").setPositiveButton(R.string.agree) { _: DialogInterface?, _: Int ->
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
						.setMarkdown(it, "请认真阅读[用户协议](https://sysu-tang.github.io/sysuer-website/docs/userAgreement)和[隐私政策](https://sysu-tang.github.io/sysuer-website/docs/privacyPolicy)")
				}
			}
		}
	}
	
	@OptIn(ExperimentalMaterial3Api::class) @Composable private fun MainScreen() {
		val progressMax by dashboardViewModel.progressMax.collectAsStateWithLifecycle()
		val progressCurrent by dashboardViewModel.progressCurrent.collectAsStateWithLifecycle()
		val searchBarState = rememberContainedSearchBarState()
		val textFieldState = rememberTextFieldState()
		var searchQuery by rememberSaveable { mutableStateOf("") }
		LaunchedEffect(Unit) { serviceViewModel.loadServiceData() }
		LaunchedEffect(textFieldState) {
			snapshotFlow { textFieldState.text.toString() }.collect { searchQuery = it }
		}
		val allItems = serviceViewModel.allItems
		val searchResults = remember(searchQuery, allItems) {
			if (searchQuery.isBlank()) emptyList()
			else allItems.filter { item ->
				item.getString("name")?.contains(searchQuery, ignoreCase = true) == true || item.getString("description")?.contains(searchQuery, ignoreCase = true) == true
			}.sortedWith(compareByDescending<JSONObject> { item ->
				when {
					item.getString("name")?.startsWith(searchQuery, ignoreCase = true) == true -> 2
					item.getString("name")?.contains(searchQuery, ignoreCase = true) == true -> 1
					else -> 0
				}
			}.thenBy { it.getString("name") })
		}
		val scope = rememberCoroutineScope()
		
		ActivityPager(
			title = getString(R.string.app_name),
			navs = listOf(
				MenuItem(getString(R.string.dashboard), Icons.Rounded.Dashboard),
				MenuItem(getString(R.string.service), Icons.Rounded.GridView),
				MenuItem(getString(R.string.account), Icons.Rounded.Person),
			             ),
			topBarContent = { page ->
				when (page) {
					0 -> {
						if (progressMax > 0) LinearProgressIndicator(
							progress = { progressCurrent.toFloat() / progressMax },
							modifier = Modifier.fillMaxWidth(),
						                                            )
						else LinearProgressIndicator(
							progress = { 1f },
							modifier = Modifier.fillMaxWidth(),
						                            )
					}
					1 -> {
						val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
						val appBarWithSearchColors = appBarWithSearchColors(searchBarColors = SearchBarDefaults.containedColors(state = searchBarState))
						val inputField = @Composable {
							SearchBarDefaults.InputField(
								textFieldState = rememberTextFieldState(),
								searchBarState = searchBarState,
								colors = appBarWithSearchColors.searchBarColors.inputFieldColors,
								onSearch = { },
								placeholder = {
									Text(modifier = Modifier.clearAndSetSemantics {}, text = stringResource(R.string.search))
								},
								leadingIcon = {
									Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search))
								},
							                            )
						}
						AppBarWithSearch(
							scrollBehavior = scrollBehavior,
							windowInsets = SearchBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
							state = searchBarState,
							colors = appBarWithSearchColors(
								appBarContainerColor = Color.Transparent,
							                               ),
							inputField = inputField,
						                )
						ExpandedFullScreenContainedSearchBar(
							state = searchBarState,
							inputField = @Composable {
								SearchBarDefaults.InputField(
									textFieldState = textFieldState,
									searchBarState = searchBarState,
									colors = appBarWithSearchColors.searchBarColors.inputFieldColors,
									onSearch = { },
									placeholder = {
										Text(modifier = Modifier.clearAndSetSemantics {}, text = stringResource(R.string.search))
									},
									leadingIcon = {
										IconButton(onClick = {
											textFieldState.setTextAndPlaceCursorAtEnd("")
											scope.launch { searchBarState.animateToCollapsed() }
										}) {
											Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
										}
									},
								                            )
							},
							colors = appBarWithSearchColors.searchBarColors,
						                                    ) {
							ServiceSearchResults(results = searchResults, onResultClick = { item ->
								textFieldState.setTextAndPlaceCursorAtEnd("")
								scope.launch { searchBarState.animateToCollapsed() }
								navigateToServiceItem(item)
							})
						}
					}
				}
			},
			pageContent = { page ->
				when (page) {
					0 -> DashboardScreen(dashboardViewModel, homeViewModel, spm, todoManager, settingManager)
					1 -> ServiceScreen(homeViewModel, serviceViewModel, searchQuery)
					2 -> AccountScreen { recreate() }
				}
			},
		             )
	}
	
	private fun navigateToServiceItem(item: JSONObject) {
		val intent = if (item.containsKey("activity")) {
			try {
				Intent(this, Class.forName(packageName + item.getString("activity"))).takeIf {
					it.resolveActivity(packageManager) != null
				}
			} catch (_: Exception) {
				null
			}
		}
		else if (item.containsKey("url")) {
			Intent(this, BrowserActivity::class.java).setData(CommonUtil.trim(item.getString("url")).toUri())
		}
		else null
		intent?.let { startActivity(it, ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle()) } ?: config.toast(R.string.activity_not_found)
	}
	
	fun showUpdateDialog(response: JSONObject) {
		if (PackageInfoCompat.getLongVersionCode(this.packageManager.getPackageInfo(this.packageName, 0)) < response.getInteger("version")) {
			path = "${
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
			}/${getString(R.string.app_name)}${response.getString("versionName")}.apk"
			val releaseLink = response.getString("link", "https://github.com/SYSU-Tang/Sysuer/releases/latest/download/app-release.apk")
			MaterialAlertDialogBuilder(this).setMessage("").setTitle(R.string.higher_version_detected).setPositiveButton(R.string.download_in_system) { _: DialogInterface?, _: Int ->
				downloadId = (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(DownloadManager.Request(Uri.parse(releaseLink))
					                                                                             .setDestinationUri(Uri.fromFile(File(path)))
					                                                                             .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED))
			}.setNegativeButton(R.string.download_in_browser) { _: DialogInterface?, _: Int ->
				startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releaseLink)))
			}.setCancelable(!response.getBoolean("enforce")).setNeutralButton(R.string.download_in_app) { _: DialogInterface?, _: Int ->
				val notificationManager = NotificationManagerCompat.from(this)
				notificationManager.createNotificationChannel(NotificationChannelCompat.Builder("update", NotificationManagerCompat.IMPORTANCE_DEFAULT).setDescription("APP下载通知").setName("下载进度通知").build())
				com.sysu.edu.api.DownloadManager.downloadFile(this, releaseLink, path, true, object : com.sysu.edu.api.DownloadManager.DownloadListener {
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
				})
			}.create().apply {
				setCancelable(!response.getBoolean("enforce"))
				show()
				findViewById<TextView>(android.R.id.message)?.let {
					Markwon.builder(this@MainActivity).build().setMarkdown(it, response.getString("description"))
				}
			}
		}
		else if (settingManager.developerMode && settingManager.betaCheck) {
			if (response.containsKey("minorVersion") && response.containsKey("majorVersion") && response.containsKey("generationVersion")) {
				val minorVersion = response.getInteger("minorVersion")
				val majorVersion = response.getInteger("majorVersion")
				val generationVersion = response.getInteger("generationVersion")
				val isBeta = response.getBooleanValue("isBeta", true)
				if (generationVersion > BuildConfig.VERSION_GENERATION || (generationVersion == BuildConfig.VERSION_GENERATION && majorVersion > BuildConfig.VERSION_MAJOR) || (generationVersion == BuildConfig.VERSION_GENERATION && majorVersion == BuildConfig.VERSION_MAJOR && minorVersion > BuildConfig.VERSION_MINOR) && isBeta) {
					val versionName = "${generationVersion}.${majorVersion}.${minorVersion}-beta"
					path = "${
						Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
					}/${getString(R.string.app_name)}-$versionName}apk"
					val previewLink = response.getString("previewLink", "https://github.com/SYSU-Tang/Sysuer/releases/download/$versionName/app-release.apk")
					MaterialAlertDialogBuilder(this).setMessage("").setTitle(R.string.beta_version_detected).setPositiveButton(R.string.download_in_system) { _: DialogInterface?, _: Int ->
						downloadId = getSystemService(DownloadManager::class.java).enqueue(DownloadManager.Request(Uri.parse(previewLink))
							                                                                   .setDestinationUri(Uri.fromFile(File(path)))
							                                                                   .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED))
					}.setNegativeButton(R.string.download_in_browser) { _: DialogInterface?, _: Int ->
						startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(previewLink)))
					}.setCancelable(!response.getBoolean("enforce")).setNeutralButton(R.string.download_in_app) { _: DialogInterface?, _: Int ->
						com.sysu.edu.api.DownloadManager.downloadFile(this, previewLink, path)
					}.create().apply {
						show()
						findViewById<TextView>(android.R.id.message)?.let {
							Markwon.builder(this@MainActivity).build().setMarkdown(it, response.getString("previewDescription", "暂无更新描述"))
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
			if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) config.toast(R.string.permission_granted)
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
	
	fun initActionMap(actionMap: MutableMap<in Int?, View.OnClickListener?>) {
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

@Composable private fun ServiceSearchResults(
	results: List<JSONObject>,
	onResultClick: (JSONObject) -> Unit,
                                            ) {
	if (results.isEmpty()) Text(
		text = stringResource(R.string.search),
		modifier = Modifier.padding(dimensionResource(R.dimen.content_padding)),
		style = MaterialTheme.typography.bodyMedium,
                           )
	else LazyColumn(modifier = Modifier.fillMaxSize()) {
		items(results, key = { it.getIntValue("id") }) { item ->
			ListItem(
				overlineContent = {
					Text(
						item.getString("name", ""),
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
						style = MaterialTheme.typography.titleMedium,
					    )
				},
				modifier = Modifier.clickable(onClick = { onResultClick(item) }),
			        ) {
				Text(
					item.getString("description", ""),
					overflow = TextOverflow.Ellipsis,
					style = MaterialTheme.typography.bodySmall,
				    )
			}
		}
	}
}