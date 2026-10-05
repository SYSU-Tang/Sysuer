package com.miyuyan.sysuer.academic

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.DownloadManager
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.IOException
import java.net.URL

/** 校历图片分组：一组标题（学年/学期）对应一组校历图片链接 */
data class CalendarSection(val title: String, val images: List<String>)

/**
 * 校历：从教务部官网抓取校历图片，按学年学期分组；支持保存到相册与分享
 */
class SchoolCalendarViewModel(application: Application) : AndroidViewModel(application) {
	private val _sections = MutableStateFlow<List<CalendarSection>>(emptyList())
	val sections: StateFlow<List<CalendarSection>> = _sections.asStateFlow()

	private val _uiState = MutableStateFlow(UiState.Unstarted)
	val uiState: StateFlow<UiState> = _uiState.asStateFlow()

	private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 16)
	val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

	init {
		load()
	}

	fun load() {
		if (_uiState.value == UiState.Loading) return
		_uiState.value = UiState.Loading
		viewModelScope.launch {
			val document = fetchDocument()
			if (document == null) {
				_uiState.value = UiState.Error
				return@launch
			}
			val sections = mutableListOf<CalendarSection>()
			var title = ""
			var images = mutableListOf<String>()
			document.selectFirst(".xiaoli")?.children()?.forEach { element ->
				if (element.tagName() == "h2") {
					if (images.isNotEmpty()) sections.add(CalendarSection(title, images))
					title = element.text()
					images = mutableListOf()
				} else {
					element.select(".xiaoliitem").forEach { item ->
						item.selectFirst("a")?.attr("href")?.takeIf { it.isNotEmpty() }?.let {
							images.add(BASE_URL + it)
						}
					}
				}
			}
			if (images.isNotEmpty()) sections.add(CalendarSection(title, images))
			_sections.value = sections
			_uiState.value = if (sections.isEmpty()) UiState.Empty else UiState.Content
		}
	}

	private suspend fun fetchDocument(): Document? = withContext(Dispatchers.IO) {
		repeat(RETRY_COUNT) {
			try {
				return@withContext Jsoup.connect(CALENDAR_URL).timeout(TIMEOUT_MILLIS).get()
			} catch (_: IOException) {
			}
		}
		null
	}

	/** 保存校历图片到相册 Pictures/SYSUER 目录 */
	fun saveImage(url: String) {
		viewModelScope.launch {
			val success = withContext(Dispatchers.IO) { saveImageInternal(url) }
			_snackbarMessage.tryEmit(
					getApplication<Application>().getString(
							if (success) R.string.save_successful else R.string.save_fail
					)
			)
		}
	}

	private fun saveImageInternal(url: String): Boolean {
		val data = download(url) ?: return false
		val context = getApplication<Application>()
		val fileName = "${System.currentTimeMillis()}.jpg"
		return try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				val uri = context.contentResolver.insert(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
						ContentValues().apply {
							put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
							put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
							put(
									MediaStore.MediaColumns.RELATIVE_PATH,
									Environment.DIRECTORY_PICTURES + "/SYSUER"
							)
						}
				) ?: return false
				context.contentResolver.openOutputStream(uri)?.use { it.write(data) } != null
			} else {
				val dir = File(
						Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
						"SYSUER"
				)
				dir.mkdirs()
				File(dir, fileName).writeBytes(data)
				true
			}
		} catch (_: Exception) {
			false
		}
	}

	/** 保存到缓存目录后调起系统查看/分享 */
	fun shareImage(url: String) {
		viewModelScope.launch {
			val data = withContext(Dispatchers.IO) { download(url) }
			if (data == null) {
				_snackbarMessage.tryEmit(
						getApplication<Application>().getString(R.string.save_fail)
				)
				return@launch
			}
			val file = File(
					getApplication<Application>().externalCacheDir,
					"${System.currentTimeMillis()}.jpg"
			)
			withContext(Dispatchers.IO) { file.writeBytes(data) }
			DownloadManager.openFile(getApplication(), file.path)
		}
	}

	private fun download(url: String): ByteArray? = try {
		val connection = URL(url).openConnection()
		connection.connectTimeout = TIMEOUT_MILLIS
		connection.readTimeout = TIMEOUT_MILLIS
		connection.getInputStream().use { it.readBytes() }
	} catch (_: Exception) {
		null
	}

	companion object {
		private const val CALENDAR_URL = "https://jwb.sysu.edu.cn/school-calendar"
		private const val BASE_URL = "https://jwb.sysu.edu.cn"
		private const val TIMEOUT_MILLIS = 3000
		private const val RETRY_COUNT = 3
	}
}
