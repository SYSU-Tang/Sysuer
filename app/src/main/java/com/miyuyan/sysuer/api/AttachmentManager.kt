package com.miyuyan.sysuer.api

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * 待办附件管理:实体文件统一存放在应用私有目录 `filesDir/todo_attachments/`,
 * 元数据 JSON 存入 `todos.attachment` 字段。
 *
 * 元数据字段:
 * - `id`:uuid,用于追踪本次编辑会话新增的条目(取消保存时清理孤儿文件)
 * - `type`:`image` / `audio` / `file`
 * - `name`:显示名(原始文件名,或拍照/录音自动命名)
 * - `mime`:MIME 类型
 * - `size`:字节数
 * - `path`:相对 [Context.getFilesDir] 的相对路径
 * - `duration`:音频时长毫秒数,仅音频条目有
 * - `addedAt`:添加时间 `yyyy-MM-dd HH:mm:ss`
 */
object AttachmentManager {

	private const val DIR_NAME = "todo_attachments"
	private const val AUTHORITY_SUFFIX = ".fileProvider"

	/** 元数据里的 path(相对 filesDir) → 实体 File */
	fun resolveFile(context: Context, path: String): File = File(context.filesDir, path)

	fun attachmentDir(context: Context): File =
		File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

	// ------------------------------------------------------------------
	// 从 content Uri 导入(相册照片、文件选择)
	// ------------------------------------------------------------------

	/**
	 * 把 content Uri 指向的内容拷贝进私有目录并生成附件元数据。
	 *
	 * @return 元数据 JSON;拷贝失败返回 null
	 */
	fun saveFromUri(context: Context, uri: Uri): JSONObject? {
		val resolver = context.contentResolver
		var fileName: String? = null
		var fileSize: Long = -1
		try {
			resolver.query(uri, null, null, null, null).use { cursor ->
				if (cursor?.moveToFirst() == true) {
					val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
					if (nameIndex != -1) fileName = cursor.getString(nameIndex)
					val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
					if (sizeIndex != -1) {
						if (!cursor.isNull(sizeIndex)) fileSize = cursor.getLong(sizeIndex)
					}
				}
			}
		} catch (_: Exception) {
		}
		val mime = resolver.getType(uri)
		return importStream(
				context,
				streamProvider = {
					resolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
				},
				displayName = fileName,
				mime = mime,
				knownSize = fileSize,
		)
	}

	/**
	 * 从输入流导入并生成附件元数据,拍照与 Uri 导入共用此落盘逻辑。
	 *
	 * @param streamProvider 输入流提供者,在 IO 线程被调用
	 * @param displayName    原始文件名,可空
	 * @param mime           已知 MIME,可空(将按扩展名猜测)
	 * @param knownSize      已知大小,未知传 -1(拷贝后统计)
	 */
	private fun importStream(
		context: Context,
		streamProvider: () -> InputStream,
		displayName: String?,
		mime: String?,
		knownSize: Long,
	): JSONObject? {
		val id = UUID.randomUUID().toString()
		val dir = attachmentDir(context)
		val extension = displayName?.substringAfterLast('.', "")?.takeIf {
			it.length <= 8 && !it.contains('/') && !it.contains(File.separatorChar)
		} ?: mime?.let {
			MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
		} ?: ""
		val target = File(dir, "$id${if (extension.isEmpty()) "" else ".$extension"}")
		return try {
			var copied = 0L
			streamProvider().use { input ->
				target.outputStream().use { output ->
					val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
					while (true) {
						val read = input.read(buffer)
						if (read == -1) break
						output.write(buffer, 0, read)
						copied += read
					}
				}
			}
			val size = if (knownSize >= 0) knownSize else copied
			val finalMime = mime ?: MimeTypeMap.getSingleton()
				.getMimeTypeFromExtension(extension.lowercase())
			buildEntry(
					context = context,
					id = id,
					file = target,
					displayName = displayName ?: target.name,
					mime = finalMime,
					size = size,
					duration = null,
			)
		} catch (_: Exception) {
			target.delete()
			null
		}
	}

