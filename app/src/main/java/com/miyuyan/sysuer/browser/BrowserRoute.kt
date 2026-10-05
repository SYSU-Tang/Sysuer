package com.miyuyan.sysuer.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Message
import android.text.TextUtils
import android.view.GestureDetector
import android.view.MotionEvent
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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.miyuyan.preference.Preference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.SwitchPreference
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.trim
import com.miyuyan.sysuer.api.ContextUtil
import com.miyuyan.sysuer.api.DownloadManager.downloadFile
import com.miyuyan.sysuer.api.DownloadManager.openFile
import com.miyuyan.sysuer.browser.data.BrowserRepository
import com.miyuyan.sysuer.browser.data.GMBridge
import com.miyuyan.sysuer.browser.data.JavaScriptEntity
import com.miyuyan.sysuer.browser.data.JsModel
import com.miyuyan.sysuer.browser.data.ScriptManager
import com.miyuyan.sysuer.browser.data.UserAgentEntity
import com.miyuyan.sysuer.nav.Browser
import com.miyuyan.sysuer.nav.JsDetail
import com.miyuyan.sysuer.nav.JsList
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.preference.BrowserPreference
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.GridDialog
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.SysuerWebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** 网页长按命中的链接或图片 */
data class WebViewHitTest(val x: Float, val y: Float, val url: String, val isImage: Boolean)

