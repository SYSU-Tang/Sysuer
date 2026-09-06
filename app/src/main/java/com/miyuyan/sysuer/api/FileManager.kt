package com.miyuyan.sysuer.api

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.source
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object FileManager {
	fun readAssets(context: Context, file: String): String {
		val jsJSON = StringBuilder()
		try {
			val input = InputStreamReader(context.assets.open(file))
			val buffer = BufferedReader(input)
			var line: String?
			while ((buffer.readLine().also { line = it }) != null) jsJSON.append(line).append("\n")
			input.close()
			buffer.close()
		} catch (_: IOException) {
		}
		return "$jsJSON"
	}
	
	fun getAttachmentRequestBody(context: Context, uri: Uri): FileRequestBody {
		val resolver: ContentResolver = context.contentResolver
		val type: String? = resolver.getType(uri)
		var resolvedFileName: String? = null
		var resolvedFileSize: Long = -1
		try {
			resolver.query(uri, null, null, null, null).use { cursor ->
				if (cursor != null && cursor.moveToFirst()) {
					val nameIndex: Int = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
					if (nameIndex != -1) {
						resolvedFileName = cursor.getString(nameIndex)
					}
					val sizeIndex: Int = cursor.getColumnIndex(OpenableColumns.SIZE)
					if (sizeIndex != -1) {
						resolvedFileSize = cursor.getLong(sizeIndex)
					}
				}
			}
		} catch (e: java.lang.Exception) {
			System.err.println("Failed to resolve file info: " + e.message)
		}
		val finalFileName = resolvedFileName ?: uri.lastPathSegment
		val finalFileSize = resolvedFileSize
		val requestBody: RequestBody = object : RequestBody() {
			override fun contentType(): okhttp3.MediaType? {
				return type?.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()
			}
			
			override fun contentLength(): Long = finalFileSize
			@Throws(IOException::class) override fun writeTo(sink: okio.BufferedSink) {
				resolver.openInputStream(uri).use { inputStream ->
					inputStream?.source().use { source ->
						source?.let { sink.writeAll(it) }
					}
				}
			}
		}
		return FileRequestBody(finalFileName, finalFileSize, requestBody)
	}
	data class FileRequestBody(val fileName: String?, val fileSize: Long, val file: RequestBody)
}

