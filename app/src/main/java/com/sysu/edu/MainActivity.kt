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
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavInflater
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager.Companion.getInstance
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationBarView
import com.sysu.edu.api.HttpManager
import com.sysu.edu.api.Params
import com.sysu.edu.api.PreferenceViewModel
import com.sysu.edu.databinding.ActivityMainBinding
import com.sysu.edu.home.HomeViewModel
import com.sysu.edu.widget.NextClassWidget
import com.sysu.edu.widget.RecentClassWidget
import com.sysu.edu.widget.TomorrowClassWidget
import com.sysu.edu.widget.WidgetUpdateWorker
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File

class MainActivity : BaseActivity() {
	lateinit var params: Params
	var downloadId: Long = 0
	var receiver: BroadcastReceiver? = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent) {
			if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent.action && intent.getLongExtra(
					DownloadManager.EXTRA_DOWNLOAD_ID, -1
				) == downloadId
			) {
				params.toast(getString(R.string.download_complete))
				com.sysu.edu.api.DownloadManager.openFile(this@MainActivity, path)
			}
		}
	}
	var http: HttpManager? = null
	var path: String = ""
	var disposable: CompositeDisposable = CompositeDisposable()
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		params = Params(this)
		http = HttpManager(object : Handler(mainLooper) {
			override fun handleMessage(msg: Message) {
				when (msg.what) {
					-1 -> params.toast(R.string.no_net_connected)
					0 -> disposable.add(
						Observable.just(JSONObject.parseObject(msg.obj as String?))
							.subscribeOn(Schedulers.io())
							.observeOn(AndroidSchedulers.mainThread())
							.subscribe(Consumer { response: JSONObject? ->
								this@MainActivity.showUpdateDialog(response!!)
							})
					)
				}
			}
		}).apply {
			setParams(params)
		}
		val binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.getRoot())
		val viewModel = ViewModelProvider(this).get<HomeViewModel>(HomeViewModel::class.java)
		initActionMap(viewModel.actionMap)
		val fragment =
			supportFragmentManager.findFragmentById(R.id.dashboard_scroll) as NavHostFragment
		val navController = fragment.navController
		val graph =
			NavInflater(this, navController.navigatorProvider).inflate(R.navigation.main_nav)
		PreferenceManager.getDefaultSharedPreferences(this).getString("home", "0")?.let {
			graph.setStartDestination(
				when (it) {
					"0" -> R.id.navigation_dashboard
					"1" -> R.id.navigation_service
					"2" -> R.id.navigation_account
					else -> R.id.navigation_dashboard
				}
			)
		}
		navController.graph = graph
		setupWithNavController(binding.navView as NavigationBarView, navController)
		val spm = ViewModelProvider(this)[PreferenceViewModel::class.java]
			.apply {
				setPM(PreferenceManager.getDefaultSharedPreferences(this@MainActivity))
				initLiveData()
				isFirstLaunch = false
			}
		val agreementDialog =
			MaterialAlertDialogBuilder(this)
				.setTitle(R.string.user_agreement_and_privacy_policy)
				.setMessage("")
				.setPositiveButton(
					R.string.agree
				) { _: DialogInterface?, _: Int ->
					spm.isAgree = true
					spm.setIsAgreeLiveData(false)
				}
				.setNegativeButton(
					R.string.disagree
				) { _: DialogInterface?, _: Int ->
					spm.isAgree = false
					supportFinishAfterTransition()
				}
				.setCancelable(false)
				.create()
		spm.isAgreeLiveData.observe(this, Observer { aBoolean: Boolean? ->
			if (aBoolean == true) {
				agreementDialog.show()
				agreementDialog.findViewById<TextView>(android.R.id.message)?.let {
					Markwon.builder(this)
						.usePlugin(StrikethroughPlugin.create()).build()
						.setMarkdown(
							it,
							"请认真阅读[用户协议](https://sysu-tang.github.io/sysuer-website/docs/userAgreement)和[隐私政策](https://sysu-tang.github.io/sysuer-website/docs/privacyPolicy)"
						)
				}
			} else {
				if (spm.update) checkUpdate()
				listOf(
					NextClassWidget::class.java,  /*TodayClassWidget.class, */
					TomorrowClassWidget::class.java,
					RecentClassWidget::class.java
				).forEach {
					sendBroadcast(Intent(this, it).apply {
						action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
						putExtra(
							AppWidgetManager.EXTRA_APPWIDGET_IDS,
							AppWidgetManager.getInstance(this@MainActivity)
								.getAppWidgetIds(ComponentName(this@MainActivity, it))
						)
					})
				}
				ContextCompat.registerReceiver(
					this,
					receiver,
					IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
					ContextCompat.RECEIVER_EXPORTED
				)
				getInstance(this).enqueue(
					OneTimeWorkRequest.Builder(WidgetUpdateWorker::class.java)
						.setInputData(
							Data.Builder()
								.putStringArray(
									"components",
									arrayOf(
										"TodayClassWidget",
										"RecentClassWidget",
										"NextClassWidget"
									)
								)
								.build()
						)
						.build()
				)
				if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) requestPermissions(
						arrayOf(
							Manifest.permission.POST_NOTIFICATIONS
						), PackageManager.PERMISSION_GRANTED
					)
			}
		})
	}
	
	fun showUpdateDialog(response: JSONObject) {
		if (PackageInfoCompat.getLongVersionCode(this.packageManager.getPackageInfo(this.packageName, 0))
			< response.getInteger("version")) {
			path =
				"${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${
					getString(R.string.app_name)
				}${response.getString("versionName")}.apk"
			val updateDialog = MaterialAlertDialogBuilder(this)
				.setMessage("")
				.setTitle(R.string.higher_version_detected)
				.setPositiveButton(
					R.string.download_in_system
				) { _: DialogInterface?, _: Int ->
					downloadId = (getSystemService(
						DOWNLOAD_SERVICE
					) as DownloadManager)
						.enqueue(
							DownloadManager.Request(Uri.parse(response.getString("link")))
								.setDestinationUri(Uri.fromFile(File(path)))
								.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
						)
				}
				.setNegativeButton(
					R.string.download_in_browser
				) { _: DialogInterface?, _: Int ->
					startActivity(
						Intent(Intent.ACTION_VIEW, Uri.parse(response.getString("link")))
					)
				}
				.setCancelable(!response.getBoolean("enforce"))
				.setNeutralButton(
					R.string.download_in_app
				) { _: DialogInterface?, _: Int ->
					com.sysu.edu.api.DownloadManager.downloadFile(
						this,
						response.getString("link"),
						path
					)
				}
				.create()
			updateDialog.show()
			updateDialog.findViewById<TextView>(android.R.id.message)?.let {
				Markwon.builder(this).build().setMarkdown(it, response.getString("description")
				)
			}
		}
	}
	
	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<String>,
		grantResults: IntArray
	) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == PackageManager.PERMISSION_GRANTED) {
			if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
				params.toast(R.string.permission_granted)
		}
	}
	
	fun checkUpdate() {
		http?.getRequest("https://sysu-tang.github.io/latest.json", 0)
	}
	
	override fun onDestroy() {
		unregisterReceiver(receiver)
		receiver = null
		disposable.dispose()
		super.onDestroy()
	}
	
	fun initActionMap(actionMap: MutableMap<in Int?, View.OnClickListener?>) {
		actionMap[302] = View.OnClickListener { _: View? ->
			packageManager.getLaunchIntentForPackage("com.comingx.zanao")?.let {
				startActivity(it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
			} ?: params.toast(R.string.no_app)
		} // 校园集市
		actionMap[601] = View.OnClickListener { _: View? ->
			PreferenceManager.getDefaultSharedPreferences(this).getString("qrcode", "")
				?.let {//new LaunchMiniProgram(this).launchMiniProgram("gh_85575b9f544e");
					startActivity(Intent(Intent.ACTION_VIEW, it.toUri()))
				} ?: params.toast(R.string.no_app)
		}// 二维码
		actionMap[602] = View.OnClickListener { _: View? ->
			packageManager.getLaunchIntentForPackage("com.tencent.wework")?.let {
				startActivity(it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
			} ?: params.toast(R.string.no_app)
		} // 企业微信
	}
}