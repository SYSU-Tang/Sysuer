package com.sysu.edu.extra

import android.Manifest
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.pm.PackageInfoCompat
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.DownloadManager
import com.sysu.edu.api.HttpManager
import com.sysu.edu.api.Params
import com.sysu.edu.databinding.ActivityUpdateBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.util.Locale

class UpdateActivity : BaseActivity() {
	var http: HttpManager? = null
	val disposable: CompositeDisposable = CompositeDisposable()
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		var versionCode: Long = 0
		val click = ArrayList<Long?>()
		val binding = ActivityUpdateBinding.inflate(layoutInflater).apply {
			setContentView(getRoot())
			toolbar.setNavigationOnClickListener { _: View? -> supportFinishAfterTransition() }
			try {
				val info = packageManager.getPackageInfo(packageName, 0)
				versionCode = PackageInfoCompat.getLongVersionCode(info)
				version.text = "${info.versionName}($versionCode)"
			} catch (_: PackageManager.NameNotFoundException) {
			}
		}
		val notificationManager = NotificationManagerCompat.from(this)
		notificationManager.createNotificationChannel(NotificationChannelCompat.Builder("update", NotificationManagerCompat.IMPORTANCE_DEFAULT)
														  .setDescription("APP下载通知")
														  .setName("下载进度通知").build())
		val params = Params(this)
		http = HttpManager(object : Handler(mainLooper) {
			override fun handleMessage(msg: Message) {
				when (msg.what) {
					-1 -> {
						params.toast(R.string.no_net_connected)
						binding.updateButton.setText(R.string.no_net_connected)
					}
					0 -> disposable.add(Observable.just<Any>(msg.obj).map<Any> { text -> JSONObject.parse(text as String) }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe { response: Any ->
						val responseVersion = (response as JSONObject).getInteger("version")
						val responseVersionName = response.getString("versionName")
						binding.changelog.setMarkdown("# " + responseVersionName + "(" + responseVersion + ")\n" + response.getString("description"))
						binding.updateButton.setText(if (responseVersion > versionCode) R.string.update else R.string.app_latest_installed)
						binding.updateButton.setOnClickListener(View.OnClickListener setOnClickListener@{ _: View? ->
							if (responseVersion > versionCode) {
								val path =
									"${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${getString(R.string.app_name)}$responseVersionName.apk"
								File(path).takeIf {
									it.exists() && it.getTotalSpace() > 0
								}?.let {
									DownloadManager.openFile(this@UpdateActivity, path)
									return@setOnClickListener
								}
								DownloadManager.downloadFile(this@UpdateActivity, response.getString("link"), path, object : DownloadManager.DownloadListener {
									override fun onDownloadProgress(progress: Long, total: Long) {
										val progressString = String.format(Locale.getDefault(), "%.2fMB/%.2fMB", progress / 1024.0f / 1024.0f, total / 1024.0f / 1024.0f)
										binding.updateButton.text = progressString
										val builder = NotificationCompat.Builder(this@UpdateActivity, "update")
											.setContentTitle(getString(R.string.download))
											.setContentText(progressString)
											.setSmallIcon(R.drawable.down)
											.setStyle(NotificationCompat.BigTextStyle()
														  .bigText(progressString))
											.setProgress((total).toInt(), progress.toInt(), false)
											.setPriority(NotificationCompat.PRIORITY_DEFAULT)
										if (ActivityCompat.checkSelfPermission(this@UpdateActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) notificationManager.notify(1002, builder.build())
									}
									
									override fun onDownloadComplete(path: String?) {
										binding.updateButton.setText(R.string.install)
										val builder = NotificationCompat.Builder(this@UpdateActivity, "update")
											.setContentTitle(getString(R.string.download))
											.setContentText(getString(R.string.apk_next_step_notice))
											.setSmallIcon(R.drawable.down)
											.setContentIntent(DownloadManager.getOpenFileIntent(this@UpdateActivity, path)?.let { PendingIntentCompat.getActivity(this@UpdateActivity, 0, it, PendingIntent.FLAG_ONE_SHOT, true) })
											.setProgress(1, 1, false)
											.setPriority(NotificationCompat.PRIORITY_DEFAULT)
										if (ActivityCompat.checkSelfPermission(this@UpdateActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
											notificationManager.notify(1002, builder.build())
										path?.let { DownloadManager.openFile(this@UpdateActivity, it) }
									}
									
									override fun onDownloadError(code: Int, message: String?) {
										params.toast(message)
									}
								})
							}
						})
					})
				}
			}
		}).apply {
			setParams(params)
		}
		binding.icon.setOnClickListener { _: View? ->
			if (click.isEmpty() || System.currentTimeMillis() - click[click.size - 1]!! < 500) if (click.size == 4) {
				params.toast(if (params.isDeveloper) R.string.developer_disabled else R.string.developer_enabled)
				params.isDeveloper = !params.isDeveloper
				click.clear()
			} else click.add(System.currentTimeMillis())
			else click.clear()
		}
		this.update
	}
	
	val update: Unit
		get() {
			http!!.getRequest("https://sysu-tang.github.io/latest.json", 0)
		}
	
	override fun onDestroy() {
		super.onDestroy()
		disposable.dispose()
	}
}