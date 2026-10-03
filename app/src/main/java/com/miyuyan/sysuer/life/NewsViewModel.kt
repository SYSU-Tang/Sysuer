package com.miyuyan.sysuer.life

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.IportalModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NewsViewModel(application: Application) : AndroidViewModel(application) {
	private val model: IportalModel = IportalModel(application)

	/** 各栏目新闻列表 */
	private val _news = MutableStateFlow<List<JSONObject>>(emptyList())
	val news = _news.asStateFlow()
	private val _subscriptions = MutableStateFlow<List<JSONObject>>(emptyList())
	val subscriptions = _subscriptions.asStateFlow()
	private val _notices = MutableStateFlow<List<JSONObject>>(emptyList())
	val notices = _notices.asStateFlow()
	private val _dailyNews = MutableStateFlow<List<JSONObject>>(emptyList())
	val dailyNews = _dailyNews.asStateFlow()

	/** 搜索联想词 */
	private val _suggestions = MutableStateFlow<List<String>>(emptyList())
	val suggestions = _suggestions.asStateFlow()

	/** 图片请求所需凭证 */
	val cookie: String get() = model.cookie
	val authorization: String get() = model.authorization

	/** 门户搜索页地址 */
	fun searchUrl(keyword: String): String =
		"https://${model.host}/searchWeb/#/index?searchWord=$keyword&module=default&size=10&current=1&sortType=score&searchType=3"

	val newsUiState = model.getUiState(TAB_NEWS)
	val subscriptionUiState = model.getUiState(TAB_SUBSCRIPTION)
	val noticeUiState = model.getUiState(TAB_NOTICE)
	val dailyNewsUiState = model.getUiState(TAB_DAILY)

	/** 各栏目当前已加载到的页码；资讯为推荐流，无分页 */
	private val pages = intArrayOf(1, 1, 1, 1)
	private val loading = booleanArrayOf(false, false, false, false)

	init {
		viewModelScope.launch {
			model.message.collect { (code, response) ->
				when (code) {
					SUGGEST_REQUEST -> {
						val suggests = response.getJSONObject("data")?.getJSONArray("suggests")
						_suggestions.value = suggests?.map { it.toString() } ?: emptyList()
					}

					TAB_NEWS -> {
						loading[TAB_NEWS] = false
						val list = response.getJSONArray("data") ?: return@collect
						_news.value = list.filterIsInstance<JSONObject>()
						newsUiState.value = if (list.isEmpty()) UiState.Empty else UiState.Content
					}

					TAB_SUBSCRIPTION, TAB_NOTICE, TAB_DAILY -> {
						loading[code] = false
						val list = response.getJSONObject("data")?.getJSONArray("records")
							?: return@collect
						append(code, list)
					}
				}
			}
		}
	}

	private fun append(tab: Int, list: JSONArray) {
		val items = list.filterIsInstance<JSONObject>()
		val flow = when (tab) {
			TAB_SUBSCRIPTION -> _subscriptions
			TAB_NOTICE -> _notices
			else -> _dailyNews
		}
		flow.value = if (pages[tab] > 2) flow.value + items else items
		val uiState = when (tab) {
			TAB_SUBSCRIPTION -> subscriptionUiState
			TAB_NOTICE -> noticeUiState
			else -> dailyNewsUiState
		}
		uiState.value = if (flow.value.isEmpty()) UiState.Empty else UiState.Content
	}

	/** 拉取栏目内容，[tab] 为栏目索引 */
	fun fetch(tab: Int) {
		pages[tab] = 1
		loading[tab] = true
		when (tab) {
			TAB_NEWS -> model.enqueue(
					"ai_service/content-portal/recommend/query-recommend", "", TAB_NEWS
			)

			else -> model.enqueue(
					"ai_service/content-portal/user/content/page",
					"{\"pageSize\":20,\"currentPage\":${pages[tab]++},\"apiCode\":\"${API_CODES[tab]}\",\"notice\":false}",
					tab
			)
		}
	}

	/** 滚动到底部时加载下一页，资讯为推荐流无分页 */
	fun loadMore(tab: Int) {
		if (tab == TAB_NEWS || loading[tab]) return
		loading[tab] = true
		val uiState = when (tab) {
			TAB_SUBSCRIPTION -> subscriptionUiState
			TAB_NOTICE -> noticeUiState
			else -> dailyNewsUiState
		}
		uiState.value = if (pages[tab] < 2) UiState.Loading else UiState.LoadMore
		model.enqueue(
				"ai_service/content-portal/user/content/page",
				"{\"pageSize\":20,\"currentPage\":${pages[tab]++},\"apiCode\":\"${API_CODES[tab]}\",\"notice\":false}",
				tab
		)
	}

	/** 搜索联想词 */
	fun fetchSuggestions(keyword: String) {
		model.enqueue(
				"ai_service/search-server/needle/suggest",
				JSONObject.of("aliasName", "collection_data", "keyWord", keyword).toJSONString(),
				SUGGEST_REQUEST
		)
	}

	fun clearSuggestions() {
		_suggestions.value = emptyList()
	}

	override fun onCleared() {
		model.dispose()
	}

	companion object {
		private const val TAB_NEWS = 0
		private const val TAB_SUBSCRIPTION = 1
		private const val TAB_NOTICE = 2
		private const val TAB_DAILY = 3
		private const val SUGGEST_REQUEST = 4

		private val API_CODES = arrayOf("", "3ytr4e6c", "3ytunvv6", "4cef8rqw")
	}
}
