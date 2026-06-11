package com.sysu.edu.api

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.sysu.edu.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

object DownloadManager {
	private val handler = Handler(Looper.getMainLooper())
	
	/**
	 * 下载网络文件到指定路径
	 * 
	 * @param context 上下文对象
	 * @param url     网络文件 URL
	 * @param path    本地文件保存路径
	 */
	@JvmStatic fun downloadFile(context: Activity?, url: String, path: String, listener: DownloadListener?) {
		downloadFile(context, Request.Builder().url(url).build(), path, listener)
	}
	
	/**
	 * 下载网络文件到指定路径
	 * 
	 * @param context 上下文对象
	 * @param url     网络文件 URL
	 * @param path    本地文件保存路径
	 */
	@JvmStatic fun downloadFile(context: Activity?, url: String, path: String) {
		downloadFile(context, Request.Builder().url(url).build(), path, null)
	}
	
	/**
	 * 下载网络文件到指定路径
	 * 
	 * @param context 上下文对象
	 * @param request 网络请求对象
	 */
	@JvmStatic fun downloadFile(context: Activity?, request: Request, path: String) {
		downloadFile(context, request, path, null)
	}
	
	/**
	 * 下载网络文件到指定路径
	 * 
	 * @param context  上下文对象
	 * @param request  网络请求对象
	 * @param path     本地文件保存路径
	 * @param listener 下载监听器
	 */
	@JvmStatic fun downloadFile(context: Context?, request: Request, path: String, listener: DownloadListener?) {
		OkHttpClient().newCall(request).enqueue(object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				println("下载网络文件报错：" + e.message)
				handler.post { Toast.makeText(context, "下载网络文件报错：" + e.message, Toast.LENGTH_SHORT).show() }
				listener?.onDownloadError(404, "下载网络文件报错：" + e.message)
			}
			
			override fun onResponse(call: Call, response: Response) {
//                MediaType type = response.body().contentType();
//                String mediaType = type == null ? "application/octet-stream" : type.toString();
				val length = response.body.contentLength()
				//                System.out.println("网络文件信息：" + String.format(Locale.getDefault(), "文件类型为%s，文件大小为%d", mediaType, length));
//                System.out.println("下载网络文件到：" + path);
				val parentFile = File(path).getParentFile()
				if (parentFile != null && !parentFile.isDirectory()) parentFile.mkdirs()
				try {
					response.body.byteStream().use { stream ->
						FileOutputStream(path).use { fos ->
							val buf = ByteArray(100 * 1024)
							var sum: Long = 0
							var len: Int
							while ((stream.read(buf).also { len = it }) != -1) {
								fos.write(buf, 0, len)
								sum += len.toLong()
								//                        String detail = String.format(Locale.getDefault(), "已下载%.2fKB", sum / 1024.0f);
								listener?.onDownloadProgress(sum, length)
								//                        System.out.println("下载进度：" + detail);
							}
							stream.close()
							fos.close()
							//                    System.out.println("下载完成");
							listener?.onDownloadComplete(path)
						}
					}
				} catch (e: Exception) {
					println("下载网络文件报错：" + e.message)
					handler.post { Toast.makeText(context, "下载网络文件报错：" + e.message, Toast.LENGTH_SHORT).show() }
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
		if (path == null) return null
		return Intent.createChooser(Intent(Intent.ACTION_VIEW)
										.addCategory("android.intent.category.DEFAULT")
										.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
										.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
										.setDataAndType(FileProvider.getUriForFile(context, "com.sysu.edu.fileProvider", File(path)), MimeTypeMap.getSingleton().getMimeTypeFromExtension(path.substring(path.lastIndexOf(".") + 1).lowercase(Locale.getDefault()))),
		                            context.getString(R.string.share)
		)
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
}
