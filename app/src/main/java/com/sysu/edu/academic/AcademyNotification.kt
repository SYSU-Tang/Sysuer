package com.sysu.edu.academic

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.databinding.ActivityPagerBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.AdapterListener
import com.sysu.edu.view.Pager2Adapter

class AcademyNotification : BaseActivity() {
	lateinit var model: JwxtModel
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val pager2Adapter = Pager2Adapter(this)
		val binding = ActivityPagerBinding.inflate(layoutInflater).apply {
			toolbar.setTitle(R.string.academic_affair_notice)
			toolbar.setNavigationOnClickListener { _: View? -> supportFinishAfterTransition() }
			pager.adapter = pager2Adapter
			TabLayoutMediator(tabLayout, pager) { tab: TabLayout.Tab?, position: Int -> tab?.setText(intArrayOf(R.string.academic_affair_notice, R.string.school_affair_notice)[position]) }.attach()
		}
		setContentView(binding.getRoot())
		model = JwxtModel(this)
		val dialog = MaterialAlertDialogBuilder(this).setMessage("")
			.setPositiveButton(R.string.confirm) { _: DialogInterface?, _: Int -> }
			.create()
		(0..1).forEach { _ ->
			pager2Adapter.add(NewsFragment().apply {
				setListener(object : AdapterListener {
					override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
					                    holder: RecyclerView.ViewHolder,
					                    position: Int) {
						holder.itemView.setOnClickListener {(adapter as NewsFragment.NewsAdapter).get(position)?.let {
									dialog.setTitle(it.getString("title"))
									getContent(it.getString("id"))
								}
						}
					}
					
					override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
					                      binding: ViewBinding?) {
					}
				})
			})
		}
		model.message.observe(this, Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) {
				when (message.first) {
					0, 1 -> response.getJSONObject("data")
						.getJSONArray("list")
						.forEach { a: Any? -> (pager2Adapter.get(message.first) as NewsFragment).add(a as JSONObject?) }
					2 -> startActivity(Intent(this, BrowserActivity::class.java).putExtra("data", ("""<!DOCTYPE html><html><head><style>
                                            body{
                                            padding: 24px !important;
                                            }
                                            a,body,p,span{
                                            font-size: 2.5rem !important;
                                            line-height: 2.0 !important;
                                             }
                                             table{
                                            table-layout: auto !important;
                                            width: 100% !important;
                                             }
                                             table,th, td
                                                    {
                                            font-size: 1.0rem !important;
                                            line-height: 1.0 !important;
                                                    border-collapse: collapse !important;
                                                    border: 2px solid windowtext !important;
                                                    }
                                            </style></head><body>""".trimIndent() + response.getString("data") + "</body></html>").trim { it <= ' ' }), ActivityOptionsCompat.makeSceneTransitionAnimation(this, binding.toolbar, "miniapp")
						.toBundle())
				}
				model.nextAll()
			}
		})
		schoolNotices
		notices
		model.next()
	}
	
	fun getList(column: String?, what: Int) {
		model.add("jwxt/system-manage/info-delivery?column=$column&deliveryObject=02&status=1&resourceCode=jwgld", what)
	}
	
	val notices: Unit
		get() {
			getList("01", 0)
		}
	val schoolNotices: Unit
		get() {
			getList("02", 1)
		}
	
	fun getContent(id: String?) {
		model.addAndNext("jwxt/system-manage/info-delivery/noticeId?id=$id", 2)
	}
	
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
}