package com.sysu.edu.api

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.FileProvider
import com.sysu.edu.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLDecoder
import java.util.Locale

object DownloadManager {
	
	val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()
	
	private val CONTENT_DISPOSITION_FILENAME_REGEX = Regex("""filename\*?=(?:UTF-8''|"?)([^";]+)"?""", RegexOption.IGNORE_CASE)
	
	fun getFileNameFromResponse(response: Response, fallbackName: String = "download"): String {
		val disposition = response.header("Content-Disposition")
		if (disposition != null) {
			val match = CONTENT_DISPOSITION_FILENAME_REGEX.find(disposition)
			if (match != null) {
				val encoded = match.groupValues[1].trim()
				return try { URLDecoder.decode(encoded, "UTF-8") } catch (_: Exception) { encoded }
			}
		}
		val url = response.request.url.toString()
		val lastSegment = url.substringAfterLast('/')
		if (lastSegment.isNotEmpty() && lastSegment.contains('.')) return lastSegment
		return fallbackName
	}
	/**
	 * 下载网络文件到指定路径
	 * 
	 * @param context 上下文对象
	 * @param url     网络文件 URL
	 * @param path    本地文件保存路径
	 */
	@JvmStatic fun downloadFile(context: Context, url: String, path: String, notify: Boolean = true, listener: DownloadListener? = null) {
		downloadFile(context,
		             Request.Builder()
			.header("Cookie", CookieManager(context).toSimpleString(url.toHttpUrl().host))
			.header("Accept-Encoding", "identity")
			.url(url).build(), path, notify, listener)
	}
	
	/**
	 * 下载网络文件到指定路径
	 * 
	 * @param context  上下文对象
	 * @param request  网络请求对象
	 * @param path     本地文件保存路径
	 * @param listener 下载监听器
	 */
	@JvmStatic fun downloadFile(context: Context, request: Request, path: String, notify: Boolean = true, listener: DownloadListener? = null) {
		okHttpClient.newCall(request).enqueue(object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				CoroutineScope(Dispatchers.Main).launch { Toast.makeText(context, "${context.getString(R.string.download_error)}:${e.message}", Toast.LENGTH_SHORT).show() }
				listener?.onDownloadError(404, e.message)
			}
			
			override fun onResponse(call: Call, response: Response) {
				val savePath = path.ifEmpty { "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${getFileNameFromResponse(response)}" }
				val length = response.body.contentLength()
				val parentFile = File(savePath).getParentFile()
				if (parentFile != null && !parentFile.isDirectory()) parentFile.mkdirs()
				try {
					NotificationManagerCompat.from(context).createNotificationChannel(NotificationChannelCompat.Builder("update", NotificationManagerCompat.IMPORTANCE_DEFAULT).setDescription("APP下载通知").setName("下载进度通知").build())
					response.body.byteStream().use { stream ->
						FileOutputStream(savePath).use { fos ->
							val buf = ByteArray(100 * 1024)
							var sum: Long = 0
							var len: Int
							while ((stream.read(buf).also { len = it }) != -1) {
								fos.write(buf, 0, len)
								sum += len.toLong()
								if (notify) notifyDownloadProgress(context, sum, length)
								listener?.onDownloadProgress(sum, length)
							}
							stream.close()
							fos.close()
							val actualLength = if (length == -1L) sum else length
							if (notify) notifyDownloadComplete(context, savePath)
							listener?.onDownloadComplete(savePath)
						}
					}
				} catch (e: Exception) {
					if (notify) notifyDownloadError(context, e.message)
					CoroutineScope(Dispatchers.Main).launch { Toast.makeText(context, "${context.getString(R.string.download_error)}:${e.message}", Toast.LENGTH_SHORT).show() }
				}
			}
		})
	}
	
	/**
	 * 打开文件
	 * 
	 * @param context 上下文对象
	 * @param path    文件路径
	 */
	@JvmStatic fun openFile(context: Context, path: String) {
		context.startActivity(getOpenFileIntent(context, path))
	}
	
	/**
	 * 获取打开文件的 Intent
	 * 
	 * @param context 上下文对象
	 * @param path    文件路径
	 * @return 打开文件的 Intent
	 */
	fun getOpenFileIntent(context: Context, path: String?): Intent? {
		return if (path == null) null
		else Intent.createChooser(Intent(Intent.ACTION_VIEW).addCategory("android.intent.category.DEFAULT")
			                          .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			                          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			                          .setDataAndType(FileProvider.getUriForFile(context, "com.sysu.edu.fileProvider", File(path)), MimeTypeMap.getSingleton().getMimeTypeFromExtension(path.substring(path.lastIndexOf(".") + 1).lowercase(Locale.getDefault()))),
		                          context.getString(R.string.share))
	}
	
	/**
	 * 下载监听器
	 */
	interface DownloadListener {
		/**
		 * 下载进度回调
		 * 
		 * @param progress 下载进度
		 */
		fun onDownloadProgress(progress: Long, total: Long)
		
		/**
		 * 下载完成回调
		 * 
		 * @param path 下载完成的文件路径
		 */
		fun onDownloadComplete(path: String?)
		
		/**
		 * 下载错误回调
		 * 
		 * @param code    错误码
		 * @param message 错误信息
		 */
		fun onDownloadError(code: Int, message: String?)
	}
	
	fun notifyDownloadProgress(context: Context, progress: Long, total: Long) {
		val indeterminate = total == -1L
		val progressString = if (indeterminate) String.format(Locale.getDefault(), "%.2fMB", progress / 1024.0f / 1024.0f) else String.format(Locale.getDefault(), "%.2fMB/%.2fMB", progress / 1024.0f / 1024.0f, total / 1024.0f / 1024.0f)
		val builder = NotificationCompat.Builder(context, "update")
			.setContentTitle(context.getString(R.string.download))
			.setContentText(progressString)
			.setSmallIcon(R.drawable.down)
			.setStyle(NotificationCompat.BigTextStyle().bigText(progressString))
			.setProgress(if (indeterminate) 0 else total.toInt(), if (indeterminate) 0 else progress.toInt(), indeterminate)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) NotificationManagerCompat.from(context).notify(1002, builder.build())
	}
	fun notifyDownloadComplete(context: Context, path: String?, message: String? = null) {
		val builder = NotificationCompat.Builder(context, "update")
			.setContentTitle(context.getString(R.string.download))
			.setContentText(message ?: "${context.getString(R.string.download_complete)}:$path")
			.setSmallIcon(R.drawable.down)
			.setContentIntent(getOpenFileIntent(context, path)?.let { it1 ->
				PendingIntentCompat.getActivity(context, 0, it1, PendingIntent.FLAG_ONE_SHOT, false)
			})
			.setProgress(1, 1, false)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) NotificationManagerCompat.from(context).notify(1002, builder.build())
		path?.let { it1 ->
			openFile(context, it1)
		}
	}
	fun notifyDownloadError(context: Context, message: String? = null) {
		val builder = NotificationCompat.Builder(context, "update")
			.setContentTitle(context.getString(R.string.download))
			.setContentText("${context.getString(R.string.download_error)}:$message")
			.setSmallIcon(R.drawable.down)
			.setProgress(1, 0, false)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) NotificationManagerCompat.from(context).notify(1002, builder.build())
	}
}