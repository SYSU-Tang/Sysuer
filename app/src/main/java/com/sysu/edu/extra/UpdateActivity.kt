package com.sysu.edu.extra

import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.view.View
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.pm.PackageInfoCompat
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.BaseActivity
import com.sysu.edu.BuildConfig
import com.sysu.edu.R
import com.sysu.edu.api.DownloadManager
import com.sysu.edu.api.HttpManager
import com.sysu.edu.databinding.ActivityUpdateBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers

class UpdateActivity : BaseActivity() {
	private lateinit var http: HttpManager
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		var versionCode: Long = 0
		val click = mutableListOf<Long?>()
		var response: JSONObject? = null
		val binding = ActivityUpdateBinding.inflate(layoutInflater).apply {
			setContentView(root)
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			val info = packageManager.getPackageInfo(packageName, 0)
			versionCode = PackageInfoCompat.getLongVersionCode(info)
			version.text = getString(R.string.version_info, info.versionName, versionCode)
			icon.setOnClickListener {
				if (click.isEmpty() || System.currentTimeMillis() - click[click.size - 1]!! < 500) if (click.size == 4) {
					config.toast(if (settingManager.developerMode) R.string.developer_disabled else R.string.developer_enabled)
					settingManager.developerMode = !settingManager.developerMode
					click.clear()
				}
				else click.add(System.currentTimeMillis())
				else click.clear()
			}
		}
		val notificationManager = NotificationManagerCompat.from(this)
		notificationManager.createNotificationChannel(NotificationChannelCompat.Builder("update", NotificationManagerCompat.IMPORTANCE_DEFAULT).setDescription("APP下载通知").setName("下载进度通知").build())
		http = HttpManager(object : Handler(mainLooper) {
			override fun handleMessage(msg: Message) {
				when (msg.what) {
					-1 -> {
						config.toast(R.string.no_net_connected)
						binding.updateButton.setText(R.string.no_net_connected)
					}
					0 -> {
						config.contextUtil.disposable.add(Observable.just(msg.obj).map {
								JSONObject.parseObject(it as String?)
							}.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({ data: Any ->
								                                                                                   response = data as JSONObject
								                                                                                   response?.apply {
									                                                                                   val responseVersion = getIntValue("version", 0)
									                                                                                   val responseVersionName = getString("versionName")
									                                                                                   if (responseVersion > versionCode) {
										                                                                                   binding.changelog.setMarkdown("# $responseVersionName($responseVersion)\n${
											                                                                                   data.getString("description", "")
										                                                                                   }")
										                                                                                   binding.updateButton.setText(R.string.higher_version_detected)
									                                                                                   }
									                                                                                   else if (settingManager.developerMode && settingManager.betaCheck && containsKey("minorVersion") && containsKey("majorVersion") && containsKey(
											                                                                                   "generationVersion")) {
										                                                                                   val minorVersion = getIntValue("minorVersion", 0)
										                                                                                   val majorVersion = getIntValue("majorVersion", 0)
										                                                                                   val generationVersion = getIntValue("generationVersion", 0)
										                                                                                   val isBeta = getBooleanValue("isBeta", false)
										                                                                                   if (generationVersion > BuildConfig.VERSION_GENERATION || (generationVersion == BuildConfig.VERSION_GENERATION && majorVersion > BuildConfig.VERSION_MAJOR) || (generationVersion == BuildConfig.VERSION_GENERATION && majorVersion == BuildConfig.VERSION_MAJOR && minorVersion > BuildConfig.VERSION_MINOR) && isBeta) {
											                                                                                   val versionName = "${generationVersion}.${majorVersion}.${minorVersion}-beta"
											                                                                                   binding.changelog.setMarkdown("# $versionName($responseVersion)\n${
												                                                                                   data.getString("previewDescription", "")
											                                                                                   }")
											                                                                                   binding.updateButton.setText(R.string.beta_version_detected)
										                                                                                   }
										                                                                                   else {
											                                                                                   binding.changelog.setMarkdown("# $responseVersionName($responseVersion)\n${
												                                                                                   data.getString("description", "")
											                                                                                   }")
											                                                                                   binding.updateButton.setText(R.string.latest_version_installed)
										                                                                                   }
									                                                                                   }
								                                                                                   }
							                                                                                   }, {
								                                                                                   response = null
								                                                                                   config.toast(R.string.no_net_connected)
								                                                                                   binding.updateButton.setText(R.string.no_net_connected)
							                                                                                   }))
					}
				}
			}
		})
		binding.updateButton.setOnClickListener(View.OnClickListener setOnClickListener@{
			var link = ""
			var path = ""
			response?.apply {
				if (getIntValue("version", 0) > versionCode) {
					path = "${
						Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
					}/${getString(R.string.app_name)}${getString("versionName", "")}.apk"
					link = getString("link", "https://github.com/SYSU-Tang/Sysuer/releases/latest/download/app-release.apk")
				}
				else if (settingManager.developerMode && settingManager.betaCheck && containsKey("minorVersion") && containsKey("majorVersion") && containsKey("generationVersion")) {
					val minorVersion = getIntValue("minorVersion", 0)
					val majorVersion = getIntValue("majorVersion", 0)
					val generationVersion = getIntValue("generationVersion", 0)
					val isBeta = getBooleanValue("isBeta", false)
					if (generationVersion > BuildConfig.VERSION_GENERATION || (generationVersion == BuildConfig.VERSION_GENERATION && majorVersion > BuildConfig.VERSION_MAJOR) || (generationVersion == BuildConfig.VERSION_GENERATION && majorVersion == BuildConfig.VERSION_MAJOR && minorVersion > BuildConfig.VERSION_MINOR) && isBeta) {
						val versionName = "${generationVersion}.${majorVersion}.${minorVersion}-beta"
						path = "${
							Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
						}/${getString(R.string.app_name)}-$versionName}apk"
						link = getString("previewLink", "https://github.com/SYSU-Tang/Sysuer/releases/download/$versionName/app-release.apk")
					}
				}
			} ?: run {
				if (http.client.dispatcher.runningCallsCount() == 0 && http.client.dispatcher.queuedCallsCount() == 0) update
				return@setOnClickListener
			}
			/*if (link.isNotEmpty() && path.isNotEmpty()) File(path).takeIf {
				it.exists() && it.getTotalSpace() > 0
			}?.let {
				DownloadManager.openFile(this@UpdateActivity, path)
				return@setOnClickListener
			} ?:*/ DownloadManager.downloadFile(this@UpdateActivity, link, path, true, object : DownloadManager.DownloadListener {
				override fun onDownloadProgress(
					progress: Long,
					total: Long,) {
//					val progressString = String.format(Locale.getDefault(), "%.2fMB/%.2fMB", progress / 1024.0f / 1024.0f, total / 1024.0f / 1024.0f)
//					binding.updateButton.text = progressString
//					val builder = NotificationCompat.Builder(this@UpdateActivity, "update")
//						.setContentTitle(getString(R.string.download))
//						.setContentText(progressString)
//						.setSmallIcon(R.drawable.down)
//						.setStyle(NotificationCompat.BigTextStyle().bigText(progressString))
//						.setProgress((total).toInt(), progress.toInt(), false)
//						.setPriority(NotificationCompat.PRIORITY_DEFAULT)
//					if (ActivityCompat.checkSelfPermission(this@UpdateActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) notificationManager.notify(1002, builder.build())
				}
				
				override fun onDownloadComplete(path: String?) {
					binding.updateButton.setText(R.string.install)
//					val builder = NotificationCompat.Builder(this@UpdateActivity, "update")
//						.setContentTitle(getString(R.string.download))
//						.setContentText(getString(R.string.apk_next_step_notice))
//						.setSmallIcon(R.drawable.down)
//						.setContentIntent(DownloadManager.getOpenFileIntent(this@UpdateActivity, path)?.let { it1 ->
//							PendingIntentCompat.getActivity(this@UpdateActivity, 0, it1, PendingIntent.FLAG_ONE_SHOT, true)
//						})
//						.setProgress(1, 1, false)
//						.setPriority(NotificationCompat.PRIORITY_DEFAULT)
//					if (ActivityCompat.checkSelfPermission(this@UpdateActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) notificationManager.notify(1002, builder.build())
//					path?.let { it1 ->
//						DownloadManager.openFile(this@UpdateActivity, it1)
//					}
				}
				
				override fun onDownloadError(
					code: Int,
					message: String?,
				                            ) {
//					config.toast(message)
				}
			})
		})
		update
	}
	
	val update: Unit
		get() {
			http.getRequest("https://sysu-tang.github.io/latest.json", 0)
		}
}