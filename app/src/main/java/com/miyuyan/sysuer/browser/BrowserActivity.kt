package com.miyuyan.sysuer.browser

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebView.WebViewTransport
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityOptionsCompat
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.trim
import com.miyuyan.sysuer.api.DownloadManager
import com.miyuyan.sysuer.api.DownloadManager.downloadFile
import com.miyuyan.sysuer.api.DownloadManager.openFile
import com.miyuyan.sysuer.browser.data.BrowserRepository
import com.miyuyan.sysuer.browser.data.JavaScriptEntity
import com.miyuyan.sysuer.browser.data.JsModel
import com.miyuyan.sysuer.browser.data.JsModelFactory
import com.miyuyan.sysuer.browser.data.ScriptManager
import com.miyuyan.sysuer.databinding.ActivityBrowserBinding
import com.miyuyan.sysuer.databinding.DialogJsBinding
import com.miyuyan.sysuer.databinding.ItemPreferenceBinding
import com.miyuyan.sysuer.view.AdapterListener
import com.miyuyan.sysuer.view.EditTextDialog
import com.miyuyan.sysuer.view.GridMenuDialog
import com.miyuyan.sysuer.view.RecyclerAdapter
import com.miyuyan.sysuer.view.SysuerWebView
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.regex.Pattern