/**
 * 浏览器状态持有者：负责创建与配置 [SysuerWebView]、注入脚本桥、拦截教务文件下载、
 * 长按命中检测，并把网页事件（进度、标题、图标、前进后退、查找、JS 对话框、下载请求）
 * 以 Compose 状态形式暴露给 [BrowserRoute]。
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
class BrowserState(
	private val context: android.content.Context,
	initialUrl: String,
	htmlData: String?,
	private val jsProvider: () -> List<JavaScriptEntity>,
) {
	val preference = BrowserPreference(context)
	val webView: SysuerWebView = SysuerWebView(context)

	// 提供当前网页地址，供 GM 桥接判断凭据请求是否来自 SYSU 域名下的页面
	// urlProvider 会被 JavascriptInterface 线程调用，读主线程维护的 url 状态而非
	// 直接调 webView.getUrl()，否则抛 "WebView method was called on thread 'JavaBridge'"
	val gmBridge = GMBridge(context) { url }
	private val cookieManager = CookieManager.getInstance()
	private val httpCookieManager by lazy { com.miyuyan.sysuer.api.CookieManager(context) }
	private val okHttpClient = OkHttpClient()
	val webSettings: WebSettings = webView.settings

	var progress by mutableIntStateOf(100); private set
	var title by mutableStateOf(""); private set
	var url by mutableStateOf(initialUrl); private set
	var icon by mutableStateOf<ImageBitmap?>(null); private set
	var canGoBack by mutableStateOf(false); private set
	var canGoForward by mutableStateOf(false); private set
	var findStatus by mutableStateOf("0/0"); private set
	var jsDialog by mutableStateOf<Pair<String, JsResult>?>(null)
	var downloadRequest by mutableStateOf<Pair<String, String>?>(null)
	var hitTest by mutableStateOf<WebViewHitTest?>(null)

	var isPC by mutableStateOf(preference.isPC); private set
	var isImageBlocked by mutableStateOf(preference.isImageBlocked); private set
	var isJSEnabled by mutableStateOf(preference.isJSEnabled); private set
	var isSaveMobileDataMode by mutableStateOf(preference.isSaveMobileDataMode); private set
	var isPrivacyMode by mutableStateOf(preference.isPrivacyMode); private set
	var isCookieAccept by mutableStateOf(preference.isCookieAccept); private set
	var isThirdPartyCookieAccept by mutableStateOf(preference.isThirdPartyCookieAccept); private set
	var theme by mutableIntStateOf(preference.theme); private set
	var uaSelection by mutableIntStateOf(preference.ua); private set

	init {
		webView.addJavascriptInterface(gmBridge, "AndroidGM")
		// 网页输入框需要 WebView 持有焦点才能唤出输入法
		webView.isFocusable = true
		webView.isFocusableInTouchMode = true
		webView.requestFocus()
		// 不向父级分发嵌套滚动：否则 WebView 会驱动顶栏折叠行为，上滑时顶栏抖动
		webView.isNestedScrollingEnabled = false
		webView.webViewClient = object : WebViewClient() {
			override fun shouldOverrideUrlLoading(
				view: WebView,
				request: WebResourceRequest,
			): Boolean {
				val target = request.url.toString()
				println("shouldOverrideUrlLoading: $target")
				if (target.startsWith("https://") || target.startsWith("http://")) {
					view.loadUrl(target)
				} else {
					Intent(Intent.ACTION_VIEW).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
						.setData(target.toUri())
						.takeIf { it.resolveActivity(context.packageManager) != null }
						?.let { context.startActivity(it) }
				}
				return true
			}

			override fun shouldInterceptRequest(
				view: WebView?,
				request: WebResourceRequest,
			): WebResourceResponse? {
				val target = request.url.toString()
				if ("https?://jwxt.sysu.edu.cn/jwxt/system-manage/infoRelease/downloadFile"
						.toRegex().matches(target)
				) {
					try {
						val response = okHttpClient.newCall(
								Request.Builder().url(target)
									.header("Cookie", cookieManager.getCookie(target) ?: "")
									.header("Referer", "https://jwxt.sysu.edu.cn/jwxt/").build()
						).execute()
						val mediaType = response.body.contentType()
						return WebResourceResponse(
								mediaType?.type ?: "application/octet-stream",
								"utf-8",
								response.body.byteStream()
						)
					} catch (_: IOException) {
					}
				}
				return super.shouldInterceptRequest(view, request)
			}

			override fun onPageStarted(view: WebView?, target: String?, favicon: Bitmap?) {
				gmBridge.clearCommands()
				jsProvider().let {
					ScriptManager.executeScripts(
							webView,
							ScriptManager.getMatchingScripts(target ?: "", it),
							"document-start"
					)
				}
				super.onPageStarted(view, target, favicon)
			}

			override fun onPageFinished(view: WebView, target: String) {
				if (isPC) view.evaluateJavascript(
						"document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));",
						null
				)
				updateNavigationState()
				super.onPageFinished(view, target)
				jsProvider().let {
					ScriptManager.executeScripts(
							webView,
							ScriptManager.getMatchingScripts(target, it),
							listOf("document-idle", "document-end")
					)
				}
			}

			override fun doUpdateVisitedHistory(
				view: WebView?, target: String?, isReload: Boolean
			) {
				updateNavigationState()
				super.doUpdateVisitedHistory(view, target, isReload)
			}
		}
		webView.webChromeClient = object : WebChromeClient() {
			override fun onJsAlert(
				view: WebView?, target: String?, message: String?, result: JsResult,
			): Boolean {
				jsDialog = (message ?: "") to result
				return true
			}

			override fun onJsConfirm(
				view: WebView?, target: String?, message: String?, result: JsResult,
			): Boolean {
				jsDialog = (message ?: "") to result
				return true
			}

			override fun onCreateWindow(
				view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message,
			): Boolean {
				val newWebView = WebView(context)
				newWebView.setWebViewClient(object : WebViewClient() {
					override fun shouldOverrideUrlLoading(
						view: WebView?, request: WebResourceRequest,
					): Boolean {
						webView.loadUrl(request.url.toString())
						newWebView.destroy()
						return super.shouldOverrideUrlLoading(view, request)
					}
				})
				val transport = resultMsg.obj as WebViewTransport
				transport.webView = newWebView
				resultMsg.sendToTarget()
				return true
			}

			override fun onReceivedTitle(view: WebView, pageTitle: String?) {
				title = pageTitle ?: ""
				url = view.url ?: ""
				super.onReceivedTitle(view, pageTitle)
			}

			override fun onReceivedIcon(view: WebView?, bitmap: Bitmap?) {
				icon = bitmap?.asImageBitmap()
				super.onReceivedIcon(view, bitmap)
			}

			override fun onProgressChanged(view: WebView?, newProgress: Int) {
				progress = newProgress
				updateNavigationState()
				super.onProgressChanged(view, newProgress)
			}
		}
		webView.setDownloadListener { link, _, _, _, _ ->
			val path =
				"${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${
					getFileName(link)
				}"
			downloadRequest = link to path
		}
		webSettings.apply {
			supportZoom()
			javaScriptEnabled = preference.isJSEnabled
			setSupportMultipleWindows(true)
			useWideViewPort = true
			loadWithOverviewMode = true
			setSupportZoom(true)
			builtInZoomControls = true
			displayZoomControls = false
			cacheMode = if (preference.isSaveMobileDataMode) WebSettings.LOAD_NO_CACHE
			else WebSettings.LOAD_DEFAULT
			javaScriptCanOpenWindowsAutomatically = true
			loadsImagesAutomatically = true
			blockNetworkImage = preference.isImageBlocked
			defaultTextEncodingName = "utf-8"
			// 初始化时应用用户配置的 UA（同步读自 DataStore 快照），未配置则保持系统默认
			preference.uaString?.let { userAgentString = it }
		}
		updatePrivacyMode(preference.isPrivacyMode)
		cookieManager.setAcceptCookie(preference.isCookieAccept)
		cookieManager.setAcceptThirdPartyCookies(webView, preference.isThirdPartyCookieAccept)
		val gesture = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
			override fun onLongPress(event: MotionEvent) {
				val result = webView.hitTestResult
				val extra = result.extra
				when (result.type) {
					WebView.HitTestResult.SRC_ANCHOR_TYPE -> if (!TextUtils.isEmpty(extra)) {
						hitTest = WebViewHitTest(event.x, event.y, extra!!, false)
					}

					WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> if (!TextUtils.isEmpty(extra)) {
						hitTest = WebViewHitTest(event.x, event.y, extra!!, true)
					}
				}
			}
		})
		webView.setOnTouchListener { view, event ->
			if (event.action == MotionEvent.ACTION_DOWN) view.requestFocus()
			gesture.onTouchEvent(event)
			false
		}
		webView.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
			if (isDoneCounting) {
				findStatus =
					"${if (numberOfMatches == 0) 0 else activeMatchOrdinal + 1}/$numberOfMatches"
			}
		}
		if (htmlData != null) {
			webSettings.userAgentString = MOBILE_UA
			webView.loadDataWithBaseURL(
					"https://jwxt.sysu.edu.cn",
					htmlData,
					"text/html",
					"utf-8",
					"https://jwxt.sysu.edu.cn"
			)
		} else webView.loadUrl(initialUrl)
	}

	private fun updateNavigationState() {
		url = webView.url ?: url
		canGoBack = webView.canGoBack()
		canGoForward = webView.canGoForward()
	}

	/** 页面导航 */
	fun goBack() {
		if (webView.canGoBack()) webView.goBack()
	}

	fun goForward() {
		if (webView.canGoForward()) webView.goForward()
	}

	/** 加载中为停止，加载完成为刷新 */
	fun refresh() {
		if (progress == 100) webView.reload() else webView.stopLoading()
	}

	fun exit() = webView.stopLoading()
	fun pageUp() = webView.pageUp(true)
	fun pageDown() = webView.pageDown(true)
	fun zoomIn() = webView.zoomIn()
	fun zoomOut() = webView.zoomOut()
	fun loadUrl(target: String) = webView.loadUrl(target)
	fun viewSource() = webView.loadUrl("view-source:${webView.url}")
	fun findAll(keyword: String) = webView.findAllAsync(keyword)
	fun findNext(forward: Boolean) = webView.findNext(forward)
	fun clearMatches() = webView.clearMatches()

	/** 浏览设置 */
	fun updatePC(enabled: Boolean) {
		preference.isPC = enabled
		isPC = enabled
		webView.reload()
	}

	fun updateImageBlocked(enabled: Boolean) {
		preference.isImageBlocked = enabled
		isImageBlocked = enabled
		webSettings.blockNetworkImage = enabled
	}

	fun updateJSEnabled(enabled: Boolean) {
		preference.isJSEnabled = enabled
		isJSEnabled = enabled
		webSettings.javaScriptEnabled = enabled
	}

	fun updateSaveMobileDataMode(enabled: Boolean) {
		preference.isSaveMobileDataMode = enabled
		isSaveMobileDataMode = enabled
		webSettings.cacheMode = if (enabled) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
	}

	fun updatePrivacyMode(enabled: Boolean) {
		preference.isPrivacyMode = enabled
		isPrivacyMode = enabled
		webSettings.domStorageEnabled = !enabled
		webSettings.allowFileAccess = !enabled
		webSettings.allowContentAccess = !enabled
	}

	fun updateCookieAccept(enabled: Boolean) {
		preference.isCookieAccept = enabled
		isCookieAccept = enabled
		cookieManager.setAcceptCookie(enabled)
	}

	fun updateThirdPartyCookieAccept(enabled: Boolean) {
		preference.isThirdPartyCookieAccept = enabled
		isThirdPartyCookieAccept = enabled
		cookieManager.setAcceptThirdPartyCookies(webView, enabled)
	}

	/** 主题：0 跟随系统、1 深色、2 浅色；深色对网页强制变暗 */
	fun updateTheme(value: Int) {
		preference.theme = value
		theme = value
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			webSettings.setForceDark(
					if (value == 1) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
			)
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			webSettings.setAlgorithmicDarkeningAllowed(true)
		}
	}

	fun setFollowSystemUA() {
		webSettings.userAgentString = WebSettings.getDefaultUserAgent(context)
		preference.ua = -1
		preference.uaString = null
		uaSelection = -1
		webView.reload()
	}

	fun setUserAgent(entity: UserAgentEntity) {
		webSettings.userAgentString = entity.ua
		preference.ua = entity.uaId ?: -1
		preference.uaString = entity.ua
		uaSelection = entity.uaId ?: -1
		webView.reload()
	}

	/** Cookie 读取与编辑（网站菜单） */
	fun getCookie(target: String): String? = cookieManager.getCookie(target)

	fun setCookie(target: String, value: String) {
		cookieManager.setCookie(target, value)
		cookieManager.flush()
	}

	fun clearCookie(target: String) {
		val cookies = cookieManager.getCookie(target) ?: return
		cookies.split(";").forEach { cookie ->
			val name = cookie.substringBefore("=").trim()
			cookieManager.setCookie(
					target, "$name=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/"
			)
		}
		cookieManager.flush()
		webView.reload()
	}

	/** 确认下载弹窗：教务域名带 Cookie 与 Referer 下载 */
	fun confirmDownload() {
		downloadRequest?.let { (link, path) ->
			if (link.toHttpUrlOrNull()?.host == "jwxt.sysu.edu.cn") {
				downloadFile(
						context,
						Request.Builder().url(link)
							.header("Cookie", httpCookieManager.toSimpleString("jwxt.sysu.edu.cn"))
							.header("Referer", "https://jwxt.sysu.edu.cn/").build(),
						path
				)
			} else downloadFile(context, link, path)
		}
		downloadRequest = null
	}

	/** 长按图片：下载到图片目录 */
	fun downloadImage(imageUrl: String, onComplete: (String?) -> Unit) {
		downloadFile(
				context,
				imageUrl,
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
					.toString() + "/" + context.getString(R.string.app_name) + "/" + getFileName(
						imageUrl
				),
				true,
				object : com.miyuyan.sysuer.api.DownloadManager.DownloadListener {
					override fun onDownloadProgress(progress: Long, total: Long) {}

					override fun onDownloadComplete(path: String?) = onComplete(path)

					override fun onDownloadError(code: Int, message: String?) = onComplete(null)
				})
	}

	fun destroy() {
		gmBridge.release()
		webView.stopLoading()
		(webView.parent as? ViewGroup)?.removeView(webView)
		webView.destroy()
	}

	private fun getFileName(target: String): String = target.toUri().let {
		it.getQueryParameter("fileName") ?: run {
			it.path?.run {
				if (isNotEmpty()) {
					val index = lastIndexOf("/")
					if (index >= 0) substring(index + 1) else this
				} else this
			} ?: "unknown"
		}
	}

	companion object {
		private const val MOBILE_UA =
			"Mozilla/5.0 (Linux; Android 14; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36"
	}
}

