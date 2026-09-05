package com.sysu.edu.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.toStringOrDefault
import com.sysu.edu.nav.navigateBack
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.RowData
import com.sysu.edu.view.SectionData
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PersonalInformationRoute(
    backStack: MutableList<NavKey>,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val viewModel: PersonalInformationViewModel = viewModel()
    val activity = LocalActivity.current

    val infoList by viewModel.infoList.observeAsState(emptyList())

    LaunchedEffect(Unit) {
        if (infoList.isEmpty()) {
            viewModel.fetchPersonalInfo()
        }
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
            } else {
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

    val tabs = tabTitles.map { MenuItem(it) }

    ActivityPager(
        title = stringResource(R.string.personal_info),
        tabs = tabs,
        onNavigationClick = { backStack.navigateBack(activity) },
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        sharedKey = "PersonalInformation",
        topBarMenus = {
            listOf(exportMarkdownMenuItem(backStack, allSections, tabs, stringResource(R.string.personal_info)))
        }
    ) { page ->
        allSections.getOrNull(page)?.let { StaggerScreen(sections = it) }
    }
}
