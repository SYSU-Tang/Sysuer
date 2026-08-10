package com.sysu.edu.academic

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.toStringOrDefault
import com.sysu.edu.api.DataStoreManager
import com.sysu.edu.browser.RichTextActivity
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.RowData
import com.sysu.edu.view.SectionData
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.toMarkdown

class PersonalInformationActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		val viewModel = ViewModelProvider(this)[PersonalInformationViewModel::class.java]
		
		setContent {
			val infoList by viewModel.infoList.observeAsState(emptyList())
			
			LaunchedEffect(Unit) {
				viewModel.fetchPersonalInfo()
			}
			val tabTitles = remember(infoList) {
				infoList.map { it.getString("zdflmc") }
			}
			val allSections = remember(infoList) {
				val dict = HashMap<String?, String?>()
				dict["bmmc"] = "部门"
				dict["id"] = "ID"
				dict["jgmc"] = "籍贯"
				dict["hjszdText"] = "高中所在地"
				dict["zjxymc"] = "宗教信仰"
				dict["sfzszdmc"] = "身份证所在地"
				dict["jkzkmc"] = "健康状况"
				dict["csd"] = "出生地"
				dict["kslbmc"] = "考生类别"
				dict["hyzk"] = "婚姻状况"
				dict["cjrbjText"] = "残疾人标记"
				dict["xxmc"] = "学校"
				dict["hyzkmc"] = "婚姻状况描述"
				
				infoList.map { item ->
					val sections = mutableStateListOf<SectionData>()
					item.getJSONArray("fields")?.filterIsInstance<JSONObject>()?.forEach { field ->
						dict[field.getString("zdmc")] = field.getString("zdzwm")
					}
					val data = item.getJSONObject("data")
					if (data != null && !data.isEmpty()) {
						val rows = mutableStateListOf<RowData>()
						data.forEach { (k, v) ->
							rows.add(RowData(dict.getOrDefault(k, k), toStringOrDefault<Any?>(v)))
						}
						sections.add(SectionData(title = item.getString("zdflmc"), rows = rows))
					}
					else {
						var count = 1
						item.getJSONArray("dataList")?.filterIsInstance<JSONObject>()?.forEach { j ->
							val rows = mutableStateListOf<RowData>()
							j.forEach { (k, v) ->
								val value = when (k) {
									"gx", "gxrzzmm", "qdxl" -> (v as? JSONObject)?.getString("label")
									else -> "$v"
								}
								rows.add(RowData(dict.getOrDefault(k, k), value))
							}
							sections.add(SectionData(title = "${count++}", rows = rows))
						}
					}
					sections
				}
			}
			
			ActivityPager(title = stringResource(R.string.personal_info), tabs = tabTitles.map { MenuItem(it) }, actions = {
				IconButton(onClick = {
					val markdown = allSections.joinToString("\n\n") { it.toMarkdown() }
					DataStoreManager.saveContent(this@PersonalInformationActivity, getString(R.string.personal_info), markdown) {
						startActivity(Intent(this@PersonalInformationActivity, RichTextActivity::class.java).putExtra("type", DataStoreManager.ContentType.MARKDOWN.name).putExtra("title", getString(R.string.personal_info)))
					}
				}) {
					Icon(painter = painterResource(R.drawable.export), contentDescription = stringResource(R.string.export))
				}
			}, onNavigationClick = { supportFinishAfterTransition() }) { page ->
				allSections.getOrNull(page)?.let { StaggerScreen(it) }
			}
		}
	}
}