/**
 * 内置浏览器：顶栏显示站点图标、标题与地址，加载进度条位于其下；底部为后退、脚本、
 * 浏览设置、菜单、网站、前进操作栏。各操作展开为 BottomSheet 与对话框。
 */
@OptIn(
		ExperimentalMaterial3Api::class
)
@Composable
fun BrowserRoute(
	backStack: MutableList<NavKey>,
	navKey: Browser = backStack.last() as Browser,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val context = LocalContext.current
	val activity = LocalActivity.current
	val contextUtil = remember { ContextUtil.getInstance(context) }
	val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
	val repository = remember { BrowserRepository(context, lifecycleScope) }
	val jsModel: JsModel = viewModel { JsModel(repository) }
	// 脚本列表由 loadJs 回调写入；脚本面板展示与页面脚本注入都读取它
	var jsList by remember { mutableStateOf<List<JavaScriptEntity>>(emptyList()) }
	val state = remember {
		BrowserState(context, navKey.url, navKey.content) { jsList }
	}
	var userAgents by remember { mutableStateOf<List<UserAgentEntity>>(emptyList()) }

	var showFindBar by remember { mutableStateOf(false) }
	var findKeyword by remember { mutableStateOf("") }
	var showMenuSheet by remember { mutableStateOf(false) }
	var showSettingsSheet by remember { mutableStateOf(false) }
	var showWebsiteSheet by remember { mutableStateOf(false) }
	var showJsSheet by remember { mutableStateOf(false) }
	var showUaSheet by remember { mutableStateOf(false) }
	var showThemeSheet by remember { mutableStateOf(false) }
	var showCookieSheet by remember { mutableStateOf(false) }
	var showCookieEditor by remember { mutableStateOf(false) }
	var cookieDraft by remember { mutableStateOf("") }
	var menuScript by remember { mutableStateOf<JavaScriptEntity?>(null) }

	LaunchedEffect(Unit) {
		userAgents = withContext(Dispatchers.IO) { repository.getAllUserAgents() }
		jsModel.loadJs { jsList = it }
	}
	BackHandler(state.progress != 100 || state.canGoBack) {
		if (state.progress != 100) state.exit()
		else if (state.canGoBack) state.goBack()
	}
	DisposableEffect(state) {
		onDispose { state.destroy() }
	}

	val shareString = stringResource(R.string.share)
	ActivityPager(
			title = state.title.ifEmpty { stringResource(R.string.app_name) },
			subtitle = state.url,
			sharedKey = navKey.url,
			logo = state.icon?.let {
				{
					Image(
							bitmap = it,
							contentDescription = null,
							modifier = Modifier.widthIn(min = 20.dp, max = 28.dp)
					)
				}
			} ?: {
				Icon(
						painter = painterResource(R.drawable.web),
						contentDescription = null,
						modifier = Modifier.widthIn(min = 20.dp, max = 28.dp)
				)
			},
			onNavigationClick = {
				if (backStack.size > 1) backStack.navigateBack(activity)
				else activity?.finishAfterTransition()
			},
			isNestedScrollEnabled = false,
			isTopBarContentFixed = true,
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			topBarContent = {
				Column {
					if (state.progress < 100) {
						LinearProgressIndicator(
								progress = { state.progress / 100f },
								modifier = Modifier.fillMaxWidth()
						)
					}
					if (showFindBar) {
						FindBar(
								keyword = findKeyword,
								status = state.findStatus,
								onKeywordChange = {
									findKeyword = it
									state.findAll(it)
								},
								onClose = {
									showFindBar = false
									state.clearMatches()
								},
								onStatusClick = { state.findAll(findKeyword) },
								onFindNext = {
									state.findNext(it)

								},
						)
					}
				}
			},
	) {
		Column(modifier = Modifier.fillMaxSize()) {
			// 网页
			Box(
					modifier = Modifier
						.weight(1f)
						.fillMaxWidth()
			) {
				AndroidView(factory = { state.webView }, modifier = Modifier.fillMaxSize())
				val density = LocalDensity.current
				state.hitTest?.let { hit ->
					DropdownMenu(
							expanded = true,
							onDismissRequest = { state.hitTest = null },
							offset = with(density) { DpOffset(hit.x.toDp(), hit.y.toDp()) },
							modifier = Modifier.background(MaterialTheme.colorScheme.surface),
					) {
						if (hit.isImage) {
							DropdownMenuItem(
									text = { Text(stringResource(R.string.download)) },
									onClick = {
										state.hitTest = null
										state.downloadImage(hit.url) { path ->
											if (path != null) {
												contextUtil.toast("下载完成，保存到：$path")
												openFile(context, path)
											}
										}
									},
							)
						}
						DropdownMenuItem(
								text = { Text(stringResource(R.string.open_in_browser)) },
								onClick = {
									state.hitTest = null
									state.loadUrl(hit.url)
								},
						)
						DropdownMenuItem(
								text = { Text(stringResource(R.string.copy)) },
								onClick = {
									state.hitTest = null
									contextUtil.copy(if (hit.isImage) "image" else "link", hit.url)
								},
						)
						DropdownMenuItem(
								text = { Text(stringResource(R.string.share)) },
								onClick = {
									state.hitTest = null
									context.startActivity(
											Intent.createChooser(
													Intent(Intent.ACTION_SEND).setType("text/plain")
														.putExtra(Intent.EXTRA_TEXT, hit.url),
													shareString
											)
									)
								},
						)
					}
				}
			}
			// 底部操作栏
			Surface {
				Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(
									vertical = dimensionResource(R.dimen.vertical_padding)
							),
						horizontalArrangement = Arrangement.SpaceEvenly,
						verticalAlignment = Alignment.CenterVertically,
				) {
					IconButton(onClick = { state.goBack() }, enabled = state.canGoBack) {
						Icon(
								Icons.AutoMirrored.Rounded.ArrowBack,
								contentDescription = stringResource(R.string.back)
						)
					}
					IconButton(onClick = {
						jsModel.loadJs { jsList = it }
						showJsSheet = true
					}) {
						Icon(
								painter = painterResource(R.drawable.js),
								contentDescription = stringResource(R.string.js)
						)
					}
					IconButton(onClick = { showSettingsSheet = true }) {
						Icon(
								painter = painterResource(R.drawable.web),
								contentDescription = stringResource(R.string.browser)
						)
					}
					IconButton(onClick = { showMenuSheet = true }) {
						Icon(
								painter = painterResource(R.drawable.menu),
								contentDescription = stringResource(R.string.menu)
						)
					}
					IconButton(onClick = { showWebsiteSheet = true }) {
						Icon(
								painter = painterResource(R.drawable.link),
								contentDescription = stringResource(R.string.website)
						)
					}
					IconButton(onClick = { state.goForward() }, enabled = state.canGoForward) {
						Icon(
								Icons.AutoMirrored.Rounded.ArrowForward,
								contentDescription = stringResource(R.string.forward)
						)
					}
				}
			}
		}
	}

	// JS alert / confirm 对话框
	state.jsDialog?.let { (message, result) ->
		AlertDialog(
				onDismissRequest = {
					result.confirm()
					state.jsDialog = null
				},
				text = { Text(message) },
				confirmButton = {
					TextButton(
							onClick = {
								result.confirm()
								state.jsDialog = null
							}, shapes = ButtonDefaults.shapes()
					) { Text(stringResource(R.string.confirm)) }
				},
		)
	}
	// 下载确认对话框
	state.downloadRequest?.let { (link, path) ->
		AlertDialog(
				onDismissRequest = { state.downloadRequest = null },
				title = { Text(stringResource(R.string.download)) },
				text = {
					Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
						Text(
								link,
								style = MaterialTheme.typography.bodySmall,
								modifier = Modifier.combinedClickable(onClick = {}, onLongClick = {
									contextUtil.copy("link", link)
									contextUtil.toast(R.string.copy_successfully)
								})
						)
						Text(
								path,
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
				},
				confirmButton = {
					TextButton(
							onClick = { state.confirmDownload() }, shapes = ButtonDefaults.shapes()
					) { Text(stringResource(R.string.download)) }
				},
				dismissButton = {
					TextButton(
							onClick = { state.downloadRequest = null },
							shapes = ButtonDefaults.shapes()
					) { Text(stringResource(R.string.cancel)) }
				},
		)
	}
	// 页面操作菜单
	if (showMenuSheet) {
		val menuActions = listOf(
				MenuItem(stringResource(R.string.back), R.drawable.left) { state.goBack() },
				MenuItem(
						stringResource(R.string.forward), R.drawable.right
				) { state.goForward() },
				MenuItem(
						stringResource(if (state.progress == 100) R.string.refresh else R.string.stop),
						if (state.progress == 100) R.drawable.refresh else R.drawable.close
				) { state.refresh() },
				MenuItem(stringResource(R.string.exit), R.drawable.exit) {
					showMenuSheet = false
					activity?.finishAfterTransition()
				},
				MenuItem(stringResource(R.string.page_up), R.drawable.up) { state.pageUp() },
				MenuItem(
						stringResource(R.string.page_down), R.drawable.down
				) { state.pageDown() },
				MenuItem(
						stringResource(R.string.zoom_in), R.drawable.zoom_in
				) { state.zoomIn() },
				MenuItem(
						stringResource(R.string.zoom_out), R.drawable.zoom_out
				) { state.zoomOut() },
				MenuItem(stringResource(R.string.find_text), R.drawable.search) {
					showMenuSheet = false
					showFindBar = true
				},
		)
		GridDialog(
				onDismissRequest = { showMenuSheet = false },
				items = menuActions,
		)
	}
	// 浏览设置
	if (showSettingsSheet) {
		ModalBottomSheet(onDismissRequest = { showSettingsSheet = false }) {
			Column(
					modifier = Modifier
						.fillMaxWidth()
						.verticalScroll(rememberScrollState())
			) {
				PreferenceCategory {
					item {
						Preference(
								onClick = { showUaSheet = true },
								title = stringResource(R.string.ua),
								icon = {
									Icon(
											painter = painterResource(R.drawable.ua),
											contentDescription = null,
											tint = MaterialTheme.colorScheme.primary
									)
								},
								summary = userAgents.firstOrNull { it.uaId == state.uaSelection }?.title
									?: stringResource(R.string.follow_system),
						)
					}
					item {
						SwitchPreference(
								title = stringResource(if (state.isPC) R.string.pc_mode else R.string.mobile_mode),
								checked = state.isPC,
								onCheckedChange = { state.updatePC(it) },
								icon = {
									Icon(
											painter = painterResource(if (state.isPC) R.drawable.laptop else R.drawable.phone),
											contentDescription = null,
											tint = MaterialTheme.colorScheme.primary
									)
								},
						)
					}
					item {
						SwitchPreference(
								title = stringResource(if (state.isImageBlocked) R.string.image_blocked else R.string.image),
								checked = state.isImageBlocked,
								onCheckedChange = { state.updateImageBlocked(it) },
								icon = {
									Icon(
											painter = painterResource(if (state.isImageBlocked) R.drawable.image_block else R.drawable.image),
											contentDescription = null,
											tint = MaterialTheme.colorScheme.primary
									)
								},
						)
					}
					item {
						SwitchPreference(
								title = stringResource(R.string.javascript),
								checked = state.isJSEnabled,
								onCheckedChange = { state.updateJSEnabled(it) },
								icon = {
									Icon(
											painter = painterResource(R.drawable.js),
											contentDescription = null,
											tint = MaterialTheme.colorScheme.primary
									)
								},
						)
					}
					item {
						SwitchPreference(
								title = stringResource(R.string.save_mobile_data_mode),
								checked = state.isSaveMobileDataMode,
								onCheckedChange = { state.updateSaveMobileDataMode(it) },
								icon = {
									Icon(
											painter = painterResource(if (state.isSaveMobileDataMode) R.drawable.no_wifi else R.drawable.wifi),
											contentDescription = null,
											tint = MaterialTheme.colorScheme.primary
									)
								},
						)
					}
					item {
						Preference(
								onClick = { showThemeSheet = true },
								title = stringResource(R.string.theme),
								icon = {
									Icon(
											painter = painterResource(R.drawable.light),
											contentDescription = null,
											tint = MaterialTheme.colorScheme.primary
									)
								},
								summary = stringResource(
										when (state.theme) {
											1 -> R.string.dark_mode
											2 -> R.string.light_mode
											else -> R.string.follow_system
										}
								),
						)
					}
					item {
						SwitchPreference(
								title = stringResource(R.string.privacy_mode),
								checked = state.isPrivacyMode,
								onCheckedChange = { state.updatePrivacyMode(it) },
								icon = {
									Icon(
											painter = painterResource(R.drawable.privacy),
											contentDescription = null,
											tint = MaterialTheme.colorScheme.primary
									)
								},
						)
					}
					item {
						Preference(
								onClick = { showCookieSheet = true },
								title = stringResource(R.string.cookie),
								icon = {
									Icon(
											painter = painterResource(R.drawable.cookie),
											contentDescription = null,
											tint = MaterialTheme.colorScheme.primary
									)
								},
						)
					}
				}
			}
		}
	}
	// UA 选择
	if (showUaSheet) {
		val iconList = listOf(
				R.drawable.laptop,
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
				R.drawable.android
		)
		ModalBottomSheet(onDismissRequest = { showUaSheet = false }) {
			Column(
					modifier = Modifier
						.fillMaxWidth()
						.verticalScroll(rememberScrollState())
			) {
				PreferenceCategory {
					item {
						Preference(
								onClick = {
									state.setFollowSystemUA()
									showUaSheet = false
								},
								title = stringResource(R.string.follow_system),
								icon = {
									Icon(
											painter = painterResource(R.drawable.setting),
											contentDescription = null,
											tint = MaterialTheme.colorScheme.primary
									)
								},
								trailing = {
									if (state.uaSelection == -1) {
										Icon(Icons.Rounded.Check, contentDescription = null)
									}
								},
						)
					}
					userAgents.forEach { entity ->
						item {
							Preference(
									onClick = {
										state.setUserAgent(entity)
										showUaSheet = false
									},
									title = entity.title ?: "",
									icon = {
										Icon(
												painter = painterResource(
														iconList.getOrElse(
																entity.uaId ?: -1
														) { R.drawable.ua }),
												contentDescription = null,
												tint = MaterialTheme.colorScheme.primary
										)
									},
									trailing = {
										if (state.uaSelection == entity.uaId) {
											Icon(Icons.Rounded.Check, contentDescription = null)
										}
									},
							)
						}
					}
				}
			}
		}
	}
	// 主题选择
	if (showThemeSheet) {
		val themes = listOf(
				Triple(R.string.follow_system, R.drawable.setting, 0),
				Triple(R.string.dark_mode, R.drawable.dark, 1),
				Triple(R.string.light_mode, R.drawable.light, 2),
		)
		ModalBottomSheet(onDismissRequest = { showThemeSheet = false }) {
			Column(
					modifier = Modifier
						.fillMaxWidth()
						.verticalScroll(rememberScrollState())
			) {
				PreferenceCategory {
					themes.forEachIndexed { _, (titleRes, iconRes, value) ->
						item {
							Preference(
									onClick = {
										state.updateTheme(value)
										showThemeSheet = false
									},
									title = stringResource(titleRes),
									icon = {
										Icon(
												painter = painterResource(iconRes),
												contentDescription = null,
												tint = MaterialTheme.colorScheme.primary
										)
									},
									trailing = {
										if (state.theme == value) {
											Icon(Icons.Rounded.Check, contentDescription = null)
										}
									},
							)
						}
					}
				}
			}
		}
	}
	// Cookie 开关
	if (showCookieSheet) {
		ModalBottomSheet(onDismissRequest = { showCookieSheet = false }) {
			Column(
					modifier = Modifier
						.fillMaxWidth()
						.verticalScroll(rememberScrollState())
			) {
				PreferenceCategory {
					item {
						SwitchPreference(
								title = stringResource(R.string.cookie),
								checked = state.isCookieAccept,
								onCheckedChange = { state.updateCookieAccept(it) },
								icon = {
									Icon(
											painter = painterResource(R.drawable.cookie),
											contentDescription = null,
											tint = MaterialTheme.colorScheme.primary
									)
								},
						)
					}
					item {
						SwitchPreference(
								title = stringResource(R.string.third_party_cookie),
								checked = state.isThirdPartyCookieAccept,
								onCheckedChange = { state.updateThirdPartyCookieAccept(it) },
								icon = {
									Icon(
											painter = painterResource(R.drawable.cookie),
											contentDescription = null,
											tint = MaterialTheme.colorScheme.primary
									)
								},
						)
					}
				}
			}
		}
	}
	// 网站菜单
	if (showWebsiteSheet) {
		GridDialog(
				onDismissRequest = { showWebsiteSheet = false },
				columns = 3,
				items = listOf(
						MenuItem(stringResource(R.string.copy_link), R.drawable.copy) {
							contextUtil.copy("url:", state.url)
							showWebsiteSheet = false
						},
						MenuItem(stringResource(R.string.share), R.drawable.share) {
							context.startActivity(
									Intent(Intent.ACTION_SEND).setType("text/plain")
										.putExtra(Intent.EXTRA_TEXT, trim(state.url))
							)
							showWebsiteSheet = false
						},
						MenuItem(stringResource(R.string.open_in_browser), R.drawable.export) {
							context.startActivity(
									Intent(Intent.ACTION_VIEW).setData(trim(state.url).toUri())
							)
							showWebsiteSheet = false
						},
						MenuItem(stringResource(R.string.cookie), R.drawable.cookie) {
							cookieDraft = state.getCookie(trim(state.url)) ?: ""
							showCookieEditor = true
						},
						MenuItem(stringResource(R.string.webpage_source), R.drawable.version) {
							state.viewSource()
							showWebsiteSheet = false
						},
				),
		)
	}
	// Cookie 编辑对话框
	if (showCookieEditor) {
		AlertDialog(
				onDismissRequest = { showCookieEditor = false },
				title = { Text(stringResource(R.string.cookie)) },
				text = {
					OutlinedTextField(
							value = cookieDraft,
							onValueChange = { cookieDraft = it },
							modifier = Modifier.fillMaxWidth()
					)
				},
				confirmButton = {
					TextButton(
							onClick = {
								state.setCookie(trim(state.url), cookieDraft)
								showCookieEditor = false
							}, shapes = ButtonDefaults.shapes()
					) { Text(stringResource(R.string.save)) }
				},
				dismissButton = {
					FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
						TextButton(
								onClick = { state.clearCookie(trim(state.url)); cookieDraft = "" },
								shapes = ButtonDefaults.shapes()
						) { Text(stringResource(R.string.clear)) }
						TextButton(
								onClick = { contextUtil.copy("Cookie:", cookieDraft) },
								shapes = ButtonDefaults.shapes()
						) { Text(stringResource(R.string.copy)) }
						TextButton(
								onClick = { showCookieEditor = false },
								shapes = ButtonDefaults.shapes()
						) { Text(stringResource(R.string.cancel)) }
					}
				},
		)
	}
	// 脚本面板
	if (showJsSheet) {
		ModalBottomSheet(onDismissRequest = { showJsSheet = false }) {
			Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(
								horizontal = dimensionResource(R.dimen.horizontal_padding),
								vertical = dimensionResource(R.dimen.vertical_padding)
						),
//					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically,
			) {
				Text(
						stringResource(R.string.js),
						style = MaterialTheme.typography.titleMedium,
						modifier = Modifier.weight(1f)
				)
//				Row {
				TextButton(
						onClick = {
							showJsSheet = false
							backStack.add(JsList(autoAdd = true))
						}, shapes = ButtonDefaults.shapes()
				) { Text(stringResource(R.string.add)) }
				TextButton(
						onClick = {
							showJsSheet = false
							backStack.add(JsList())
						}, shapes = ButtonDefaults.shapes()
				) { Text(stringResource(R.string.manage)) }
//				}
			}
			HorizontalDivider()
			Column(
					modifier = Modifier
						.fillMaxWidth()
						.verticalScroll(rememberScrollState())
			) {
				PreferenceCategory {
					ScriptManager.getMatchingScripts(
							state.url, jsList.orEmpty(), includeDisabled = true
					).forEach { item ->
						item {
							// ModalBottomSheet 渲染在独立窗口中，与 SharedTransitionLayout
							// 不在同一层级，无法参与共享元素过渡（会抛
							// "layouts are not part of the same hierarchy"）；脚本条目的
							// 共享元素过渡由脚本列表页（JsListRoute）承担
							Preference(
									enabled = item.state == 1,
									onClick = { ScriptManager.executeScript(item, state.webView) },
									title = item.title ?: "",
									summary = when {
										item.state != 1 -> stringResource(R.string.disable) + (item.description?.let { " · $it" }
											?: "")

										else -> item.description
									},
									icon = {
										Icon(
												painter = painterResource(R.drawable.js),
												contentDescription = null,
												tint = MaterialTheme.colorScheme.primary
										)
									},
									trailing = {
										Box {
											IconButton(onClick = { menuScript = item }) {
												Icon(
														Icons.Rounded.MoreVert,
														contentDescription = null
												)
											}
											val scriptId = "${item.title}_${item.namespace ?: ""}"
											DropdownMenu(
													expanded = menuScript == item,
													onDismissRequest = { menuScript = null },
											) {
												state.gmBridge.getCommands(scriptId)
													.forEach { commandName ->
														DropdownMenuItem(
																text = { Text(commandName) },
																onClick = {
																	menuScript = null
																	state.webView.evaluateJavascript(
																			"if (window.gm_commands && window.gm_commands['${scriptId}_${commandName}']) window.gm_commands['${scriptId}_${commandName}']();",
																			null
																	)
																},
														)
													}
												DropdownMenuItem(
														text = { Text(stringResource(R.string.run)) },
														onClick = {
															menuScript = null
															ScriptManager.executeScript(
																	item, state.webView
															)
														},
												)
												DropdownMenuItem(
														text = { Text(stringResource(R.string.edit)) },
														onClick = {
															menuScript = null
															backStack.add(JsDetail(item.id))
														},
												)
												DropdownMenuItem(
														text = {
															Text(
																	stringResource(
																			if (item.state == 0) R.string.enable else R.string.disable
																	)
															)
														},
														onClick = {
															menuScript = null
															item.state = 1 - item.state
															jsModel.updateJs(item)
														},
												)
												DropdownMenuItem(
														text = { Text(stringResource(R.string.delete)) },
														onClick = {
															menuScript = null
															jsModel.deleteJs(item)
														},
												)
											}
										}
									},
							)
						}
					}
				}
			}
		}
	}
}