	// ------------------------------------------------------------------
	// 拍照
	// ------------------------------------------------------------------

	/** 拍照目标:先创建空文件占位,再取 FileProvider Uri 交给系统相机写回 */
	fun createCameraTarget(context: Context): Pair<Uri, File> {
		val file = File(attachmentDir(context), "${UUID.randomUUID()}.jpg")
		file.createNewFile()
		val uri = FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
		return uri to file
	}

	/** 拍照成功后把占位文件转成附件元数据;失败/取消时清理占位文件 */
	fun commitCameraFile(context: Context, file: File): JSONObject? {
		if (!(file.isFile && file.length() > 0L)) {
			file.delete()
			return null
		}
		val id = file.nameWithoutExtension
		return buildEntry(
				context = context,
				id = id,
				file = file,
				displayName = "IMG_${nowStamp()}.jpg",
				mime = "image/jpeg",
				size = file.length(),
				duration = null,
		)
	}

	// ------------------------------------------------------------------
	// 元数据组装
	// ------------------------------------------------------------------

	private fun buildEntry(
		context: Context,
		id: String,
		file: File,
		displayName: String,
		mime: String?,
		size: Long,
		duration: Long?,
	): JSONObject {
		val entry = JSONObject.of(
				"id", id,
				"type", classifyType(displayName, mime, duration),
				"name", displayName,
				"mime", mime ?: "application/octet-stream",
				"size", size,
				"path", file.relativeTo(context.filesDir).invariantSeparatorsPath,
		)
		duration?.takeIf { it > 0 }?.let { entry["duration"] = it }
		entry["addedAt"] = LocalDateTime.now()
			.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
		return entry
	}