class BrowserActivity : BaseActivity() {
	val progress: MutableLiveData<Int?> = MutableLiveData<Int?>()
	val disposable: CompositeDisposable by lazy { config.contextUtil.disposable }
	lateinit var web: SysuerWebView
	lateinit var binding: ActivityBrowserBinding
	lateinit var webSettings: WebSettings
	val cookieManager: CookieManager = CookieManager.getInstance()
	val httpCookieManager by lazy { com.miyuyan.sysuer.api.CookieManager(this) }
	var refreshButton: MaterialButton? = null
	val repository: BrowserRepository by lazy {
		BrowserRepository(this, lifecycleScope)
	}
	val model: JsModel by lazy {
		ViewModelProvider(this, JsModelFactory(repository))[JsModel::class.java]
	}
	private var gmBridge: com.miyuyan.sysuer.browser.data.GMBridge? = null
	@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility") override fun onCreate(
		savedInstanceState: Bundle?,
	                                                                                         ) {
		super.onCreate(savedInstanceState)
		binding = ActivityBrowserBinding.inflate(layoutInflater)
		setContentView(binding.root)
		binding.toolbar.setNavigationOnClickListener { finishAfterTransition() }
		val preference = BrowserPreference(this)
		val okHttpClient = OkHttpClient()
		val url = intent.dataString ?: "https://www.sysu.edu.cn/"
		model.loadJs()
		web = binding.web.apply {
			gmBridge = com.miyuyan.sysuer.browser.data.GMBridge(this@BrowserActivity)
			addJavascriptInterface(gmBridge!!, "AndroidGM")
			isFocusable = true
			isFocusableInTouchMode = true
			requestFocus()
			webViewClient = object : WebViewClient() {
				override fun shouldOverrideUrlLoading(
					view: WebView,
					request: WebResourceRequest,
				                                     ): Boolean {
					val url1 = request.url.toString()
					if (url1.startsWith("https://") || url1.startsWith("http://")) view.loadUrl(url1)
					else {
						Intent(Intent.ACTION_VIEW).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
							.setData(url1.toUri())
							.takeIf { it.resolveActivity(packageManager) != null }
							?.let {
								startActivity(it)
							}
					}
					return true
				}
				
				override fun shouldInterceptRequest(
					view: WebView?,
					request: WebResourceRequest,
				                                   ): WebResourceResponse? {
					val url1 = request.url.toString()
					if (Pattern.compile("//jwxt.sysu.edu.cn/jwxt/system-manage/infoRelease/downloadFile",
					                    Pattern.DOTALL).matcher(url1).find()) {
						try {
							val response = okHttpClient.newCall(Request.Builder()
								                                    .url(url1)
								                                    .header("Cookie",
								                                            cookieManager.getCookie(url1))
								                                    .header("Referer",
								                                            "https://jwxt.sysu.edu.cn/jwxt/")
								                                    .build()).execute()
							val mediaType = response.body.contentType()
							return WebResourceResponse(mediaType?.type
								                           ?: "application/octet-stream",
							                           "utf-8",
							                           response.body.byteStream())
						} catch (_: IOException) {
						}
					}
					return super.shouldInterceptRequest(view, request)
				}
				
				override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
					gmBridge?.clearCommands()
					model.js.value?.let {
						ScriptManager.executeScripts(web,
						                             ScriptManager.getMatchingScripts(url ?: "",
						                                                              it),
						                             "document-start")
					}
					super.onPageStarted(view, url, favicon)
				}
				
				override fun onPageFinished(view: WebView, link: String) {
					if (preference.isPC) view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));",
					                                             null)
					model.js.value?.let {
						ScriptManager.executeScripts(web,
						                             ScriptManager.getMatchingScripts(url, it),
						                             listOf("document-idle", "document-end"))
					}
					super.onPageFinished(view, link)
				}
			}
			webChromeClient = object : WebChromeClient() {
				override fun onJsConfirm(
					view: WebView?,
					url: String?,
					message: String?,
					result: JsResult,
				                        ): Boolean {
					MaterialAlertDialogBuilder(this@BrowserActivity).setMessage(message)
						.setPositiveButton(R.string.confirm) { _: DialogInterface?, _: Int -> result.confirm() }
						.create()
						.show()
					return true
				}
				
				override fun onJsAlert(
					view: WebView?,
					url: String?,
					message: String?,
					result: JsResult,
				                      ): Boolean {
					MaterialAlertDialogBuilder(this@BrowserActivity).setMessage(message)
						.setPositiveButton(R.string.confirm) { _: DialogInterface?, _: Int -> result.confirm() }
						.create()
						.show()
					return true
				}
				
				override fun onCreateWindow(
					view: WebView?,
					isDialog: Boolean,
					isUserGesture: Boolean,
					resultMsg: Message,
				                           ): Boolean {
					val newWebView = WebView(this@BrowserActivity)
					newWebView.setWebViewClient(object : WebViewClient() {
						override fun shouldOverrideUrlLoading(
							view: WebView?,
							request: WebResourceRequest,
						                                     ): Boolean {
							web.loadUrl(request.url.toString())
							newWebView.destroy()
							return super.shouldOverrideUrlLoading(view, request)
						}
					})
					val transport = resultMsg.obj as WebViewTransport
					transport.webView = newWebView
					resultMsg.sendToTarget()
					return true
				}
				
				override fun onReceivedTitle(view: WebView, title: String?) {
					binding.toolbar.title = title
					binding.toolbar.subtitle = view.url
					super.onReceivedTitle(view, title)
				}
				
				override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
					binding.toolbar.setLogo(BitmapDrawable(resources, icon))
					binding.toolbar.isLogoAdjustViewBounds = true
					binding.toolbar.setLogoScaleType(ImageView.ScaleType.FIT_CENTER)
					super.onReceivedIcon(view, icon)
				}
				
				override fun onProgressChanged(view: WebView?, newProgress: Int) {
					super.onProgressChanged(view, newProgress)
					this@BrowserActivity.progress.postValue(newProgress)
				}
			}
		}        /*
         * 下载弹窗
         * */
		val downloadDialog = GridMenuDialog(this).apply {
			setColumn(1)
			set<Int?>(mutableListOf(R.string.link, R.string.location),
			          mutableListOf(R.drawable.link, R.drawable.save),
			          mutableListOf(GridMenuDialog.onGridMenuClickListener {
				          startActivity(Intent(Intent.ACTION_VIEW,
				                               getMenu(0)?.text.toString()
					                               .toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
			          }, GridMenuDialog.onGridMenuClickListener { dialog.dismiss() }))
			setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START)
			setGravity(Gravity.START or Gravity.CENTER_VERTICAL)
			getMenu(0)?.setMaxLines(Int.MAX_VALUE)
			getMenu(0)?.setOnLongClickListener {
				config.copy("link", getMenu(0)?.text.toString())
				config.toast(R.string.copy_successfully)
				true
			}
			setNegativeButton(R.string.cancel) { _, _: Int -> }
		}
		
		web.setDownloadListener { downloadLink: String, _: String?, _: String?, _: String?, _: Long ->
			val path = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${getFileName(downloadLink)}"
			downloadDialog.getMenu(0)?.text = downloadLink
			downloadDialog.getMenu(1)?.text = path
			downloadDialog.setPositiveButton(R.string.download) { _: DialogInterface?, _: Int ->
				println(downloadLink)
				if (downloadLink.toHttpUrlOrNull()?.host == "jwxt.sysu.edu.cn") downloadFile(this,
				                                                    Request.Builder()
					                                                      .url(downloadLink)
					                                                      .header("Cookie", httpCookieManager.toSimpleString("jwxt.sysu.edu.cn"))
					                                                      .header("Referer",
					                                                              "https://jwxt.sysu.edu.cn/")
					                                                      .build(),
				                                                    path)
				else downloadFile(this, downloadLink, path)
				downloadDialog.dismiss()
			}
			downloadDialog.show()
		}
		webSettings = web.settings.apply {
			supportZoom()
			javaScriptEnabled = preference.isJSEnabled
			setSupportMultipleWindows(true)
			useWideViewPort = true
			loadWithOverviewMode = true
			setSupportZoom(true)
			builtInZoomControls = true
			displayZoomControls = false
			cacheMode = if (preference.isSaveMobileDataMode) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
			javaScriptCanOpenWindowsAutomatically = true
			loadsImagesAutomatically = true
			blockNetworkImage = preference.isImageBlocked
			defaultTextEncodingName = "utf-8"
		}
		
		setPrivacyMode(preference.isPrivacyMode)
		cookieManager.setAcceptCookie(preference.isCookieAccept)
		cookieManager.setAcceptThirdPartyCookies(web, preference.isThirdPartyCookieAccept)/*
         * 长按菜单
         * */
		val anchorView = View(this)
		anchorView.setLayoutParams(FrameLayout.LayoutParams(1, 1))
		(web.parent as FrameLayout).addView(anchorView)
		val gesture = GestureDetector(this, object : SimpleOnGestureListener() {
			override fun onLongPress(e: MotionEvent) {
				anchorView.x = e.x
				anchorView.y = e.y
				val pop = PopupMenu(this@BrowserActivity, anchorView)
				val result = web.getHitTestResult()
				val type = result.type
				val extra = result.extra
				when (type) {
					WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
						if (!TextUtils.isEmpty(extra)) showLinkMenu(extra!!, pop)
					}
					WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
						if (!TextUtils.isEmpty(extra)) showImageMenu(extra!!, pop)
					}
				}
			}
		})
		web.setOnTouchListener { v: View?, event: MotionEvent? ->
			if (event?.action == MotionEvent.ACTION_DOWN) {
				v?.requestFocus()
			}
			gesture.onTouchEvent(event!!)
			false
		}/*
         * 脚本弹窗
         * */
		val jsDialog = BottomSheetDialog(this)
		val jsBinding = DialogJsBinding.inflate(layoutInflater)
		jsDialog.setContentView(jsBinding.root)
		jsDialog.setTitle(R.string.js)
		val jsAdapter = JSAdapter()
		jsAdapter.listener = object : AdapterListener {
			override fun onBind(
				adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
				holder: RecyclerView.ViewHolder,
				position: Int,
			                   ) {
				val item = jsAdapter.get(position)
				holder.itemView.setOnClickListener {
					ScriptManager.executeScript(item, web)
				}
				holder.itemView.setOnLongClickListener { v: View? ->
					val pop = PopupMenu(this@BrowserActivity, v!!)
					pop.menuInflater.inflate(R.menu.js_item_menu, pop.menu)
					val scriptId = "${item.title}_${item.namespace ?: ""}"
					gmBridge?.getCommands(scriptId)?.forEach { commandName ->
						pop.menu.add(0, 0, 0, commandName).setOnMenuItemClickListener {
							web.evaluateJavascript("if (window.gm_commands && window.gm_commands['${scriptId}_${commandName}']) window.gm_commands['${scriptId}_${commandName}']();",
							                       null)
							true
						}
					}
					
					pop.menu.add(0, R.id.run, 1, R.string.run)
					pop.show()
					pop.menu.findItem(R.id.ban)
						.setTitle(if (item.state == 1) R.string.disable else R.string.enable)
					pop.setOnMenuItemClickListener { menuItem: MenuItem ->
						when (menuItem.itemId) {
							R.id.edit -> {
								v.transitionName = "script"
								startActivity(Intent(this@BrowserActivity,
								                     JSActivity::class.java).putExtras(Bundle().apply {
									putLong("id", item.id)
								}).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
								return@setOnMenuItemClickListener true
							}
							R.id.delete -> {
								jsAdapter.remove(position)
								model.deleteJs(item)
								return@setOnMenuItemClickListener true
							}
							R.id.ban -> {
								item.state = 1 - item.state
								model.updateJs(item)
								jsAdapter.notifyItemChanged(position)
								return@setOnMenuItemClickListener true
							}
							R.id.run -> {
								v.performClick()
								return@setOnMenuItemClickListener true
							}
							else -> false
						}
					}
					false
				}
			}
			
			override fun onCreate(
				adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
				binding: ViewBinding?,
			                     ) {
			}
		}
		jsBinding.recyclerView.root.apply {
			layoutManager = LinearLayoutManager(this@BrowserActivity)
			adapter = jsAdapter
		}
		jsBinding.manage.setOnClickListener { v: View? ->
			startActivity(Intent(this, JSActivity::class.java),
			              ActivityOptionsCompat.makeSceneTransitionAnimation(this, v!!, "miniapp")
				              .toBundle())
		}
		jsBinding.add.setOnClickListener { v: View? ->
			startActivity(Intent(this, JSActivity::class.java).putExtra("operation", "add"),
			              ActivityOptionsCompat.makeSceneTransitionAnimation(this, v!!, "miniapp")
				              .toBundle())
		}        /*
         * 菜单弹窗
         * */
		val menuDialog = GridMenuDialog(this)
		menuDialog.set<Int?>(mutableListOf(R.string.back,
		                                   R.string.forward,
		                                   R.string.refresh,
		                                   R.string.exit,
		                                   R.string.page_up,
		                                   R.string.page_down,
		                                   R.string.zoom_in,
		                                   R.string.zoom_out,
		                                   R.string.find_text),
		                     mutableListOf(R.drawable.left,
		                                   R.drawable.right,
		                                   R.drawable.refresh,
		                                   R.drawable.exit,
		                                   R.drawable.up,
		                                   R.drawable.down,
		                                   R.drawable.zoom_in,
		                                   R.drawable.zoom_out,
		                                   R.drawable.search),
		                     mutableListOf(GridMenuDialog.onGridMenuClickListener { goBack() },
		                                   GridMenuDialog.onGridMenuClickListener { goForward() },
		                                   GridMenuDialog.onGridMenuClickListener { refresh() },
		                                   GridMenuDialog.onGridMenuClickListener { supportFinishAfterTransition() },
		                                   GridMenuDialog.onGridMenuClickListener { pageUp() },
		                                   GridMenuDialog.onGridMenuClickListener { pageDown() },
		                                   GridMenuDialog.onGridMenuClickListener { web.zoomIn() },
		                                   GridMenuDialog.onGridMenuClickListener { web.zoomOut() },
		                                   GridMenuDialog.onGridMenuClickListener {
			                                   binding.searchContainer.visibility = View.VISIBLE
			                                   web.findAllAsync(binding.keyword.text.toString())
			                                   menuDialog.dismiss()
		                                   }))
		refreshButton = menuDialog.getMenu(2)
		/*
         * UA 弹窗
         * */
		val uaDialog = GridMenuDialog(this).apply {
			setColumn(2)
			setSelectable(true)
			setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START)
			setGravity(Gravity.CENTER_VERTICAL or Gravity.START)
			add(R.string.follow_system, R.drawable.setting) {
				webSettings.userAgentString = WebSettings.getDefaultUserAgent(this@BrowserActivity)
				preference.ua = -1
				web.reload()
			}
		}
		lifecycleScope.launch {
			val ua = withContext(Dispatchers.IO) {
				repository.getAllUserAgents()
			}
			val iconList = listOf(R.drawable.laptop,
			                      R.drawable.laptop,
			                      R.drawable.laptop,
			                      R.drawable.mac,
			                      R.drawable.android,
			                      R.drawable.tablet,
			                      R.drawable.iphone,
			                      R.drawable.ipad,
			                      R.drawable.ua,
			                      R.drawable.laptop,
			                      R.drawable.laptop,
			                      R.drawable.android)
			ua.forEach { entity ->
				uaDialog.add(entity.title ?: "",
				             iconList.getOrElse(entity.uaId ?: -1) { R.drawable.ua }) {
					webSettings.userAgentString = entity.ua
					preference.ua = entity.uaId ?: -1
					web.reload()
				}
			}
			uaDialog.selectMenu(preference.ua + 1)
			uaDialog.clickMenu(preference.ua + 1)
		}
		/*
         * 主题弹窗
         * */
		val themeDialog = GridMenuDialog(this).apply {
			setColumn(1)
			setSelectable(true)
			val themeTitle = mutableListOf(R.string.follow_system,
			                               R.string.dark_mode,
			                               R.string.light_mode)
			val themeIcon = mutableListOf<Int?>(R.drawable.setting,
			                                    R.drawable.dark,
			                                    R.drawable.light)
			val themeAction = mutableListOf(GridMenuDialog.onGridMenuClickListener { preference.theme = 0 },
			                                GridMenuDialog.onGridMenuClickListener { preference.theme = 1 },
			                                GridMenuDialog.onGridMenuClickListener { preference.theme = 2 })
			set(themeTitle, themeIcon, themeAction)
			setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START)
			setGravity(Gravity.CENTER_VERTICAL or Gravity.START)
			selectMenu(preference.theme)
		}        /*
         * Cookie 弹窗
         * */
		val cookieModeDialog = GridMenuDialog(this).apply {
			setColumn(1)
			setMultipleSelectable(true)
			setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START)
			setGravity(Gravity.CENTER_VERTICAL or Gravity.START)
			set(mutableListOf(R.string.cookie, R.string.third_party_cookie),
			    mutableListOf(R.drawable.cookie, R.drawable.cookie),
			    mutableListOf(GridMenuDialog.onGridMenuClickListener {
				    val accept = !preference.isCookieAccept
				    preference.isCookieAccept = accept
				    cookieManager.setAcceptCookie(accept)
			    }, GridMenuDialog.onGridMenuClickListener {
				    val accept = !preference.isThirdPartyCookieAccept
				    preference.isThirdPartyCookieAccept = accept
				    cookieManager.setAcceptThirdPartyCookies(web, accept)
			    }))
			toggleMenu(0, preference.isCookieAccept)
			toggleMenu(1, preference.isThirdPartyCookieAccept)
		}            /*
         * 网页弹窗
         * */
		val browserDialog = GridMenuDialog(this).apply {
			val webTitle = mutableListOf(R.string.ua,
			                             if (preference.isPC) R.string.pc_mode else R.string.mobile_mode,
			                             if (preference.isImageBlocked) R.string.image_blocked else R.string.image,
			                             R.string.javascript,
			                             R.string.save_mobile_data_mode,
			                             R.string.theme,
			                             R.string.privacy_mode,
			                             R.string.cookie)
			val webIcon = mutableListOf<Int?>(R.drawable.ua,
			                                  if (preference.isPC) R.drawable.laptop else R.drawable.phone,
			                                  if (preference.isImageBlocked) R.drawable.image_block else R.drawable.image,
			                                  R.drawable.js,
			                                  R.drawable.wifi,
			                                  R.drawable.light,
			                                  R.drawable.privacy,
			                                  R.drawable.cookie)
			val webAction = mutableListOf(GridMenuDialog.onGridMenuClickListener { uaDialog.show() },
			                              GridMenuDialog.onGridMenuClickListener { v: MaterialButton? ->
				                              val pc = !preference.isPC
				                              preference.isPC = pc
				                              v?.setText(if (pc) R.string.pc_mode else R.string.mobile_mode)
				                              v?.setIconResource(if (pc) R.drawable.laptop else R.drawable.phone)
				                              web.reload()
			                              },
			                              GridMenuDialog.onGridMenuClickListener { v: MaterialButton? ->
				                              val imageBlocked = !preference.isImageBlocked
				                              preference.isImageBlocked = imageBlocked
				                              v?.setText(if (imageBlocked) R.string.image_blocked else R.string.image)
				                              v?.setIconResource(if (imageBlocked) R.drawable.image_block else R.drawable.image)
				                              webSettings.blockNetworkImage = imageBlocked
			                              },
			                              GridMenuDialog.onGridMenuClickListener {
				                              val jsEnabled = !preference.isJSEnabled
				                              preference.isJSEnabled = jsEnabled
				                              webSettings.javaScriptEnabled = jsEnabled
			                              },
			                              GridMenuDialog.onGridMenuClickListener { v: MaterialButton? ->
				                              val saveMobileDataMode = !preference.isSaveMobileDataMode
				                              preference.isSaveMobileDataMode = saveMobileDataMode
				                              v?.setIconResource(if (saveMobileDataMode) R.drawable.no_wifi else R.drawable.wifi)
				                              webSettings.cacheMode = if (saveMobileDataMode) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_NO_CACHE
			                              },
			                              GridMenuDialog.onGridMenuClickListener {
				                              themeDialog.show()
				                              //                    String css = """
				                              //                            body { background-color: #121212 !important; color: #e0e0e0 !important; }\
				                              //                            a { color: #80cbc4 !important; }\
				                              //                            img { filter: brightness(0.8) contrast(1.2); }""";
				                              //                    web.evaluateJavascript("var style = document.createElement('style'); style.innerHTML = '" + css + "'; document.head.appendChild(style);", null);
				                              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) webSettings.setForceDark(
					                              WebSettings.FORCE_DARK_ON)
				                              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) webSettings.setAlgorithmicDarkeningAllowed(
					                              true)
			                              },
			                              GridMenuDialog.onGridMenuClickListener {
				                              val privacyMode = !preference.isPrivacyMode
				                              setPrivacyMode(privacyMode)
				                              preference.isPrivacyMode = privacyMode
			                              },
			                              GridMenuDialog.onGridMenuClickListener { cookieModeDialog.show() })
			set(webTitle, webIcon, webAction)
			setTogglable(intArrayOf(3, 4, 6), true)
			setColumn(4)
			toggleMenu(3, preference.isJSEnabled)
			toggleMenu(4, preference.isSaveMobileDataMode)
			toggleMenu(6, preference.isPrivacyMode)
		}
		/*
         * Cookie 弹窗
         * */
		val cookieDialog = EditTextDialog(this).apply {
			setTitle(R.string.cookie)
		}/*
         * 网站弹窗
         * */
		val websiteDialog = GridMenuDialog(this).apply {
			set<Int?>(mutableListOf(R.string.copy,
			                        R.string.share,
			                        R.string.open_in_browser,
			                        R.string.cookie,
			                        R.string.webpage_source),
			          mutableListOf(R.drawable.copy,
			                        R.drawable.share,
			                        R.drawable.export,
			                        R.drawable.cookie,
			                        R.drawable.version),
			          mutableListOf<GridMenuDialog.onGridMenuClickListener?>(GridMenuDialog.onGridMenuClickListener {
				          config.copy("url:", web.url)
			          }, GridMenuDialog.onGridMenuClickListener {
				          startActivity(Intent(Intent.ACTION_SEND).setType("text/plain")
					                        .putExtra(Intent.EXTRA_TEXT, trim(web.url)))
			          }, GridMenuDialog.onGridMenuClickListener {
				          startActivity(Intent(Intent.ACTION_VIEW).setData(trim(web.url).toUri()))
			          }, GridMenuDialog.onGridMenuClickListener {
				          val targetUrl = trim(web.url)
				          cookieDialog.value = cookieManager.getCookie(targetUrl)
				          cookieDialog.getDialog()
					          .setButton(DialogInterface.BUTTON_POSITIVE,
					                     getString(R.string.save)) { _: DialogInterface?, _: Int ->
						          cookieManager.setCookie(targetUrl, cookieDialog.getText())
					          }
				          cookieDialog.getDialog()
					          .setButton(DialogInterface.BUTTON_NEGATIVE,
					                     getString(R.string.clear)) { _: DialogInterface?, _: Int ->
									val cookies = cookieManager.getCookie(targetUrl) ?: return@setButton
									val cookieList = cookies.split(";")
									for (cookie in cookieList) {
									val cookieName = cookie.substringBefore("=").trim()
									val expiredCookie = "$cookieName=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/"
									cookieManager.setCookie(targetUrl, expiredCookie)
									}
									cookieManager.flush()
						          cookieDialog.value = ""
						          web.reload()
					          }
				          cookieDialog.getDialog()
					          .setButton(DialogInterface.BUTTON_NEUTRAL,
					                     getString(R.string.copy)) { _: DialogInterface?, _: Int ->
						          config.copy("Cookie:", cookieDialog.getText())
					          }
				          cookieDialog.show()
			          }, GridMenuDialog.onGridMenuClickListener {
				          web.loadUrl("view-source:${web.url}")
			          }))
			setColumn(4)
		}
		
		binding.js.setOnClickListener {
			model.loadJs()
			lifecycleScope.launch {
				val jsList = withContext(Dispatchers.IO) {
					repository.getAllJavaScript()
				}
				jsAdapter.set(ScriptManager.getMatchingScripts(web.url ?: "", jsList)
					              .toMutableList())
				jsDialog.show()
			}
		}
		binding.menu.setOnClickListener { menuDialog.show() }
		binding.browser.setOnClickListener { browserDialog.show() }
		binding.website.setOnClickListener { websiteDialog.show() }
		binding.close.setOnClickListener {
			binding.searchContainer.visibility = View.GONE
			web.clearMatches()
		}
		binding.keyword.addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(s: Editable?) {
			}
			
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
			}
			
			override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
				web.findAllAsync("$s")
			}
		})
		binding.next.setOnClickListener { web.findNext(true) }
		binding.last.setOnClickListener { web.findNext(false) }
		binding.number.setOnClickListener {
			web.findAllAsync(binding.keyword.text.toString())
		}
		web.setFindListener { activeMatchOrdinal: Int, numberOfMatches: Int, isDoneCounting: Boolean ->
			if (isDoneCounting) binding.number.text = "${if (activeMatchOrdinal == 0) 0 else activeMatchOrdinal + 1}/$numberOfMatches"
		}
		
		progress.observe(this) { p: Int? ->
			refreshButton?.setIconResource(if (p == 100) R.drawable.refresh else R.drawable.close)
			refreshButton?.setText(if (p == 100) R.string.refresh else R.string.stop)
		}
		binding.back.setOnClickListener {
			goBack()
		}
		binding.forward.setOnClickListener {
			goForward()
		}
		if (intent.hasExtra("data") && intent.getStringExtra("data") != null) {
			webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 14; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36")
			web.loadDataWithBaseURL("https://jwxt.sysu.edu.cn",
			                        intent.getStringExtra("data") ?: "",
			                        "text/html",
			                        "utf-8",
			                        "https://jwxt.sysu.edu.cn")
		}
		else web.loadUrl(url)
		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				if (progress.value != 100) web.stopLoading()
				else if (web.canGoBack()) web.goBack()
				else supportFinishAfterTransition()
			}
		})
	}
	
	fun setPrivacyMode(enabled: Boolean) {
		webSettings.domStorageEnabled = !enabled
		webSettings.allowFileAccess = !enabled
		webSettings.allowContentAccess = !enabled
	}
	
	override fun onDestroy() {
		gmBridge?.release()
		disposable.dispose()
		web.stopLoading()
		(web.parent as ViewGroup).removeView(web)
		web.destroy()
		super.onDestroy()
	}
	
	fun goBack() {
		if (web.canGoBack()) web.goBack()
	}
	
	fun goForward() {
		if (web.canGoForward()) web.goForward()
	}
	
	fun refresh() {
		if (progress.value == 100) web.reload()
		else web.stopLoading()
	}
	
	fun pageUp() {
		web.pageUp(true)
	}
	
	fun pageDown() {
		web.pageDown(true)
	}
	
	private fun showLinkMenu(url: String, popup: PopupMenu) {
		popup.menu.add(R.string.open_in_browser).setOnMenuItemClickListener {
			web.loadUrl(url)
			true
		}
		popup.menu.add(R.string.copy).setOnMenuItemClickListener {
			config.copy("link", url)
			true
		}
		popup.menu.add(R.string.share).setOnMenuItemClickListener {
			shareText(url)
			true
		}
		popup.show()
	}
	
	private fun showImageMenu(imageUrl: String, popup: PopupMenu) {
		popup.menu.add(R.string.download)
			.setOnMenuItemClickListener {
//            System.out.println(imageUrl);
//            System.out.println(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + getString(R.string.app_name) + "/" + getFileName(imageUrl));
				downloadFile(this,
				             imageUrl,
				             Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
					             .toString() + "/" + getString(R.string.app_name) + "/" + getFileName(
					             imageUrl),
				             true,
				             object : DownloadManager.DownloadListener {
					             override fun onDownloadProgress(progress: Long, total: Long) {
//						             println("$progress $total")
					             }
					             
					             override fun onDownloadComplete(path: String?) {
						             Snackbar.make(web,
						                           "下载完成，保存到：$path",
						                           Snackbar.LENGTH_LONG).setAction(R.string.open) {
							             path?.let {
								             openFile(this@BrowserActivity, it)
							             }
						             }.show()
					             }
					             
					             override fun onDownloadError(code: Int, message: String?) {
//						             println("$code $message")
					             }
				             })
				true
			}
		popup.menu.add(R.string.open_in_browser).setOnMenuItemClickListener {
			web.loadUrl(imageUrl)
			true
		}
		popup.menu.add(R.string.copy).setOnMenuItemClickListener {
			config.copy("image", imageUrl)
			true
		}
		popup.menu.add(R.string.share).setOnMenuItemClickListener {
			shareText(imageUrl)
			true
		}
		popup.show()
	}
	
	private fun shareText(text: String?) {
		startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
			                                   .putExtra(Intent.EXTRA_TEXT, text),
		                                   getString(R.string.share)))
	}
	
	fun getFileName(url: String): String {
		return url.toUri().let{
			it.getQueryParameter("fileName")
				?: run{
					it.path?.run{
						if (isNotEmpty()){
							val i = lastIndexOf("/")
						if (i >= 0) substring(i + 1)
					}
					} ?: "unknown"
				} as String
		}
//		var path = ""
//		try {
//			path = URLDecoder.decode(URI.create(url).path, "utf-8")
//		} catch (e: UnsupportedEncodingException) {
//			e.printStackTrace()
//		}
//		val i = path.lastIndexOf("/")
//		return if (i >= 0) path.substring(i + 1) else path
	}
	
	internal class JSAdapter : RecyclerAdapter<JavaScriptEntity>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object : RecyclerView.ViewHolder(LayoutInflater.from(parent.context)
				                                        .inflate(R.layout.item_preference,
				                                                 parent,
				                                                 false)) {}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val item = get(position)
			ItemPreferenceBinding.bind(holder.itemView).apply {
				itemTitle.text = item.title
				itemContent.text = item.description
				itemIcon.setImageResource(R.drawable.js)
				root.updateAppearance(position, itemCount)
			}
			super.onBindViewHolder(holder, position)
		}
	}
}