@Composable
private fun FindBar(
	keyword: String,
	status: String,
	onKeywordChange: (String) -> Unit,
	onClose: () -> Unit,
	onStatusClick: () -> Unit,
	onFindNext: (Boolean) -> Unit,
) {
	Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(
						vertical = dimensionResource(R.dimen.vertical_padding)
				),
			verticalAlignment = Alignment.CenterVertically,
	) {
		IconButton(onClick = onClose) {
			Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
		}
		TextField(
				value = keyword,
				onValueChange = onKeywordChange,
				placeholder = {
					Text(
							stringResource(R.string.keyword),
							style = MaterialTheme.typography.bodySmall
					)
				},
				singleLine = true,
				textStyle = MaterialTheme.typography.bodyMedium,
				modifier = Modifier
					.weight(1f)
					.fillMaxWidth(),
				shape = RoundedCornerShape(12.dp),
		)
		TextButton(onClick = onStatusClick, shapes = ButtonDefaults.shapes()) {
			Text(status)
		}
		IconButton(onClick = { onFindNext(true) }) {
			Icon(painter = painterResource(R.drawable.down), contentDescription = null)
		}
		IconButton(onClick = { onFindNext(false) }) {
			Icon(painter = painterResource(R.drawable.up), contentDescription = null)
		}
	}
}
