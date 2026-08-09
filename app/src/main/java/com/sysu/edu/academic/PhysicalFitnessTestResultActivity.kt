package com.sysu.edu.academic

import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CookieManager
import com.sysu.edu.api.HttpManager
import com.sysu.edu.api.TargetUrl
import com.sysu.edu.databinding.ActivityPagerBinding
import com.sysu.edu.view.Pager2Adapter
import com.sysu.edu.view.StaggerFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class PhysicalFitnessTestResultActivity : BaseActivity() {
	lateinit var http: HttpManager
	var position: Int = 0
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val adp = Pager2Adapter(this)
		adp.add(StaggerFragment()).add(StaggerFragment()).add(StaggerFragment())
		val binding = ActivityPagerBinding.inflate(layoutInflater).apply {
			toolbar.setTitle(R.string.physical_fitness_test_result)
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			pager.adapter = adp
			TabLayoutMediator(tabLayout, pager) { tab: TabLayout.Tab?, position: Int ->
				tab?.text = mutableListOf<String?>("体测成绩", "体育积分", "游泳")[position]
			}.attach()
		}
		setContentView(binding.root)
		val cm = CookieManager(this)
		val urls = mutableListOf("m/tice", "m/kwjfList", "m/tice/studentSwim")
		val name = mutableListOf(R.string.total_score, R.string.total_gym_credit, R.string.is_pass)
		val links = mutableListOf(mutableListOf(), mutableListOf(), mutableListOf<String>())
		(0..2).forEach { i ->
			get(cm, urls[i], links[i], adp.get(i) as StaggerFragment, name[i], i)
		}
	}
	
	private fun get(
		cm: CookieManager, urls: String,
		links: MutableList<String>,
		adp: StaggerFragment,
		name: Int,
		tabIndex: Int
	) {
		config.contextUtil.disposable.add(Observable.fromCallable<Any> {
			Jsoup.connect("https://tice.sysu.edu.cn/${urls}")
				.header("Cookie", cm.toSimpleString("tice.sysu.edu.cn"))
				.userAgent("Mozilla/5.0 (Linux; Android 15.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36")
				.timeout(3000)
				.get()
		}
			.retry(3)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe {
				val doc = it as Document
				if (doc.getElementById("netid-login") != null) config.contextUtil.login(
					TargetUrl.TICE
				) {
					get(cm, urls, links, adp, name, tabIndex)
				} else doc.select("a.weui-cell.weui-cell_access")
					.forEach { element ->
						val title = element.selectFirst(".weui-cell__bd p")
							?.text()
						val score = element.selectFirst("span")
							?.text()
						val link = element.attr("href")
						links.add(link)
						adp.addSection(
							title,
							R.drawable.calendar,
							mutableListOf(getString(name)),
							mutableListOf(score)
						              )

						val sectionIndex = adp.sections.size - 1
						val twoColumnsAdapter = adp.sectionAdapter.getTwoColumnsAdapter(sectionIndex)
						val currentPos = links.size - 1
						
						twoColumnsAdapter.setRowClickListener(0) {
							if (twoColumnsAdapter.itemCount == 1 && tabIndex != 2) {
								config.contextUtil.disposable.add(Observable.fromCallable<Any> {
									Jsoup.connect("https://tice.sysu.edu.cn${links[currentPos]}")
										.header("Cookie", cm.toSimpleString("tice.sysu.edu.cn"))
										.userAgent("Mozilla/5.0 (Linux; Android 15.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36")
										.timeout(3000)
										.get()
								}
									.retry(3)
									.subscribeOn(Schedulers.io())
									.observeOn(AndroidSchedulers.mainThread())
									.subscribe { detailDoc ->
										(detailDoc as Document).select(".weui-cell")
											.forEach { cellElement ->
												when (tabIndex) {
													0 -> {
														val cellTitle = cellElement.selectFirst(".weui-cell__bd p")?.text()
														val cellScore = cellElement.selectFirst(".weui-cell__ft")?.text()
														twoColumnsAdapter.add(key = cellTitle, value = cellScore)
													}
													1 -> {
														twoColumnsAdapter.add(
															key = cellElement.selectFirst("p")?.text() ?: "",
															value = cellElement.selectFirst(".weui-cell__hd")?.text() ?: ""
														)
														cellElement.select(".container").forEach { item ->
															twoColumnsAdapter.add(
																key = item.selectFirst(".left_side")?.text() ?: "",
																value = item.selectFirst("p")?.text() ?: ""
															)
														}
													}
												}
											}
									})
							}
						}
					}
			})
	}
	
	override fun onDestroy() {
		super.onDestroy()
		config.contextUtil.disposable.dispose()
	}
}