	private fun classifyType(name: String, mime: String?, duration: Long?): String {
		if ((duration ?: 0L) > 0) return "audio"
		val m = mime ?: ""
		if (m.startsWith("image/")) return "image"
		if (m.startsWith("audio/")) return "audio"
		val lower = name.lowercase()
		if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
			lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp")
		) return "image"
		if (lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".mp3") ||
			lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".amr") ||
			lower.endsWith(".flac")
		) return "audio"
		return "file"
	}

	// ------------------------------------------------------------------
	// 录音
	// ------------------------------------------------------------------

	/**
	 * 创建并准备好录音器(AAC/M4A),**尚未开始录制**。
	 * 由调用方决定何时 [startRecording] / [pauseRecording] / [resumeRecording]。
	 *
	 * @return 录音器与输出文件;准备失败时文件已被清理并返回 null
	 */
	fun prepareRecording(context: Context): Pair<MediaRecorder, File>? {
		val file = File(attachmentDir(context), "${UUID.randomUUID()}.m4a")
		val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			MediaRecorder(context)
		} else {
			@Suppress("DEPRECATION") MediaRecorder()
		}
		return try {
			recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
			recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
			recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
			recorder.setAudioSamplingRate(44100)
			recorder.setAudioEncodingBitRate(128000)
			recorder.setAudioChannels(1)
			recorder.setOutputFile(file.absolutePath)
			recorder.prepare()
			recorder to file
		} catch (_: Exception) {
			recorder.release()
			file.delete()
			null
		}
	}
	
	/** 开始录制。返回 false 表示设备/状态不支持,调用方应回退到未录制态 */
	fun startRecording(recorder: MediaRecorder): Boolean = try {
		recorder.start()
		true
	} catch (_: Exception) {
		false
	}
	
	/** 暂停录制(API 24+) */
	fun pauseRecording(recorder: MediaRecorder): Boolean = try {
		recorder.pause()
		true
	} catch (_: Exception) {
		false
	}
	
	/** 从暂停处继续录制(API 24+) */
	fun resumeRecording(recorder: MediaRecorder): Boolean = try {
		recorder.resume()
		true
	} catch (_: Exception) {
		false
	}

	/** 停止录音并生成附件元数据;录音过短(如 0 字节)时清理并返回 null */
	fun stopRecording(context: Context, recorder: MediaRecorder, file: File): JSONObject? {
		return try {
			recorder.stop()
			if (!file.isFile || file.length() == 0L) {
				file.delete()
				null
			} else {
				val duration = queryAudioDuration(file)
				buildEntry(
						context = context,
						id = file.nameWithoutExtension,
						file = file,
						displayName = "REC_${nowStamp()}.m4a",
						mime = "audio/mp4",
						size = file.length(),
						duration = duration,
				)
			}
		} catch (_: Exception) {
			file.delete()
			null
		} finally {
			recorder.release()
		}
	}

	/** 取消录音:释放资源并丢弃半成品文件 */
	fun cancelRecording(recorder: MediaRecorder, file: File) {
		try {
			recorder.stop()
		} catch (_: Exception) {
		}
		recorder.release()
		file.delete()
	}

	private fun queryAudioDuration(file: File): Long {
		val retriever = MediaMetadataRetriever()
		return try {
			retriever.setDataSource(file.absolutePath)
			val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
			ms?.toLongOrNull() ?: 0L
		} catch (_: Exception) {
			0L
		} finally {
			retriever.release()
		}
	}

	// ------------------------------------------------------------------
	// 删除 / 复制 / 打开
	// ------------------------------------------------------------------

	/** 删除附件条目对应的物理文件 */
	fun deleteAttachment(context: Context, attachment: JSONObject) {
		val path = attachment.getString("path", "")
		if (path.isNotEmpty()) {
			val file = resolveFile(context, path)
			if (file.isFile && file.parentFile?.name == DIR_NAME) file.delete()
		}
	}

	/** 删除一组附件的全部物理文件(删除待办时级联清理) */
	fun deleteAttachments(context: Context, attachments: JSONArray) {
		attachments.forEach { entry ->
			(entry as? JSONObject)?.let { deleteAttachment(context, it) }
		}
	}

	/** 复制待办时深拷贝附件:每个文件复制为新 uuid,原附件文件不动 */
	fun duplicateAttachments(context: Context, attachments: Iterable<JSONObject>): MutableList<JSONObject> {
		val result = mutableListOf<JSONObject>()
		val dir = attachmentDir(context)
		attachments.forEach { source ->
			val sourceFile = resolveFile(context, source.getString("path", ""))
			if (!sourceFile.isFile) return@forEach
			val newId = UUID.randomUUID().toString()
			val newName = newId + "." + sourceFile.extension
			val target = File(dir, newName)
			try {
				sourceFile.copyTo(target, overwrite = false)
				val copy = JSONObject.parseObject(source.toJSONString())
				copy["id"] = newId
				copy["path"] = target.relativeTo(context.filesDir).invariantSeparatorsPath
				result.add(copy)
			} catch (_: Exception) {
				target.delete()
			}
		}
		return result
	}

	/** 用外部应用打开附件(FileProvider + ACTION_VIEW),图片/音频/文件通用 */
	fun openWithViewer(context: Context, attachment: JSONObject) {
		val file = resolveFile(context, attachment.getString("path", ""))
		if (!file.isFile) return
		val intent = Intent(Intent.ACTION_VIEW)
			.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			.setDataAndType(fileProviderUri(context, file), mimeOf(attachment, file))
		try {
			context.startActivity(intent)
		} catch (_: ActivityNotFoundException) {
			try {
				context.startActivity(Intent.createChooser(intent, null))
			} catch (_: Exception) {
			}
		}
	}
	
	/** 「打开为」:总是弹出应用选择器(ACTION_VIEW + chooser) */
	fun openAttachmentAs(context: Context, attachment: JSONObject) {
		val file = resolveFile(context, attachment.getString("path", ""))
		if (!file.isFile) return
		openFileAs(context, file, mimeOf(attachment, file))
	}
	
	/** 「打开为」任意本地文件:弹出应用选择器 */
	fun openFileAs(context: Context, file: File, mime: String) {
		val intent = Intent(Intent.ACTION_VIEW)
			.addCategory(Intent.CATEGORY_DEFAULT)
			.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			.setDataAndType(fileProviderUri(context, file), mime)
		val chooser = Intent.createChooser(intent, context.getString(R.string.open_as))
		// createChooser 不会继承 NEW_TASK,非 Activity 上下文需补上
		if (context.findActivity() == null) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		try {
			context.startActivity(chooser)
		} catch (_: Exception) {
		}
	}
	
	/** 分享附件:ACTION_SEND + FileProvider Uri */
	fun shareAttachment(context: Context, attachment: JSONObject) {
		val file = resolveFile(context, attachment.getString("path", ""))
		if (!file.isFile) return
		shareFile(context, file, mimeOf(attachment, file))
	}
	
	/** 分享任意本地文件:ACTION_SEND + FileProvider Uri */
	fun shareFile(context: Context, file: File, mime: String) {
		val intent = Intent(Intent.ACTION_SEND)
			.setType(mime)
			.putExtra(Intent.EXTRA_STREAM, fileProviderUri(context, file))
			.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		val chooser = Intent.createChooser(intent, context.getString(R.string.share))
		if (context.findActivity() == null) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		try {
			context.startActivity(chooser)
		} catch (_: Exception) {
		}
	}
	
	/**
	 * 「保存到」相册(API 29+):写入 `Pictures/SYSUER`,无需任何存储权限。
	 *
	 * @return 是否保存成功。低于 Q 的设备返回 false,由调用方回退到 SAF 另存为。
	 */
	fun saveImageToGallery(context: Context, attachment: JSONObject): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
		val file = resolveFile(context, attachment.getString("path", ""))
		if (!file.isFile) return false
		val mime = mimeOf(attachment, file)
		val displayName = attachment.getString("name", "")
			.ifEmpty { "IMG_${nowStamp()}.jpg" }
		return try {
			val resolver = context.contentResolver
			val values = ContentValues().apply {
				put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
				put(MediaStore.MediaColumns.MIME_TYPE, mime)
				put(
						MediaStore.MediaColumns.RELATIVE_PATH,
						Environment.DIRECTORY_PICTURES + "/SYSUER",
				)
				put(MediaStore.MediaColumns.IS_PENDING, 1)
			}
			val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
					?: return false
			val copied = try {
				resolver.openOutputStream(uri)?.use { output ->
					file.inputStream().use { it.copyTo(output) }
				} != null
			} catch (_: Exception) {
				false
			}
			if (copied) {
				values.clear()
				values.put(MediaStore.MediaColumns.IS_PENDING, 0)
				resolver.update(uri, values, null, null)
			} else {
				resolver.delete(uri, null, null)
			}
			copied
		} catch (_: Exception) {
			false
		}
	}
	
	/** SAF 另存为:把私有文件写入用户选定的目标 Uri */
	fun copyToUri(context: Context, attachment: JSONObject, target: Uri): Boolean {
		val file = resolveFile(context, attachment.getString("path", ""))
		return file.isFile && try {
			context.contentResolver.openOutputStream(target)?.use { output ->
				file.inputStream().use { it.copyTo(output) }
			} != null
		} catch (_: Exception) {
			false
		}
	}
	
	private fun fileProviderUri(context: Context, file: File): Uri =
		FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
	
	private fun mimeOf(attachment: JSONObject, file: File): String =
		attachment.getString("mime", "")
			?.takeIf { it.isNotEmpty() && it != "application/octet-stream" }
			?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
				?: "application/octet-stream"
	
	/** 向上查找 Context 链中包裹的 Activity,用于判断是否需要补 NEW_TASK */
	private tailrec fun Context.findActivity(): Activity? = when (this) {
		is Activity -> this
		is ContextWrapper -> baseContext.findActivity()
		else -> null
	}

	private fun nowStamp(): String =
		LocalDateTime.now()
			.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

	private const val DEFAULT_BUFFER_SIZE = 8192
}
