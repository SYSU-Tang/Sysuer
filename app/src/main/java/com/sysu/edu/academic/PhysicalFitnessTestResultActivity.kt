package com.sysu.edu.academic

import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.HttpManager
import com.sysu.edu.api.TargetUrl
import com.sysu.edu.databinding.ActivityPagerBinding
import com.sysu.edu.view.AdapterListener
import com.sysu.edu.view.Pager2Adapter
import com.sysu.edu.view.StaggeredFragment
import java.util.regex.Pattern

class PhysicalFitnessTestResultActivity : BaseActivity() {
	lateinit var http: HttpManager
	var position: Int = 0
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val adp = Pager2Adapter(this)
		val page1 = StaggeredFragment()
		val page2 = StaggeredFragment()
		val page3 = StaggeredFragment()
		adp.add(page1).add(page2).add(page3)
		val binding = ActivityPagerBinding.inflate(layoutInflater).apply {
			toolbar.setTitle(R.string.physical_fitness_test_result)
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			pager.adapter = adp
			TabLayoutMediator(tabLayout, pager) { tab: TabLayout.Tab?, position: Int ->
				tab?.text = mutableListOf<String?>("体测成绩", "体育积分", "游泳")[position]
			}.attach()
		}
		setContentView(binding.root)
		//val cm = CookieManager(this)
		/*val a = Observable.fromCallable<Any> {
			Jsoup.connect("https://tice.sysu.edu.cn/m/tice")
				.header("Cookie", cm.toSimpleString("tice.sysu.edu.cn"))
				.userAgent("Mozilla/5.0 (Linux; Android 15.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36")
				.timeout(3000)
				.get()
		}
			.retry(3)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe {
				(it as Document).select("a.weui-cell.weui-cell_access").forEach { element ->
					val title = element.selectFirst(".weui-cell__bd p")?.text()
					val score = element.selectFirst(".weui-cell__ft span")?.text()
					val link = element.attr("href")
					val fragment = adp.get(0) as StaggeredFragment
					fragment.add(title,
					             R.drawable.calendar,
					             mutableListOf("总成绩"),
					             mutableListOf(score))
					fragment.setListener(object : AdapterListener {
						override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
						                    holder: RecyclerView.ViewHolder,
						                    position: Int) {
							val twoColumnsAdapter = fragment.staggeredAdapter.getTwoColumnsAdapter(
								position)
							twoColumnsAdapter?.setListener(object : AdapterListener {
								override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
								                    holder: RecyclerView.ViewHolder,
								                    position: Int) {
									holder.itemView.setOnClickListener {
										if (adapter.itemCount == 1) {
											val b = Observable.fromCallable<Any> {
												Jsoup.connect("https://tice.sysu.edu.cn$link")
													.header("Cookie",
													        cm.toSimpleString("tice.sysu.edu.cn"))
													.userAgent("Mozilla/5.0 (Linux; Android 15.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36")
													.timeout(3000)
													.get()
											}
												.retry(3)
												.subscribeOn(Schedulers.io())
												.observeOn(AndroidSchedulers.mainThread())
												.subscribe { doc ->
													(doc as Document).select(".weui-cell")
														.forEach { element ->
															val title = element.selectFirst(".weui-cell__bd p")
																?.text()
															val score = element.selectFirst(".weui-cell__ft")
																?.text()
															twoColumnsAdapter.add(key = title,
															                      value = score)
														}
												}
										}
									}
								}
								
								override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
								                      binding: ViewBinding?) {
								}
							})
						}
						
						override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
						                      binding: ViewBinding?) {
						}
					})
				}
			}*/
		http = HttpManager(object : Handler(this.mainLooper) {
			override fun handleMessage(msg: Message) {
				super.handleMessage(msg)
				val what = msg.what
				if (what == -1) config.toast(R.string.no_net_connected)
				else {
					if (msg.data.getInt("code") == 302 || Pattern.compile("window\\.location\\.href.+?\"/caslogin\"",
					                                                      Pattern.DOTALL)
							.matcher(msg.obj as String)
							.find()) {
						config.toast(R.string.login_warning)
						config.gotoLogin(TargetUrl.TICE)
					}
					else when (what) {
						0, 1, 2 -> {
							val urls = ArrayList<String?>()
							val matcher = Pattern.compile("<a class=\"weui-cell weui-cell_access\".+?</a>",
							                              Pattern.DOTALL).matcher(msg.obj as String)
							val page = adp.get(what) as StaggeredFragment
							while (matcher.find()) {
								val matcher1 = Pattern.compile("<div class=\"weui-cell__bd\".*?<p.*?>(.+?)</p>.*?<div class=\"weui-cell__ft.*?\">.+?<span.+?>(.+?)(&nbsp;)?</span>",
								                               Pattern.DOTALL)
									.matcher(matcher.group())
								if (matcher1.find()) page.add(matcher1.group(1),
								                              R.drawable.calendar,
								                              mutableListOf(arrayOf("总成绩",
								                                                    "总积分",
								                                                    "是否达标")[msg.what]),
								                              mutableListOf(matcher1.group(2)
									                                            ?.trim { it <= ' ' }))
								val matcher2 = Pattern.compile("<a class=\"weui-cell weui-cell_access\" href=\"(.+?)\">",
								                               Pattern.DOTALL)
									.matcher(matcher.group())
								if (matcher2.find()) urls.add(matcher2.group(1))
							}
							page.setListener(object : AdapterListener {
								override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
								                    holder: RecyclerView.ViewHolder,
								                    position: Int) {
									page.staggeredAdapter.getTwoColumnsAdapter(position)
										?.setListener(object : AdapterListener {
											override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
											                    holder: RecyclerView.ViewHolder,
											                    position: Int) {
												holder.itemView.setOnClickListener {
													if (adapter.itemCount == 1 && what != 2) {
														if (what == 0) getDetail(urls[position])
														if (what == 1) getCreditDetail(urls[position])
														this@PhysicalFitnessTestResultActivity.position = position
													}
												}
											}
											
											override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
											                      binding: ViewBinding?) {
											}
										})
								}
								
								override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
								                      binding: ViewBinding?) {
								}
							})
							when (msg.what) {
								0 -> credit
								1 -> swim
							}
						}
						3 -> {
							val matcher = Pattern.compile("<a class=\"weui-cell\">.*?<div class=\"weui-cell__bd\">(.+?)</div>.*?<div class=\"weui-cell__ft\">(.+?)</div>.*?</a>",
							                              Pattern.DOTALL).matcher(msg.obj as String)
							while (matcher.find()) page1.staggeredAdapter.addRow(position,
							                                                     matcher.group(1)
								                                                     ?.replace("</span>",
								                                                               "~")
								                                                     ?.replace("<.+?>".toRegex(),
								                                                               "")
								                                                     ?.replace("\\s".toRegex(),
								                                                               "")
								                                                     ?.trim { it <= ' ' },
							                                                     matcher.group(2)
								                                                     ?.replace("</span>",
								                                                               "~")
								                                                     ?.replace("<.+?>".toRegex(),
								                                                               "")
								                                                     ?.replace("%s",
								                                                               "")
								                                                     ?.trim { it <= ' ' })
						}
						4 -> {
							val matcher = Pattern.compile("<a class=\"weui-cell\">.+?</a>",
							                              Pattern.DOTALL).matcher(msg.obj as String)
							while (matcher.find()) {
								val matcher1 = Pattern.compile("class=\"left_side\">(.+?)</div>.+?<p>(.+?)</p></div>",
								                               Pattern.DOTALL)
									.matcher(matcher.group())
								val creditMatcher = Pattern.compile("class=\"ticeImg\">(.+?)<",
								                                    Pattern.DOTALL)
									.matcher(matcher.group())
								if (creditMatcher.find()) page2.staggeredAdapter.addRow(position,
								                                                        "积分",
								                                                        creditMatcher.group(
									                                                        1)
									                                                        ?.trim { it <= ' ' })
								while (matcher1.find()) page2.staggeredAdapter.addRow(position,
								                                                      matcher1.group(
									                                                      1),
								                                                      matcher1.group(
									                                                      2))
							}
						}
					}
				}
			}
		}).apply {
			setParams(this@PhysicalFitnessTestResultActivity.config)
			setUA("Mozilla/5.0 (Linux; Android 15.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36")
		}
		result
	}
	
	fun getDetail(url: String?) {
		http.getRequest("https://tice.sysu.edu.cn$url", 3)
	}
	
	fun getCreditDetail(url: String?) {
		http.getRequest("https://tice.sysu.edu.cn$url", 4)
	}
	
	val result: Unit
		get() {
			http.getRequest("https://tice.sysu.edu.cn/m/tice", 0)
		}
	val credit: Unit
		get() {
			http.getRequest("https://tice.sysu.edu.cn/m/kwjfList", 1)
		}
	val swim: Unit
		get() {
			http.getRequest("https://tice.sysu.edu.cn/m/tice/studentSwim", 2)
		}
}