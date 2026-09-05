package com.miyuyan.sysuer.academic

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.core.app.ActivityOptionsCompat
import androidx.core.net.toUri
import androidx.core.view.size
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.DownloadManager.openFile
import com.miyuyan.sysuer.browser.BrowserActivity
import com.miyuyan.sysuer.databinding.ActivityCalendarBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.Callable

class CalendarActivity : BaseActivity() {
	var top: Int = 0
	fun saveImage(url: String?, fileName: String): Boolean {
		return saveImage(url, Environment.DIRECTORY_PICTURES + "/SYSUER", fileName, true)
	}
	
	fun saveImage(
		url: String?,
		parentDir: String?,
		fileName: String,
		defaultDir: Boolean,
	             ): Boolean {
		var fileInputStream: FileInputStream? = null
		try {
			val resourceFile = Glide.with(this).asFile().load(url).submit().get()
			val fileUri = if (defaultDir) {
				contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
					put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
					put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
					put(MediaStore.MediaColumns.RELATIVE_PATH, parentDir)
				})
			}
			else {
				Uri.fromFile(File(parentDir, fileName))
			} ?: return false            // 3. 写入数据
			contentResolver.openOutputStream(fileUri).use { outStream ->
				if (outStream == null) return false
				fileInputStream = FileInputStream(resourceFile)
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					FileUtils.copy(fileInputStream, outStream)
				}
				else {
					val buffer = ByteArray(1024 * 4)
					var bytesRead: Int
					while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
						outStream.write(buffer, 0, bytesRead)
					}
				}
				outStream.flush()
			}
			return true
		} catch (_: Exception) {
			return false
		} finally {
			try {
				fileInputStream?.close()
			} catch (_: IOException) {
			}
		}
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityCalendarBinding.inflate(layoutInflater)
		setContentView(binding.getRoot())
		binding.toolbar.setNavigationOnClickListener { _: View? -> finishAfterTransition() }
		binding.scroll.setOnScrollChangeListener { _: NestedScrollView?, _: Int, scrollY: Int, _: Int, oldScrollY: Int ->
			if (top > scrollY && binding.tabs.selectedTabPosition == 1 && scrollY < oldScrollY) binding.tabs.getTabAt(0)?.select()
			else if (top <= scrollY && binding.tabs.selectedTabPosition == 0 && scrollY > oldScrollY) binding.tabs.getTabAt(1)?.select()
		}
		binding.tabs.addOnTabSelectedListener(object : OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab?) {
				if (binding.content.size > 2) top = binding.content.getChildAt(2).top
				when (binding.tabs.selectedTabPosition) {
					0 -> {
						if (binding.scroll.scrollY >= top) binding.scroll.smoothScrollTo(0, 0)
					}
					1 -> {
						if (binding.scroll.scrollY <= top) binding.scroll.smoothScrollTo(0, top)
					}
				}
			}
			
			override fun onTabUnselected(tab: TabLayout.Tab?) {
			}
			
			override fun onTabReselected(tab: TabLayout.Tab?) {
			}
		})
		config.contextUtil.disposable.add(Observable.fromCallable<Any>(Callable {
			Jsoup.connect("https://jwb.sysu.edu.cn/school-calendar").timeout(3000).get()
		}).retry(3).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({ a: Any ->
			                                                                                             (a as Document).selectFirst(".xiaoli")?.children()?.forEach { element: Element? ->
					                                                                                             if (element!!.tagName() == "h2") binding.tabs.addTab(binding.tabs.newTab().setText(element.text()))
					                                                                                             else  // if (element.className().equals("row"))
						                                                                                             element.select(".xiaoliitem").forEach { li: Element? ->
								                                                                                             val image = ImageView(this@CalendarActivity)
								                                                                                             val url = "https://jwb.sysu.edu.cn" + li!!.selectFirst("a")?.attr("href") //																				 println(url)
								                                                                                             Glide.with(this@CalendarActivity).load(url).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(image)
								                                                                                             image.setOnClickListener {
									                                                                                             startActivity(Intent(this@CalendarActivity, BrowserActivity::class.java).setData(url.toUri()),
									                                                                                                           ActivityOptionsCompat.makeSceneTransitionAnimation(this@CalendarActivity, image, "miniapp").toBundle())
								                                                                                             }
								                                                                                             image.setOnLongClickListener { _: View? ->
									                                                                                             val pop = PopupMenu(this@CalendarActivity,
									                                                                                                                 image,
									                                                                                                                 0,
									                                                                                                                 0,
									                                                                                                                 com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow)
									                                                                                             val menu = pop.menu
									                                                                                             menu.add(R.string.save).setOnMenuItemClickListener { _: MenuItem? ->
											                                                                                             config.contextUtil.disposable.add(Observable.fromCallable {
												                                                                                             saveImage(url, System.currentTimeMillis().toString() + ".jpg")
											                                                                                             }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({ success ->
												                                                                                                                                                                                config.toast(if (success) R.string.save_successful else R.string.save_fail)
											                                                                                                                                                                                }, {
												                                                                                                                                                                                config.toast(R.string.save_fail)
											                                                                                                                                                                                }))
											                                                                                             config.toast(if (saveImage(url,
											                                                                                                                        System.currentTimeMillis().toString() + ".jpg")) R.string.save_successful
											                                                                                                          else R.string.save_fail)
											                                                                                             true
										                                                                                             }
									                                                                                             menu.add(R.string.copy_link).setOnMenuItemClickListener { _: MenuItem? ->
											                                                                                             config.copy("link", url)
											                                                                                             config.toast(R.string.copy_successfully)
											                                                                                             true
										                                                                                             }
									                                                                                             menu.add(R.string.share).setOnMenuItemClickListener { _: MenuItem? ->
											                                                                                             val fileName = System.currentTimeMillis().toString() + ".jpg"
											                                                                                             config.contextUtil.disposable.add(Observable.fromCallable {
												                                                                                             saveImage(url, externalCacheDir?.path, fileName, false)
											                                                                                             }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({ success ->
												                                                                                                                                                                                if (success && externalCacheDir != null) {
													                                                                                                                                                                                openFile(this@CalendarActivity,
													                                                                                                                                                                                         externalCacheDir!!.path + "/" + fileName)
												                                                                                                                                                                                }
												                                                                                                                                                                                else {
													                                                                                                                                                                                config.toast(R.string.save_fail) // 或者自定义提示
												                                                                                                                                                                                }
											                                                                                                                                                                                }, {
												                                                                                                                                                                                config.toast(R.string.save_fail)
											                                                                                                                                                                                }))
											                                                                                             openFile(this@CalendarActivity, externalCacheDir!!.path + "/" + fileName)
											                                                                                             true
										                                                                                             }
									                                                                                             pop.show()
									                                                                                             true
								                                                                                             }
								                                                                                             binding.content.addView(image)
							                                                                                             }
				                                                                                             }
			                                                                                             binding.progressBar.visibility = View.GONE
		                                                                                             }, {
			                                                                                             config.toast(R.string.no_net_connected)
		                                                                                             }))
	}
}