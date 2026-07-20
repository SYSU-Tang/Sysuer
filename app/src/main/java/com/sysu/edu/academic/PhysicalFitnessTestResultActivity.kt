package com.sysu.edu.academic

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CookieManager
import com.sysu.edu.api.HttpManager
import com.sysu.edu.api.TargetUrl
import com.sysu.edu.databinding.ActivityPagerBinding
import com.sysu.edu.view.AdapterListener
import com.sysu.edu.view.Pager2Adapter
import com.sysu.edu.view.StaggeredFragment
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
		adp.add(StaggeredFragment()).add(StaggeredFragment()).add(StaggeredFragment())
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
			get(cm, urls[i], links[i], adp.get(i) as StaggeredFragment, name[i])
			(adp.get(i) as StaggeredFragment).setListener(object : AdapterListener {
				override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
				                    holder: RecyclerView.ViewHolder,
				                    pos: Int) {
					val twoColumnsAdapter = (adp.get(i) as StaggeredFragment).staggeredAdapter.getTwoColumnsAdapter(
						pos)
					twoColumnsAdapter?.setListener(object : AdapterListener {
						override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
						                    holder: RecyclerView.ViewHolder,
						                    position: Int) {
							holder.itemView.setOnClickListener {
								if (adapter.itemCount == 1 && i != 2) {
									config.contextUtil.disposable.add(Observable.fromCallable<Any> {
										Jsoup.connect("https://tice.sysu.edu.cn${links[i][pos]}")
											.header("Cookie", cm.toSimpleString("tice.sysu.edu.cn"))
											.userAgent("Mozilla/5.0 (Linux; Android 15.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36")
											.timeout(3000)
											.get()
									}
										                                  .retry(3)
										                                  .subscribeOn(Schedulers.io())
										                                  .observeOn(
											                                  AndroidSchedulers.mainThread())
										                                  .subscribe { doc ->
											                                  (doc as Document).select(
												                                  ".weui-cell")
												                                  .forEach { element ->
													                                  when (i) {
														                                  0 -> {
															                                  val title = element.selectFirst(
																                                  ".weui-cell__bd p")
																                                  ?.text()
															                                  val score = element.selectFirst(
																                                  ".weui-cell__ft")
																                                  ?.text()
															                                  twoColumnsAdapter.add(
																                                  key = title,
																                                  value = score)
														                                  }
														                                  1 -> {
															                                  twoColumnsAdapter.add(
																                                  key = element.selectFirst(
																	                                  "p")
																	                                  ?.text()
																	                                  ?: "",
																                                  value = element.selectFirst(
																	                                  ".weui-cell__hd")
																	                                  ?.text()
																	                                  ?: "")
															                                  element.select(
																                                  ".container")
																                                  .forEach { item ->
																	                                  twoColumnsAdapter.add(
																		                                  key = item.selectFirst(
																			                                  ".left_side")
																			                                  ?.text()
																			                                  ?: "",
																		                                  value = item.selectFirst(
																			                                  "p")
																			                                  ?.text()
																			                                  ?: "")
																                                  }
														                                  }
													                                  }
												                                  }
										                                  })
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
	}
	
	private fun get(
		cm: CookieManager,urls: String,
	                links: MutableList<String>,
	                adp: StaggeredFragment,
	                name: Int) {
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
				                                  if ((it as Document).getElementById("netid-login") != null) config.contextUtil.login(
					                                  TargetUrl.TICE) {
					                                  get(cm, urls, links, adp, name)
				                                  }
				                                  else it.select("a.weui-cell.weui-cell_access")
					                                  .forEach { element ->
						                                  val title = element.selectFirst(".weui-cell__bd p")
							                                  ?.text()
						                                  val score = element.selectFirst("span")
							                                  ?.text()
						                                  val link = element.attr("href")
						                                  links.add(link)
						                                  adp.add(title,
						                                          R.drawable.calendar,
						                                          mutableListOf(getString(name)),
						                                          mutableListOf(score))
					                                  }
			                                  })
	}
	
	override fun onDestroy() {
		super.onDestroy()
		config.contextUtil.disposable.dispose()
	}
}